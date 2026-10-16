package com.sensorlockdown.emergency

import android.content.Context
import android.net.Uri
import android.os.Bundle

data class RecoveryResponse(
    val isSuccess: Boolean,
    val recoverySecret: String? = null,
    val errorMessage: String? = null,
    val timestamp: Long = 0L
)

/**
 * Client for communicating with the main Lockdown app via secure, signature-protected IPC.
 */
class EmergencyRecoveryClient(private val context: Context) {

    private val providerUri = Uri.parse("content://com.sensorlockdown.app.emergency")

    fun requestRecoverySecret(): RecoveryResponse {
        return try {
            val responseBundle: Bundle? = context.contentResolver.call(
                providerUri,
                METHOD_GET_RECOVERY_SECRET,
                null,
                null
            )

            if (responseBundle == null) {
                return RecoveryResponse(
                    isSuccess = false,
                    errorMessage = "No response from Lockdown Emergency Provider. Verify main app is installed."
                )
            }

            val success = responseBundle.getBoolean(KEY_SUCCESS, false)
            if (success) {
                val secret = responseBundle.getString(KEY_RECOVERY_SECRET)
                val timestamp = responseBundle.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
                RecoveryResponse(isSuccess = true, recoverySecret = secret, timestamp = timestamp)
            } else {
                val errorMsg = responseBundle.getString(KEY_ERROR_MESSAGE, "Identity verification failed.")
                RecoveryResponse(isSuccess = false, errorMessage = errorMsg)
            }
        } catch (e: SecurityException) {
            RecoveryResponse(
                isSuccess = false,
                errorMessage = "SecurityException: Calling app signature does not match main application certificate."
            )
        } catch (e: Exception) {
            RecoveryResponse(
                isSuccess = false,
                errorMessage = "IPC Communication failure: ${e.localizedMessage}"
            )
        }
    }

    companion object {
        const val METHOD_GET_RECOVERY_SECRET = "getRecoverySecret"
        const val KEY_SUCCESS = "key_success"
        const val KEY_RECOVERY_SECRET = "key_recovery_secret"
        const val KEY_ERROR_MESSAGE = "key_error_message"
        const val KEY_TIMESTAMP = "key_timestamp"
    }
}

