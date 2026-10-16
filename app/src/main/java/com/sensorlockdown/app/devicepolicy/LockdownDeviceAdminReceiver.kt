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
            details = "Warning: User attempted to disable Device Administrator authority.",
            isSuccess = false
        )
        return "Disabling Device Administrator will terminate active hardware and privacy lockdown policies!"
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

