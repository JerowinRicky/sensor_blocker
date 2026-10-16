package com.sensorlockdown.app.devicepolicy

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.sensorlockdown.app.persistence.LockdownDatabase
import com.sensorlockdown.app.security.AuditLogger

/**
 * Android Device Admin Receiver.
 * Responds to lifecycle changes in Device Admin / Device Owner authority.
 */
class LockdownDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        val database = LockdownDatabase.getInstance(context)
        val auditLogger = AuditLogger(database.auditLogDao())
        auditLogger.logEvent(
            eventType = AuditLogger.EventType.DEVICE_OWNER_STATUS_CHANGED,
            actor = AuditLogger.ACTOR_SYSTEM,
            details = "Device Administrator privileges granted to Lockdown system.",
            isSuccess = true
        )
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val database = LockdownDatabase.getInstance(context)
        val auditLogger = AuditLogger(database.auditLogDao())
        auditLogger.logEvent(
            eventType = AuditLogger.EventType.UNAUTHORIZED_ATTEMPT,
            actor = AuditLogger.ACTOR_SYSTEM,
            details = "Unauthorized attempt to disable Device Administrator authority.",
            isSuccess = false
        )
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
            dpm?.lockNow()
        } catch (ignored: Exception) {}

        return "PROTECTED APP: Uninstallation and deactivation require authentication with your 6-digit PIN inside the Sensor Lockdown app."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val database = LockdownDatabase.getInstance(context)
        val auditLogger = AuditLogger(database.auditLogDao())
        auditLogger.logEvent(
            eventType = AuditLogger.EventType.DEVICE_OWNER_STATUS_CHANGED,
            actor = AuditLogger.ACTOR_SYSTEM,
            details = "Device Administrator privileges revoked.",
            isSuccess = false
        )
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, LockdownDeviceAdminReceiver::class.java)
        }
    }
}

