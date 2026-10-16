package com.sensorlockdown.app.emergency

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import com.sensorlockdown.app.persistence.SecurityPreferences
import com.sensorlockdown.app.security.AuditLogger

/**
 * Manages authorized emergency recovery operations and IPC identity verification.
 */
class EmergencyRecoveryManager(
    private val context: Context,
    private val securityPreferences: SecurityPreferences,
    private val auditLogger: AuditLogger,
    private val callingUidProvider: () -> Int = { runCatching { Binder.getCallingUid() }.getOrDefault(-1) }
) {

    /**
     * Verifies that the IPC caller is signed by the exact same certificate as this application.
     */
    fun isCallerAuthorized(callingPackage: String?): Boolean {
        if (callingPackage.isNullOrEmpty()) {
            return false
        }
        val callingUid = callingUidProvider()
        val packageUid = try {
            context.packageManager.getPackageUid(callingPackage, 0)
        } catch (e: Exception) {
            -1
        }
        if (callingUid != -1 && packageUid != -1 && callingUid != packageUid) {
            return false
        }

        val match = try {
            context.packageManager.checkSignatures(context.packageName, callingPackage)
        } catch (e: Exception) {
            PackageManager.SIGNATURE_NO_MATCH
        }
        return match == PackageManager.SIGNATURE_MATCH
    }

    /**
     * Retrieves the emergency recovery secret after verifying caller identity.
     */
    fun getAuthorizedRecoverySecret(callingPackage: String?): String? {
        val authorized = isCallerAuthorized(callingPackage)
        if (!authorized) {
            auditLogger.logEvent(
                eventType = AuditLogger.EventType.UNAUTHORIZED_ATTEMPT,
                actor = callingPackage ?: "UNKNOWN_CALLER",
                details = "Unauthorized emergency recovery IPC attempt blocked. Signature mismatch.",
                isSuccess = false
            )
            return null
        }

        val secret = securityPreferences.getEmergencyRecoverySecret()
        auditLogger.logEvent(
            eventType = AuditLogger.EventType.EMERGENCY_ACCESS,
            actor = callingPackage ?: AuditLogger.ACTOR_EMERGENCY_APP,
            details = "Emergency recovery credential retrieved by authorized companion app.",
            isSuccess = true
        )
        return secret
    }
}

