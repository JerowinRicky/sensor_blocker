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

    // ==========================================
    // Microphone Privacy
    // ==========================================
    fun evaluateMicrophoneCapability(): Pair<EnforcementStatus, String?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ SensorPrivacyManager exists, but requires MANAGE_SENSOR_PRIVACY (system-only)
            // Or DevicePolicyManager microphone restriction where supported
            Pair(
                EnforcementStatus.PARTIALLY_ENFORCED,
                "Microphone privacy toggle is restricted to System UI on Android 12+. Audio input muting applied where permitted."
            )
        } else {
            Pair(
                EnforcementStatus.PARTIALLY_ENFORCED,
                "Global microphone hardware cut is not exposed to non-system apps. Audio stream mute fallback available."
            )
        }
    }

    fun applyMicrophoneRestriction(mute: Boolean): Boolean {
        return try {
            audioManager?.isMicrophoneMute = mute
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isMicrophoneMuted(): Boolean {
        return audioManager?.isMicrophoneMute == true
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

