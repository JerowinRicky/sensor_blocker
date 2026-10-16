package com.sensorlockdown.app.emergency

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.sensorlockdown.app.persistence.LockdownDatabase
import com.sensorlockdown.app.persistence.SecurityPreferences
import com.sensorlockdown.app.security.AuditLogger

/**
 * Secure ContentProvider IPC endpoint.
 * Protected by signature-level permission and runtime package signature verification.
 */
class EmergencyRecoveryProvider : ContentProvider() {

    private lateinit var recoveryManager: EmergencyRecoveryManager

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        val db = LockdownDatabase.getInstance(ctx)
        val auditLogger = AuditLogger(db.auditLogDao())
        val prefs = SecurityPreferences(ctx)
        recoveryManager = EmergencyRecoveryManager(ctx, prefs, auditLogger)
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method == METHOD_GET_RECOVERY_SECRET) {
            val caller = callingPackage
            val secret = recoveryManager.getAuthorizedRecoverySecret(caller)
            if (secret == null) {
                val errBundle = Bundle()
                errBundle.putBoolean(KEY_SUCCESS, false)
                errBundle.putString(KEY_ERROR_MESSAGE, "Caller identity verification failed.")
                return errBundle
            }

            val bundle = Bundle()
            bundle.putBoolean(KEY_SUCCESS, true)
            bundle.putString(KEY_RECOVERY_SECRET, secret)
            bundle.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            return bundle
        }
        return super.call(method, arg, extras)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        const val AUTHORITY = "com.sensorlockdown.app.emergency"
        const val METHOD_GET_RECOVERY_SECRET = "getRecoverySecret"
        const val KEY_SUCCESS = "key_success"
        const val KEY_RECOVERY_SECRET = "key_recovery_secret"
        const val KEY_ERROR_MESSAGE = "key_error_message"
        const val KEY_TIMESTAMP = "key_timestamp"
    }
}

