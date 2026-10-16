package com.sensorlockdown.app.lockdown.engine

import android.media.AudioManager
import com.sensorlockdown.app.devicepolicy.DevicePolicyController
import com.sensorlockdown.app.lockdown.model.ControlId
import com.sensorlockdown.app.lockdown.model.ControlPolicy
import com.sensorlockdown.app.lockdown.model.EnforcementStatus
import com.sensorlockdown.app.lockdown.model.LockdownPolicy
import com.sensorlockdown.app.lockdown.model.OverallLockdownStatus
import com.sensorlockdown.app.persistence.dao.PolicyDao
import com.sensorlockdown.app.persistence.entity.LockdownStateEntity
import com.sensorlockdown.app.persistence.entity.PolicyControlEntity
import com.sensorlockdown.app.privacy.PrivacyController
import com.sensorlockdown.app.security.AuditLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Core lockdown orchestration engine.
 * Applies policies transactionally, validates enforcement with the OS,
 * persists verified results, and logs audit events.
 */
class LockdownEngine(
    private val capabilityEvaluator: CapabilityEvaluator,
    private val devicePolicyController: DevicePolicyController,
    private val privacyController: PrivacyController,
    private val audioManager: AudioManager?,
    private val policyDao: PolicyDao,
    private val auditLogger: AuditLogger
) {

    /**
     * Executes the lockdown activation transaction.
     * Progress listener receives (currentControl, step, totalSteps).
     */
    suspend fun activateLockdown(
        requestedSelections: Map<ControlId, Boolean>,
        onProgress: (ControlId, Int, Int) -> Unit = { _, _, _ -> }
    ): LockdownPolicy {
        val totalSteps = ControlId.values().size
        var step = 0
        val verifiedControls = mutableMapOf<ControlId, ControlPolicy>()

        for (controlId in ControlId.values()) {
            step++
            onProgress(controlId, step, totalSteps)
            val requested = requestedSelections[controlId] ?: true
            val evaluated = capabilityEvaluator.evaluateControl(controlId, requested)

            if (!requested) {
                // User intentionally opted out of this control
                verifiedControls[controlId] = evaluated.copy(
                    enforcedState = false,
                    status = EnforcementStatus.REQUESTED,
                    lastVerifiedAt = System.currentTimeMillis()
                )
                continue
            }

            // Attempt enforcement only if supported
            if (evaluated.supportedState) {
                val success = applyPolicy(controlId, true)
                val isEnforced = verifyPolicy(controlId)
                val status = if (isEnforced) {
                    EnforcementStatus.ENFORCED
                } else if (evaluated.status == EnforcementStatus.PARTIALLY_ENFORCED) {
                    EnforcementStatus.PARTIALLY_ENFORCED
                } else {
                    EnforcementStatus.ERROR
                }

                verifiedControls[controlId] = evaluated.copy(
                    enforcedState = isEnforced,
                    status = status,
                    lastVerifiedAt = System.currentTimeMillis()
                )

                if (isEnforced) {
                    auditLogger.logEvent(
                        eventType = AuditLogger.EventType.POLICY_APPLIED,
                        details = "Policy applied and verified: ${controlId.name} -> ENFORCED",
                        isSuccess = true
                    )
                } else {
                    auditLogger.logEvent(
                        eventType = AuditLogger.EventType.POLICY_FAILED,
                        details = "Policy application failed or unverified: ${controlId.name}",
                        isSuccess = false
                    )
                }
            } else {
                // Unsupported or not present; honestly keep evaluated status
                verifiedControls[controlId] = evaluated.copy(
                    enforcedState = false,
                    lastVerifiedAt = System.currentTimeMillis()
                )
            }
        }

        // Calculate composite lockdown health
        val enforcedCount = verifiedControls.values.count { it.enforcedState }
        val overallStatus = when {
            enforcedCount == 0 -> OverallLockdownStatus.FAILED
            verifiedControls.values.any { it.requestedState && !it.enforcedState && it.supportedState } ->
                OverallLockdownStatus.PARTIALLY_ACTIVE
            verifiedControls.values.any { it.requestedState && !it.supportedState } ->
                OverallLockdownStatus.PARTIALLY_ACTIVE
            else -> OverallLockdownStatus.FULLY_ACTIVE
        }

        val policy = LockdownPolicy(
            version = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isActive = overallStatus != OverallLockdownStatus.FAILED,
            controls = verifiedControls,
            overallStatus = overallStatus
        )

        // Persist policy state to Room
        persistPolicy(policy)

        auditLogger.logEvent(
            eventType = AuditLogger.EventType.LOCKDOWN_ACTIVATED,
            details = "Lockdown activated. Enforced: $enforcedCount / $totalSteps controls. Status: ${overallStatus.name}",
            isSuccess = overallStatus != OverallLockdownStatus.FAILED
        )

        return policy
    }

    /**
     * Secure deactivation: removes active policies and verifies removal.
     */
    suspend fun deactivateLockdown(): LockdownPolicy {
        for (controlId in ControlId.values()) {
            applyPolicy(controlId, false)
        }

        val refreshedControls = capabilityEvaluator.evaluateAllControls()
        val policy = LockdownPolicy(
            version = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isActive = false,
            controls = refreshedControls,
            overallStatus = OverallLockdownStatus.INACTIVE
        )

        persistPolicy(policy)

        auditLogger.logEvent(
            eventType = AuditLogger.EventType.LOCKDOWN_DEACTIVATED,
            details = "Lockdown deactivated by authorized user. System policies cleared.",
            isSuccess = true
        )

        return policy
    }

    /**
     * Re-verifies existing policies upon reboot or app start.
     */
    suspend fun verifyAndSyncPolicy(): LockdownPolicy {
        val existingState = policyDao.getLockdownStateSnapshot()
        val isActive = existingState?.isActive == true

        val refreshedControls = mutableMapOf<ControlId, ControlPolicy>()
        for (controlId in ControlId.values()) {
            val evaluated = capabilityEvaluator.evaluateControl(controlId, isActive)
            refreshedControls[controlId] = evaluated.copy(lastVerifiedAt = System.currentTimeMillis())
        }

        val overallStatus = if (!isActive) {
            OverallLockdownStatus.INACTIVE
        } else {
            val enforcedCount = refreshedControls.values.count { it.enforcedState }
            if (enforcedCount > 0) OverallLockdownStatus.PARTIALLY_ACTIVE else OverallLockdownStatus.FAILED
        }

        val policy = LockdownPolicy(
            version = existingState?.policyVersion ?: 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isActive = isActive,
            controls = refreshedControls,
            overallStatus = overallStatus
        )

        persistPolicy(policy)
        return policy
    }

    private fun applyPolicy(controlId: ControlId, enable: Boolean): Boolean {
        return when (controlId) {
            ControlId.CAMERA -> devicePolicyController.setCameraDisabled(enable)
            ControlId.SCREEN_CAPTURE -> devicePolicyController.setScreenCaptureDisabled(enable)
            ControlId.MICROPHONE -> privacyController.applyMicrophoneRestriction(enable)
            ControlId.LOCATION -> devicePolicyController.setLocationRestricted(enable)
            ControlId.USB_DATA -> devicePolicyController.setUsbRestrictions(enable)
            ControlId.BLUETOOTH -> devicePolicyController.setBluetoothRestricted(enable)
            ControlId.WIFI -> devicePolicyController.setWifiConfigRestricted(enable)
            ControlId.APP_INSTALLATION -> devicePolicyController.setAppInstallUninstallRestricted(enable)
            ControlId.APP_UNINSTALLATION -> devicePolicyController.setUninstallBlocked(enable)
            ControlId.FACTORY_RESET -> devicePolicyController.setFactoryResetRestricted(enable)
            ControlId.SAFE_BOOT -> devicePolicyController.setSafeBootRestricted(enable)
            ControlId.SPEAKER -> {
                audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, enable)
                true
            }
            else -> false
        }
    }

    private fun verifyPolicy(controlId: ControlId): Boolean {
        return when (controlId) {
            ControlId.CAMERA -> devicePolicyController.isCameraDisabled()
            ControlId.SCREEN_CAPTURE -> devicePolicyController.isScreenCaptureDisabled()
            ControlId.MICROPHONE -> privacyController.isMicrophoneMuted()
            ControlId.LOCATION -> devicePolicyController.isLocationRestricted()
            ControlId.USB_DATA -> devicePolicyController.isUsbRestricted()
            ControlId.BLUETOOTH -> devicePolicyController.isBluetoothRestricted()
            ControlId.WIFI -> devicePolicyController.isWifiConfigRestricted()
            ControlId.APP_INSTALLATION -> devicePolicyController.isAppInstallRestricted()
            ControlId.APP_UNINSTALLATION -> devicePolicyController.isUninstallBlocked()
            ControlId.FACTORY_RESET -> devicePolicyController.isFactoryResetRestricted()
            ControlId.SAFE_BOOT -> devicePolicyController.isSafeBootRestricted()
            ControlId.SPEAKER -> audioManager?.isStreamMute(AudioManager.STREAM_MUSIC) == true
            else -> false
        }
    }

    private suspend fun persistPolicy(policy: LockdownPolicy) {
        val entities = policy.controls.values.map { control ->
            PolicyControlEntity(
                controlId = control.controlId.name,
                category = control.controlId.category.name,
                requestedState = control.requestedState,
                enforcedState = control.enforcedState,
                status = control.status.name,
                reason = control.reason,
                lastVerifiedAt = control.lastVerifiedAt
            )
        }
        policyDao.insertOrUpdateControls(entities)
        policyDao.saveLockdownState(
            LockdownStateEntity(
                isActive = policy.isActive,
                policyVersion = policy.version,
                lastActivatedAt = if (policy.isActive) System.currentTimeMillis() else null,
                overallStatus = policy.overallStatus.name
            )
        )
    }

    fun observePolicy(): Flow<LockdownPolicy> {
        return policyDao.getAllControls().map { controlEntities ->
            val controlsMap = controlEntities.associate { entity ->
                val controlId = runCatching { ControlId.valueOf(entity.controlId) }.getOrDefault(ControlId.CAMERA)
                controlId to ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "",
                    requestedState = entity.requestedState,
                    supportedState = entity.status == EnforcementStatus.ENFORCED.name || entity.status == EnforcementStatus.PARTIALLY_ENFORCED.name,
                    enforcedState = entity.enforcedState,
                    status = runCatching { EnforcementStatus.valueOf(entity.status) }.getOrDefault(EnforcementStatus.ERROR),
                    reason = entity.reason,
                    lastVerifiedAt = entity.lastVerifiedAt
                )
            }
            LockdownPolicy(
                controls = controlsMap,
                isActive = controlsMap.values.any { it.enforcedState }
            )
        }
    }
}

