package com.sensorlockdown.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sensorlockdown.app.devicepolicy.DevicePolicyController
import com.sensorlockdown.app.lockdown.engine.CapabilityEvaluator
import com.sensorlockdown.app.lockdown.engine.LockdownEngine
import com.sensorlockdown.app.persistence.LockdownDatabase
import com.sensorlockdown.app.privacy.PrivacyController
import com.sensorlockdown.app.security.AuditLogger
import com.sensorlockdown.app.sensors.SensorDiscoveryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles device reboot. Verifies active device policies and logs the reboot event.
 * Avoids continuously polling or running unneeded background services.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val db = LockdownDatabase.getInstance(context)
                val auditLogger = AuditLogger(db.auditLogDao())
                val dpc = DevicePolicyController(context)
                val sensorDiscovery = SensorDiscoveryService(context)
                val privacyController = PrivacyController(context, dpc)
                val capabilityEvaluator = CapabilityEvaluator(context, dpc, privacyController, sensorDiscovery)
                val engine = LockdownEngine(
                    capabilityEvaluator = capabilityEvaluator,
                    devicePolicyController = dpc,
                    privacyController = privacyController,
                    audioManager = null,
                    policyDao = db.policyDao(),
                    auditLogger = auditLogger
                )

                auditLogger.logEvent(
                    eventType = AuditLogger.EventType.DEVICE_REBOOT_DETECTED,
                    actor = AuditLogger.ACTOR_SYSTEM,
                    details = "Device reboot detected. Re-evaluating active hardware and sensor policies.",
                    isSuccess = true
                )

                engine.verifyAndSyncPolicy()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

