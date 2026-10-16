package com.sensorlockdown.app.authentication

import com.sensorlockdown.app.authentication.model.RateLimitState
import com.sensorlockdown.app.persistence.SecurityPreferences

/**
 * Enforces exponential backoff and lockout windows for failed PIN attempts.
 * Persists failure counters and lockout timestamps.
 */
class PinRateLimiter(
    private val securityPreferences: SecurityPreferences,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {

    fun getRateLimitState(): RateLimitState {
        val now = timeProvider()
        val expiry = securityPreferences.getLockoutExpiry()
        val failed = securityPreferences.getFailedAttempts()

        return if (expiry > now) {
            val secondsLeft = ((expiry - now) + 999) / 1000
            RateLimitState(
                isLockedOut = true,
                failedAttempts = failed,
                secondsRemaining = secondsLeft
            )
        } else {
            RateLimitState(
                isLockedOut = false,
                failedAttempts = failed,
                secondsRemaining = 0L
            )
        }
    }

    /**
     * Records a failed attempt. Calculates any applicable lockout period.
     * Returns the seconds of lockout applied, or 0 if within normal retry limits.
     */
    fun recordFailure(): Long {
        val currentFailed = securityPreferences.getFailedAttempts() + 1
        securityPreferences.setFailedAttempts(currentFailed)

        val lockoutSeconds = calculateLockoutSeconds(currentFailed)
        if (lockoutSeconds > 0) {
            val expiry = timeProvider() + (lockoutSeconds * 1000)
            securityPreferences.setLockoutExpiry(expiry)
        }
        return lockoutSeconds
    }

    /**
     * Resets failed attempts and lockout timers upon successful authentication.
     */
    fun recordSuccess() {
        securityPreferences.clearRateLimiting()
    }

    /**
     * Rate-limiting policy tier calculation:
     * - 1 to 4 attempts: 0s (Normal retry)
     * - 5 to 9 attempts: 30s delay
     * - 10 to 14 attempts: 300s (5 min) delay
     * - 15+ attempts: 1800s (30 min) delay
     */
    fun calculateLockoutSeconds(failedAttempts: Int): Long {
        return when {
            failedAttempts < 5 -> 0L
            failedAttempts < 10 -> 30L
            failedAttempts < 15 -> 300L
            else -> 1800L
        }
    }

    fun getRemainingAttemptsBeforeLockout(): Int {
        val failed = securityPreferences.getFailedAttempts()
        return when {
            failed < 5 -> 5 - failed
            failed < 10 -> 10 - failed
            failed < 15 -> 15 - failed
            else -> 1
        }
    }
}

