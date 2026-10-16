package com.sensorlockdown.app.lockdown.engine

import android.content.Context
import android.hardware.ConsumerIrManager
import android.media.AudioManager
import com.sensorlockdown.app.devicepolicy.DevicePolicyController
import com.sensorlockdown.app.lockdown.model.ControlId
import com.sensorlockdown.app.lockdown.model.ControlPolicy
import com.sensorlockdown.app.lockdown.model.EnforcementStatus
import com.sensorlockdown.app.privacy.PrivacyController
import com.sensorlockdown.app.sensors.SensorDiscoveryService

/**
 * Evaluates the real-time capability and enforceability of every control.
 * Strictly avoids fake security: distinguishes between physical presence and OS policy authority.
 */
class CapabilityEvaluator(
    private val context: Context,
    private val devicePolicyController: DevicePolicyController,
    private val privacyController: PrivacyController,
    private val sensorDiscoveryService: SensorDiscoveryService
) {

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun evaluateAllControls(requestedStates: Map<ControlId, Boolean> = emptyMap()): Map<ControlId, ControlPolicy> {
        val result = mutableMapOf<ControlId, ControlPolicy>()
        for (controlId in ControlId.values()) {
            val requested = requestedStates[controlId] ?: true
            result[controlId] = evaluateControl(controlId, requested)
        }
        return result
    }

    fun evaluateControl(controlId: ControlId, requestedState: Boolean): ControlPolicy {
        val isDeviceOwner = devicePolicyController.isDeviceOwner()
        val isAdmin = devicePolicyController.isAdminActive()

        return when (controlId) {
            ControlId.CAMERA -> {
                val (status, reason) = if (isAdmin) {
                    val enforced = devicePolicyController.isCameraDisabled()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Managed via Device Administrator.")
                } else {
                    Pair(EnforcementStatus.REQUESTED, "Device Administrator required. Tap switch to activate on this device.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Disables all device cameras system-wide.",
                    requestedState = requestedState,
                    supportedState = true,
                    enforcedState = if (isAdmin) devicePolicyController.isCameraDisabled() else false,
                    status = status,
                    reason = reason
                )
            }

            ControlId.SCREEN_CAPTURE -> {
                val (status, reason) = if (isAdmin) {
                    val enforced = devicePolicyController.isScreenCaptureDisabled()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Managed via Device Administrator.")
                } else {
                    Pair(EnforcementStatus.REQUESTED, "Device Administrator required. Tap switch to activate on this device.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Prevents screenshots and screen recording across the operating system.",
                    requestedState = requestedState,
                    supportedState = true,
                    enforcedState = if (isAdmin) devicePolicyController.isScreenCaptureDisabled() else false,
                    status = status,
                    reason = reason
                )
            }

            ControlId.MICROPHONE -> {
                val isMuted = privacyController.isMicrophoneMuted()
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Mutes audio input stream across the system, apps, and phone calls.",
                    requestedState = requestedState,
                    supportedState = true,
                    enforcedState = isMuted,
                    status = if (isMuted) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED,
                    reason = "Enforced via in-call audio server watchdog and telephony stream muting."
                )
            }

            ControlId.LOCATION -> {
                val (status, reason) = if (isDeviceOwner) {
                    val enforced = devicePolicyController.isLocationRestricted()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Enforced via Device Owner user restriction.")
                } else {
                    Pair(EnforcementStatus.REQUESTED, "Device Owner required for policy lock. Location can be toggled in System Settings.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Prevents location sharing across applications.",
                    requestedState = requestedState,
                    supportedState = true,
                    enforcedState = if (isDeviceOwner) devicePolicyController.isLocationRestricted() else false,
                    status = status,
                    reason = reason
                )
            }

            ControlId.USB_DATA -> {
                val (status, reason) = if (isDeviceOwner) {
                    val enforced = devicePolicyController.isUsbRestricted()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Enforced via DISALLOW_USB_FILE_TRANSFER and DISALLOW_DEBUGGING_FEATURES.")
                } else {
                    Pair(EnforcementStatus.UNSUPPORTED, "Requires Device Owner provisioning via ADB to block USB data buses.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Blocks USB data transfer and developer debugging features.",
                    requestedState = requestedState,
                    supportedState = isDeviceOwner,
                    enforcedState = devicePolicyController.isUsbRestricted(),
                    status = status,
                    reason = reason
                )
            }

            ControlId.BLUETOOTH -> {
                val (status, reason) = if (isDeviceOwner) {
                    val enforced = devicePolicyController.isBluetoothRestricted()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Enforced via DISALLOW_BLUETOOTH user restriction.")
                } else {
                    Pair(EnforcementStatus.REQUESTED, "Device Owner required for policy lock. Bluetooth can be toggled in System Settings.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Restricts Bluetooth radios and sharing.",
                    requestedState = requestedState,
                    supportedState = true,
                    enforcedState = if (isDeviceOwner) devicePolicyController.isBluetoothRestricted() else false,
                    status = status,
                    reason = reason
                )
            }

            ControlId.WIFI -> {
                val (status, reason) = if (isDeviceOwner) {
                    val enforced = devicePolicyController.isWifiConfigRestricted()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Enforced via DISALLOW_CONFIG_WIFI.")
                } else {
                    Pair(EnforcementStatus.REQUESTED, "Device Owner required for policy lock. Wi-Fi can be configured in System Settings.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Prevents unauthorized Wi-Fi configuration modifications.",
                    requestedState = requestedState,
                    supportedState = true,
                    enforcedState = if (isDeviceOwner) devicePolicyController.isWifiConfigRestricted() else false,
                    status = status,
                    reason = reason
            }

            ControlId.NFC -> {
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "System-wide NFC radio disable.",
                    requestedState = requestedState,
                    supportedState = false,
                    enforcedState = false,
                    status = EnforcementStatus.UNSUPPORTED,
                    reason = "Android AOSP does not expose a public API to disable NFC hardware without OEM proprietary SDKs."
                )
            }

            ControlId.IR_BLASTER -> {
                val hasIr = irManager?.hasIrEmitter() == true
                if (!hasIr) {
                    ControlPolicy(
                        controlId = controlId,
                        title = controlId.displayName,
                        description = "Infrared transmitter.",
                        requestedState = requestedState,
                        supportedState = false,
                        enforcedState = false,
                        status = EnforcementStatus.NOT_PRESENT,
                        reason = "No Infrared emitter detected on this hardware."
                    )
                } else {
                    ControlPolicy(
                        controlId = controlId,
                        title = controlId.displayName,
                        description = "Infrared transmitter.",
                        requestedState = requestedState,
                        supportedState = false,
                        enforcedState = false,
                        status = EnforcementStatus.UNSUPPORTED,
                        reason = "OEM does not expose a controllable system restriction API for IR emitter."
                    )
                }
            }

            ControlId.SPEAKER -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val isMuted = audioManager?.isStreamMute(AudioManager.STREAM_MUSIC) == true
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "System audio output muting.",
                    requestedState = requestedState,
                    supportedState = true,
                    enforcedState = isMuted,
                    status = if (isMuted) EnforcementStatus.PARTIALLY_ENFORCED else EnforcementStatus.REQUESTED,
                    reason = "Mutes media audio streams. Physical speaker hardware disable is not supported by AOSP."
                )
            }

            ControlId.ACCELEROMETER,
            ControlId.GYROSCOPE,
            ControlId.MAGNETOMETER,
            ControlId.PROXIMITY,
            ControlId.LIGHT_SENSOR,
            ControlId.BAROMETER -> {
                val (status, reason) = sensorDiscoveryService.getSensorRestrictionEnforceability(controlId)
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Physical sensor bus restriction.",
                    requestedState = requestedState,
                    supportedState = false,
                    enforcedState = false,
                    status = status,
                    reason = reason
                )
            }

            ControlId.APP_INSTALLATION -> {
                val (status, reason) = if (isDeviceOwner) {
                    val enforced = devicePolicyController.isAppInstallRestricted()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Enforced via DISALLOW_INSTALL_APPS.")
                } else {
                    Pair(EnforcementStatus.UNSUPPORTED, "Requires Device Owner to restrict new app installations.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Blocks installation of unapproved applications.",
                    requestedState = requestedState,
                    supportedState = isDeviceOwner,
                    enforcedState = devicePolicyController.isAppInstallRestricted(),
                    status = status,
                    reason = reason
                )
            }

            ControlId.APP_UNINSTALLATION -> {
                val (status, reason) = if (isDeviceOwner) {
                    val enforced = devicePolicyController.isUninstallBlocked()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Enforced via setUninstallBlocked.")
                } else {
                    Pair(EnforcementStatus.UNSUPPORTED, "Requires Device Owner to block removal of this application.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Protects this lockdown application against unauthorized uninstallation.",
                    requestedState = requestedState,
                    supportedState = isDeviceOwner,
                    enforcedState = devicePolicyController.isUninstallBlocked(),
                    status = status,
                    reason = reason
                )
            }

            ControlId.FACTORY_RESET -> {
                val (status, reason) = if (isDeviceOwner) {
                    val enforced = devicePolicyController.isFactoryResetRestricted()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Enforced via DISALLOW_FACTORY_RESET.")
                } else {
                    Pair(EnforcementStatus.UNSUPPORTED, "Requires Device Owner to prevent settings-based factory reset.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Restricts factory reset execution through settings.",
                    requestedState = requestedState,
                    supportedState = isDeviceOwner,
                    enforcedState = devicePolicyController.isFactoryResetRestricted(),
                    status = status,
                    reason = reason
                )
            }

            ControlId.SAFE_BOOT -> {
                val (status, reason) = if (isDeviceOwner) {
                    val enforced = devicePolicyController.isSafeBootRestricted()
                    Pair(if (enforced) EnforcementStatus.ENFORCED else EnforcementStatus.REQUESTED, "Enforced via DISALLOW_SAFE_BOOT.")
                } else {
                    Pair(EnforcementStatus.UNSUPPORTED, "Requires Device Owner to restrict safe boot rebooting.")
                }
                ControlPolicy(
                    controlId = controlId,
                    title = controlId.displayName,
                    description = "Blocks booting device into Safe Mode to bypass policies.",
                    requestedState = requestedState,
                    supportedState = isDeviceOwner,
                    enforcedState = devicePolicyController.isSafeBootRestricted(),
                    status = status,
                    reason = reason
                )
            }
        }
    }
}

