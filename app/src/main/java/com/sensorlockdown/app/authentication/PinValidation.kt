package com.sensorlockdown.app.authentication

object PinValidation {

    const val REQUIRED_PIN_LENGTH = 6

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val error: String) : ValidationResult()
    }

    fun validate(pin: String): ValidationResult {
        if (pin.isEmpty()) {
            return ValidationResult.Invalid("PIN cannot be empty.")
        }
        if (pin.length != REQUIRED_PIN_LENGTH) {
            return ValidationResult.Invalid("PIN must be exactly $REQUIRED_PIN_LENGTH numeric digits.")
        }
        if (!pin.all { it.isDigit() }) {
            return ValidationResult.Invalid("PIN must contain only numbers (0-9).")
        }
        return ValidationResult.Valid
    }

    fun validateSetup(pin: String, confirmPin: String): ValidationResult {
        val firstCheck = validate(pin)
        if (firstCheck is ValidationResult.Invalid) return firstCheck

        if (pin != confirmPin) {
            return ValidationResult.Invalid("PINs do not match.")
        }
        return ValidationResult.Valid
    }
}

