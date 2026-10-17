package com.sensorlockdown.launcher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sensorlockdown.launcher.data.AppInfo
import com.sensorlockdown.launcher.data.LauncherPreferences
import com.sensorlockdown.launcher.security.PinSecurityManager
import com.sensorlockdown.launcher.service.AppDiscoveryService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LauncherScreenState {
    PIN_SETUP,
    PIN_SETUP_CONFIRM,
    SETUP_APPS,
    PIN_LOGIN,
    HOME,
    APP_MANAGEMENT,
    SWITCH_DEFAULT_CONFIRM,
    POWER_OFF_AUTH,
    BLOCKED_APP_AUTH
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val securityManager = PinSecurityManager(application)
    private val preferences = LauncherPreferences(application)
    private val discoveryService = AppDiscoveryService(application)

    private val _screenState = MutableStateFlow<LauncherScreenState>(LauncherScreenState.PIN_LOGIN)
    val screenState: StateFlow<LauncherScreenState> = _screenState.asStateFlow()

    private val _blockedPackageName = MutableStateFlow<String?>(null)
    val blockedPackageName: StateFlow<String?> = _blockedPackageName.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _currentPinInput = MutableStateFlow("")
    val currentPinInput: StateFlow<String> = _currentPinInput.asStateFlow()

    private val _firstPinCandidate = MutableStateFlow("")

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lockoutSeconds = MutableStateFlow(0L)
    val lockoutSeconds: StateFlow<Long> = _lockoutSeconds.asStateFlow()

    private val _allInstalledApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allInstalledApps: StateFlow<List<AppInfo>> = _allInstalledApps.asStateFlow()

    private val _allowedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allowedApps: StateFlow<List<AppInfo>> = _allowedApps.asStateFlow()

    private val _selectedPackagesInSetup = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackagesInSetup: StateFlow<Set<String>> = _selectedPackagesInSetup.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var lockoutTickerJob: Job? = null

    init {
        determineInitialState()
        refreshApps()
    }

    fun determineInitialState() {
        if (!securityManager.isPinConfigured()) {
            _screenState.value = LauncherScreenState.PIN_SETUP
        } else if (!preferences.isInitialSetupCompleted()) {
            _screenState.value = LauncherScreenState.SETUP_APPS
        } else {
            _screenState.value = LauncherScreenState.PIN_LOGIN
        }
        _currentPinInput.value = ""
        _errorMessage.value = null
        checkLockout()
    }

    fun onDigitEntered(digit: String) {
        if (_currentPinInput.value.length < 6) {
            _currentPinInput.value += digit
            _errorMessage.value = null
            if (_currentPinInput.value.length == 6) {
                processCompletedPin(_currentPinInput.value)
            }
        }
    }

    fun onBackspace() {
        if (_currentPinInput.value.isNotEmpty()) {
            _currentPinInput.value = _currentPinInput.value.dropLast(1)
            _errorMessage.value = null
        }
    }

    fun onClearPin() {
        _currentPinInput.value = ""
        _errorMessage.value = null
    }

    private fun processCompletedPin(pin: String) {
        when (_screenState.value) {
            LauncherScreenState.PIN_SETUP -> {
                _firstPinCandidate.value = pin
                _currentPinInput.value = ""
                _screenState.value = LauncherScreenState.PIN_SETUP_CONFIRM
            }
            LauncherScreenState.PIN_SETUP_CONFIRM -> {
                if (pin == _firstPinCandidate.value) {
                    securityManager.setupPin(pin)
                    _currentPinInput.value = ""
                    _screenState.value = LauncherScreenState.SETUP_APPS
                } else {
                    _errorMessage.value = "PINs do not match. Try again."
                    _currentPinInput.value = ""
                    _screenState.value = LauncherScreenState.PIN_SETUP
                }
            }
            LauncherScreenState.PIN_LOGIN -> {
                if (securityManager.verifyPin(pin)) {
                    _currentPinInput.value = ""
                    _errorMessage.value = null
                    _screenState.value = LauncherScreenState.HOME
                } else {
                    handleFailedAuth()
                }
            }
            LauncherScreenState.SWITCH_DEFAULT_CONFIRM -> {
                if (securityManager.verifyPin(pin)) {
                    _currentPinInput.value = ""
                    _errorMessage.value = null
                    _screenState.value = LauncherScreenState.HOME
                    discoveryService.openDefaultHomeSettings()
                } else {
                    handleFailedAuth()
                }
            }
            LauncherScreenState.APP_MANAGEMENT -> {
                if (securityManager.verifyPin(pin)) {
                    _currentPinInput.value = ""
                    _errorMessage.value = null
                    _screenState.value = LauncherScreenState.SETUP_APPS
                } else {
                    handleFailedAuth()
                }
            }
            LauncherScreenState.POWER_OFF_AUTH -> {
                if (securityManager.verifyPin(pin)) {
                    _currentPinInput.value = ""
                    _errorMessage.value = null
                    _screenState.value = LauncherScreenState.HOME
                    com.sensorlockdown.launcher.service.LauncherSecurityService.authorizePowerActions(30_000L)
                    com.sensorlockdown.launcher.service.LauncherSecurityService.triggerPowerDialog()
                } else {
                    handleFailedAuth()
                }
            }
            LauncherScreenState.BLOCKED_APP_AUTH -> {
                if (securityManager.verifyPin(pin)) {
                    _currentPinInput.value = ""
                    _errorMessage.value = null
                    _blockedPackageName.value = null
                    _screenState.value = LauncherScreenState.HOME
                } else {
                    handleFailedAuth()
                }
            }
            else -> {}
        }
    }

    private fun handleFailedAuth() {
        _currentPinInput.value = ""
        if (securityManager.isLockedOut()) {
            startLockoutCountdown()
        } else {
            _errorMessage.value = "Incorrect PIN. Please try again."
        }
    }

    private fun checkLockout() {
        if (securityManager.isLockedOut()) {
            startLockoutCountdown()
        }
    }

    private fun startLockoutCountdown() {
        lockoutTickerJob?.cancel()
        lockoutTickerJob = viewModelScope.launch {
            while (securityManager.isLockedOut()) {
                val remaining = securityManager.getLockoutRemainingSeconds()
                _lockoutSeconds.value = remaining
                _errorMessage.value = "Too many failed attempts. Try again in ${remaining}s"
                delay(1000L)
            }
            _lockoutSeconds.value = 0L
            _errorMessage.value = null
        }
    }

    fun refreshApps() {
        checkSecurityStatus()
        viewModelScope.launch {
            val allowed = preferences.getAllowedPackages()
            _selectedPackagesInSetup.value = allowed
            val installed = discoveryService.loadInstalledApps(allowed)
            _allInstalledApps.value = installed
            _allowedApps.value = installed.filter { it.isAllowed }
        }
    }

    fun checkSecurityStatus() {
        _isAccessibilityEnabled.value = com.sensorlockdown.launcher.service.LauncherSecurityService.isRunning()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedPackagesInSetup.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackagesInSetup.value = current
    }

    fun selectAllApps() {
        _selectedPackagesInSetup.value = _allInstalledApps.value.map { it.packageName }.toSet()
    }

    fun deselectAllApps() {
        _selectedPackagesInSetup.value = emptySet()
    }

    fun saveAllowedApps() {
        preferences.setAllowedPackages(_selectedPackagesInSetup.value)
        preferences.setInitialSetupCompleted(true)
        refreshApps()
        _screenState.value = LauncherScreenState.HOME
    }

    fun launchApp(appInfo: AppInfo): Boolean {
        com.sensorlockdown.launcher.service.LauncherSecurityService.isLauncherForegroundOrLockedState = false
        return discoveryService.launchApp(appInfo.packageName, appInfo.activityName)
    }

    fun requestUninstall(appInfo: AppInfo): Boolean {
        return discoveryService.requestUninstall(appInfo.packageName)
    }

    fun lockLauncher() {
        _currentPinInput.value = ""
        _errorMessage.value = null
        _screenState.value = LauncherScreenState.PIN_LOGIN
        com.sensorlockdown.launcher.service.LauncherSecurityService.isLauncherForegroundOrLockedState = true
    }

    fun requestSwitchDefaultLauncher() {
        _currentPinInput.value = ""
        _errorMessage.value = null
        _screenState.value = LauncherScreenState.SWITCH_DEFAULT_CONFIRM
    }

    fun requestManageApps() {
        _currentPinInput.value = ""
        _errorMessage.value = null
        _screenState.value = LauncherScreenState.APP_MANAGEMENT
    }

    fun cancelAction() {
        _currentPinInput.value = ""
        _errorMessage.value = null
        _blockedPackageName.value = null
        _screenState.value = LauncherScreenState.HOME
    }

    fun handleBlockedPackage(packageName: String) {
        _blockedPackageName.value = packageName
        _currentPinInput.value = ""
        _errorMessage.value = null
        _screenState.value = LauncherScreenState.BLOCKED_APP_AUTH
        com.sensorlockdown.launcher.service.LauncherSecurityService.isLauncherForegroundOrLockedState = true
    }

    fun handlePowerAuthRequest() {
        _currentPinInput.value = ""
        _errorMessage.value = null
        _screenState.value = LauncherScreenState.POWER_OFF_AUTH
        com.sensorlockdown.launcher.service.LauncherSecurityService.isLauncherForegroundOrLockedState = true
    }

    fun requestPowerOff() {
        _currentPinInput.value = ""
        _errorMessage.value = null
        _screenState.value = LauncherScreenState.POWER_OFF_AUTH
        com.sensorlockdown.launcher.service.LauncherSecurityService.isLauncherForegroundOrLockedState = true
    }

    fun onScreenTurnedOff() {
        if (preferences.isLockOnScreenOff() &&
            securityManager.isPinConfigured() &&
            preferences.isInitialSetupCompleted()
        ) {
            _currentPinInput.value = ""
            _errorMessage.value = null
            _screenState.value = LauncherScreenState.PIN_LOGIN
            com.sensorlockdown.launcher.service.LauncherSecurityService.isLauncherForegroundOrLockedState = true
        }
    }

    fun openAccessibilitySettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(intent)
    }
}

