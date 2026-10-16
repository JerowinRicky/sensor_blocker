package com.sensorlockdown.app.privacy

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.sensorlockdown.app.devicepolicy.DevicePolicyController
import com.sensorlockdown.app.lockdown.model.EnforcementStatus

/**
 * Compatibility layer for Camera, Microphone, and Location privacy controls.
 * Respects platform version variations and enterprise policies.
 */
class PrivacyController(
    private val context: Context,
    private val devicePolicyController: DevicePolicyController
) {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val sensorPrivacyManager: Any? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService("sensor_privacy")
        } else {
            null
        }

    // ==========================================
    // Camera Privacy
    // ==========================================
    fun evaluateCameraCapability(): Pair<EnforcementStatus, String?> {
        return if (devicePolicyController.isAdminActive()) {
            Pair(EnforcementStatus.ENFORCED, "Enforceable via DevicePolicyManager.")
        } else {
            Pair(
                EnforcementStatus.UNSUPPORTED,
                "Requires Device Administrator or Device Owner authority to disable camera system-wide."
            )
        }
    }

    fun applyCameraRestriction(disable: Boolean): Boolean {
        return devicePolicyController.setCameraDisabled(disable)
    }

    fun isCameraRestricted(): Boolean {
        return devicePolicyController.isCameraDisabled()
    }

    private val audioLockdownWatchdog: AudioLockdownWatchdog = AudioLockdownWatchdog(context)

    // ==========================================
    // Microphone Privacy
    // ==========================================
    fun evaluateMicrophoneCapability(): Pair<EnforcementStatus, String?> {
        return Pair(
            EnforcementStatus.ENFORCED,
            "Enforced via in-call audio server watchdog and persistent telephony stream muting."
        )
    }

    fun applyMicrophoneRestriction(mute: Boolean): Boolean {
        return try {
            if (mute) {
                audioLockdownWatchdog.startEnforcement(
                    micLockdown = true,
                    speakerLockdown = audioLockdownWatchdog.isSpeakerLockdownActive
                )
            } else {
                audioLockdownWatchdog.stopEnforcement()
            }
            audioManager?.isMicrophoneMute = mute
            true
        } catch (e: Exception) {
            false
        }
    }

    fun applySpeakerRestriction(mute: Boolean): Boolean {
        return try {
            if (mute) {
                audioLockdownWatchdog.startEnforcement(
                    micLockdown = audioLockdownWatchdog.isMicrophoneLockdownActive,
                    speakerLockdown = true
                )
            } else {
                audioLockdownWatchdog.stopEnforcement()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isMicrophoneMuted(): Boolean {
        return audioManager?.isMicrophoneMute == true || audioLockdownWatchdog.isMicrophoneLockdownActive
    }

    fun isSpeakerMuted(): Boolean {
        return audioLockdownWatchdog.isSpeakerLockdownActive || audioManager?.isStreamMute(AudioManager.STREAM_MUSIC) == true
    }

    fun openSystemPrivacySettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_PRIVACY_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ignored: Exception) {}
        }
    }

    // ==========================================
    // Location Privacy
    // ==========================================
    fun evaluateLocationCapability(): Pair<EnforcementStatus, String?> {
        return if (devicePolicyController.isDeviceOwner()) {
            Pair(EnforcementStatus.ENFORCED, "Enforceable via Device Owner DISALLOW_SHARE_LOCATION policy.")
        } else {
            Pair(
                EnforcementStatus.UNSUPPORTED,
                "Requires Device Owner privileges to enforce device-wide location restriction across all apps."
            )
        }
    }

    fun applyLocationRestriction(restrict: Boolean): Boolean {
        return devicePolicyController.setLocationRestricted(restrict)
    }

    fun isLocationRestricted(): Boolean {
        return devicePolicyController.isLocationRestricted()
    }
}

