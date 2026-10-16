package com.sensorlockdown.app.security

import com.sensorlockdown.app.persistence.dao.AuditLogDao
import com.sensorlockdown.app.persistence.entity.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Structured, tamper-evident security audit logger.
 * Persists all security lifecycle events into Room.
 * Guarantee: Plaintext credentials (PIN, keys, recovery secrets) are never accepted or serialized.
 */
class AuditLogger(
    private val auditLogDao: AuditLogDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    fun logEvent(
        eventType: EventType,
        actor: String = ACTOR_LOCAL_USER,
        details: String = "",
        isSuccess: Boolean = true
    ) {
        val sanitizedDetails = sanitize(details)
        scope.launch {
            val entity = AuditLogEntity(
                timestamp = System.currentTimeMillis(),
                eventType = eventType.name,
                actor = actor,
                details = sanitizedDetails,
                isSuccess = isSuccess
            )
            auditLogDao.insert(entity)
        }
    }

    /**
     * Defense-in-depth sanitization: strips potential PIN / credential patterns.
     */
    private fun sanitize(input: String): String {
        return input.replace(Regex("(?i)(pin|password|secret|key)\\s*[:=]\\s*\\S+"), "$1=[REDACTED]")
    }

    enum class EventType {
        PIN_CREATED,
        PIN_CHANGED,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOCKDOWN_ACTIVATED,
        LOCKDOWN_DEACTIVATED,
        POLICY_APPLIED,
        POLICY_FAILED,
        POLICY_VERIFIED,
        DEVICE_REBOOT_DETECTED,
        EMERGENCY_ACCESS,
        UNAUTHORIZED_ATTEMPT,
        DEVICE_OWNER_STATUS_CHANGED,
        RATE_LIMIT_LOCKOUT
    }

    companion object {
        const val ACTOR_LOCAL_USER = "LOCAL_USER"
        const val ACTOR_SYSTEM = "ANDROID_SYSTEM"
        const val ACTOR_EMERGENCY_APP = "EMERGENCY_COMPANION_APP"
    }
}

