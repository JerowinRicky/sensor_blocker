package com.sensorlockdown.app.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensorlockdown.app.LockdownApplication
import com.sensorlockdown.app.authentication.model.AuthResult
import com.sensorlockdown.app.authentication.model.RateLimitState
import com.sensorlockdown.app.devicepolicy.DeviceManagementState
import com.sensorlockdown.app.lockdown.model.Category
import com.sensorlockdown.app.lockdown.model.ControlId
import com.sensorlockdown.app.lockdown.model.ControlPolicy
import com.sensorlockdown.app.lockdown.model.LockdownPolicy
import com.sensorlockdown.app.lockdown.model.OverallLockdownStatus
import com.sensorlockdown.app.persistence.entity.AuditLogEntity
import com.sensorlockdown.app.security.DeviceIntegrityChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val securityPatch: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
    val managementState: DeviceManagementState = DeviceManagementState.NORMAL_APP
)

data class ActivationProgress(
    val isActivating: Boolean = false,
    val currentControl: ControlId? = null,
    val step: Int = 0,
    val totalSteps: Int = 0
)

class LockdownViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LockdownApplication

    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _rateLimitState = MutableStateFlow(RateLimitState(false, 0, 0))
    val rateLimitState: StateFlow<RateLimitState> = _rateLimitState.asStateFlow()

    private val _lockdownPolicy = MutableStateFlow(LockdownPolicy())
    val lockdownPolicy: StateFlow<LockdownPolicy> = _lockdownPolicy.asStateFlow()

    private val _requestedSelections = MutableStateFlow<Map<ControlId, Boolean>>(emptyMap())
    val requestedSelections: StateFlow<Map<ControlId, Boolean>> = _requestedSelections.asStateFlow()

    private val _activationProgress = MutableStateFlow(ActivationProgress())
    val activationProgress: StateFlow<ActivationProgress> = _activationProgress.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntity>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntity>> = _auditLogs.asStateFlow()

    private val _integrityResult = MutableStateFlow(DeviceIntegrityChecker.IntegrityResult(false, false, false, emptyList()))
    val integrityResult: StateFlow<DeviceIntegrityChecker.IntegrityResult> = _integrityResult.asStateFlow()

    private var countdownJob: Job? = null

    init {
        refreshDeviceInfo()
        refreshRateLimitState()
        refreshPolicy()
        refreshAuditLogs()
        refreshIntegrity()
    }

    fun isPinSet(): Boolean = app.pinAuthenticationService.isPinSet()

    fun refreshDeviceInfo() {
        _deviceInfo.value = DeviceInfo(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            managementState = app.devicePolicyController.getManagementState()
        )
    }

    fun refreshRateLimitState() {
        val state = app.pinRateLimiter.getRateLimitState()
        _rateLimitState.value = state
        if (state.isLockedOut && (countdownJob == null || countdownJob?.isActive == false)) {
            startLockoutCountdown()
        }
    }

    private fun startLockoutCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val state = app.pinRateLimiter.getRateLimitState()
                _rateLimitState.value = state
                if (!state.isLockedOut) break
                delay(1000)
            }
        }
    }

    fun setupInitialPin(pin: String, confirmPin: String): AuthResult {
        val result = app.pinAuthenticationService.setupInitialPin(pin, confirmPin)
        if (result is AuthResult.Success) {
            _isAuthenticated.value = true
            refreshPolicy()
        }
        return result
    }

    fun loginWithPin(pin: String): AuthResult {
        val result = app.pinAuthenticationService.authenticate(pin)
        refreshRateLimitState()
        if (result is AuthResult.Success) {
            _isAuthenticated.value = true
            refreshPolicy()
        }
        return result
    }

    fun changePin(currentPin: String, newPin: String, confirmNewPin: String): AuthResult {
        return app.pinAuthenticationService.changePin(currentPin, newPin, confirmNewPin)
    }

    fun logout() {
        _isAuthenticated.value = false
    }

    fun toggleControlSelection(controlId: ControlId, enabled: Boolean) {
        val current = _requestedSelections.value.toMutableMap()
        current[controlId] = enabled
        _requestedSelections.value = current

        // Update local evaluated model
        val currentPolicy = _lockdownPolicy.value
        val existingControl = currentPolicy.controls[controlId]
        if (existingControl != null) {
            val updated = existingControl.copy(requestedState = enabled)
            _lockdownPolicy.value = currentPolicy.copy(
                controls = currentPolicy.controls + (controlId to updated)
            )
        }
    }

    fun refreshPolicy() {
        viewModelScope.launch {
            val evaluated = app.capabilityEvaluator.evaluateAllControls(_requestedSelections.value)
            val dbState = app.database.policyDao().getLockdownStateSnapshot()
            val isActive = dbState?.isActive == true

            val overallStatus = when {
                !isActive -> OverallLockdownStatus.INACTIVE
                evaluated.values.count { it.enforcedState } == 0 -> OverallLockdownStatus.FAILED
                evaluated.values.any { it.requestedState && !it.enforcedState } -> OverallLockdownStatus.PARTIALLY_ACTIVE
                else -> OverallLockdownStatus.FULLY_ACTIVE
            }

            _lockdownPolicy.value = LockdownPolicy(
                version = dbState?.policyVersion ?: 1,
                isActive = isActive,
                controls = evaluated,
                overallStatus = overallStatus
            )
        }
    }

    fun activateLockdown(onComplete: (LockdownPolicy) -> Unit) {
        viewModelScope.launch {
            _activationProgress.value = ActivationProgress(isActivating = true)
            val policy = app.lockdownEngine.activateLockdown(_requestedSelections.value) { control, step, total ->
                _activationProgress.value = ActivationProgress(
                    isActivating = true,
                    currentControl = control,
                    step = step,
                    totalSteps = total
                )
            }
            _activationProgress.value = ActivationProgress(isActivating = false)
            _lockdownPolicy.value = policy
            refreshAuditLogs()
            onComplete(policy)
        }
    }

    fun deactivateLockdown(onComplete: () -> Unit) {
        viewModelScope.launch {
            val policy = app.lockdownEngine.deactivateLockdown()
            _lockdownPolicy.value = policy
            refreshAuditLogs()
            onComplete()
        }
    }

    fun refreshAuditLogs() {
        viewModelScope.launch {
            val logs = app.database.auditLogDao().getRecentLogs(100)
            _auditLogs.value = logs
        }
    }

    fun refreshIntegrity() {
        _integrityResult.value = app.deviceIntegrityChecker.checkIntegrity()
    }

    fun getScreenCaptureProtection(): Boolean = app.securityPreferences.isScreenCaptureProtectionEnabled()

    fun setScreenCaptureProtection(enabled: Boolean) {
        app.securityPreferences.setScreenCaptureProtectionEnabled(enabled)
    }

    fun getEmergencyRecoverySecret(): String? {
        return app.securityPreferences.getEmergencyRecoverySecret()
    }
}

