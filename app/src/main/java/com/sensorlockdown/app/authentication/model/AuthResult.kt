package com.sensorlockdown.app.authentication.model

sealed class AuthResult {
    object Success : AuthResult()
    data class InvalidPin(val remainingAttemptsBeforeLockout: Int) : AuthResult()
    data class LockedOut(val lockoutSecondsRemaining: Long) : AuthResult()
    data class ValidationError(val message: String) : AuthResult()
}

data class RateLimitState(
    val isLockedOut: Boolean,
    val failedAttempts: Int,
    val secondsRemaining: Long
)
