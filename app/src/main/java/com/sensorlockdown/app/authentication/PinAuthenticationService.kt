package com.sensorlockdown.app.authentication

import com.sensorlockdown.app.authentication.model.AuthResult
import com.sensorlockdown.app.persistence.SecurityPreferences
import com.sensorlockdown.app.security.AuditLogger
import com.sensorlockdown.app.security.CryptoManager

/**
 * High-level PIN authentication service.
 * Coordinates validation, PBKDF2 hashing, Android Keystore protection,
 * brute-force rate-limiting, and structured audit event recording.
 */
class PinAuthenticationService(
    private val securityPreferences: SecurityPreferences,
    private val rateLimiter: PinRateLimiter,
    private val auditLogger: AuditLogger
) {

    fun isPinSet(): Boolean = securityPreferences.hasPin()

    /**
     * Creates and stores the initial PIN and generates the companion Emergency Recovery Secret.
     */
    fun setupInitialPin(pin: String, confirmPin: String): AuthResult {
        val validation = PinValidation.validateSetup(pin, confirmPin)
        if (validation is PinValidation.ValidationResult.Invalid) {
            return AuthResult.ValidationError(validation.error)
        }

        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPin(pin.toCharArray(), salt)
        securityPreferences.savePinCredentials(hash, salt)

        // Generate and safely persist separate emergency recovery secret
        val emergencySecret = CryptoManager.generateEmergencyRecoverySecret()
        securityPreferences.saveEmergencyRecoverySecret(emergencySecret)

        rateLimiter.recordSuccess()
        auditLogger.logEvent(
            eventType = AuditLogger.EventType.PIN_CREATED,
            details = "Initial 6-digit PIN created and emergency recovery material provisioned.",
            isSuccess = true
        )

        return AuthResult.Success
    }

    /**
     * Authenticates an entered PIN against stored cryptographic salt + PBKDF2 hash.
     * Enforces rate limiting before verification.
     */
    fun authenticate(pin: String): AuthResult {
        val rateLimitState = rateLimiter.getRateLimitState()
        if (rateLimitState.isLockedOut) {
            auditLogger.logEvent(
                eventType = AuditLogger.EventType.RATE_LIMIT_LOCKOUT,
                details = "Authentication attempt rejected due to active lockout (${rateLimitState.secondsRemaining}s remaining).",
                isSuccess = false
            )
            return AuthResult.LockedOut(rateLimitState.secondsRemaining)
        }

        val validation = PinValidation.validate(pin)
        if (validation is PinValidation.ValidationResult.Invalid) {
            return AuthResult.ValidationError(validation.error)
        }

        val storedHash = securityPreferences.getPinHash()
        val storedSalt = securityPreferences.getPinSalt()

        if (storedHash == null || storedSalt == null) {
            return AuthResult.ValidationError("No PIN is currently configured on this device.")
        }

        val isMatch = CryptoManager.verifyPin(pin.toCharArray(), storedSalt, storedHash)
        return if (isMatch) {
            rateLimiter.recordSuccess()
            auditLogger.logEvent(
                eventType = AuditLogger.EventType.LOGIN_SUCCESS,
                details = "User successfully authenticated with 6-digit PIN.",
                isSuccess = true
            )
            AuthResult.Success
        } else {
            val lockoutSeconds = rateLimiter.recordFailure()
            auditLogger.logEvent(
                eventType = AuditLogger.EventType.LOGIN_FAILED,
                details = "PIN verification failed. Total failures recorded: ${securityPreferences.getFailedAttempts()}.",
                isSuccess = false
            )
            if (lockoutSeconds > 0) {
                AuthResult.LockedOut(lockoutSeconds)
            } else {
                AuthResult.InvalidPin(rateLimiter.getRemainingAttemptsBeforeLockout())
            }
        }
    }

    /**
     * Changes existing PIN after verifying the current PIN.
     * Preserves lockdown and rate limiting state.
     */
    fun changePin(currentPin: String, newPin: String, confirmNewPin: String): AuthResult {
        val authResult = authenticate(currentPin)
        if (authResult !is AuthResult.Success) {
            return authResult
        }

        val validation = PinValidation.validateSetup(newPin, confirmNewPin)
        if (validation is PinValidation.ValidationResult.Invalid) {
            return AuthResult.ValidationError(validation.error)
        }

        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPin(newPin.toCharArray(), salt)
        securityPreferences.savePinCredentials(hash, salt)

        auditLogger.logEvent(
            eventType = AuditLogger.EventType.PIN_CHANGED,
            details = "PIN was successfully updated by authorized user.",
            isSuccess = true
        )

        return AuthResult.Success
    }
}
