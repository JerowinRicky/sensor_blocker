package com.sensorlockdown.app.authentication

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinValidationTest {

    @Test
    fun `validate - valid 6 digit pin succeeds`() {
        val result = PinValidation.validate("123456")
        assertThat(result).isEqualTo(PinValidation.ValidationResult.Valid)
    }

    @Test
    fun `validate - empty pin rejected`() {
        val result = PinValidation.validate("")
        assertThat(result).isInstanceOf(PinValidation.ValidationResult.Invalid::class.java)
        val error = (result as PinValidation.ValidationResult.Invalid).error
        assertThat(error).contains("empty")
    }

    @Test
    fun `validate - less than 6 digits rejected`() {
        val result = PinValidation.validate("12345")
        assertThat(result).isInstanceOf(PinValidation.ValidationResult.Invalid::class.java)
        val error = (result as PinValidation.ValidationResult.Invalid).error
        assertThat(error).contains("exactly 6")
    }

    @Test
    fun `validate - more than 6 digits rejected`() {
        val result = PinValidation.validate("1234567")
        assertThat(result).isInstanceOf(PinValidation.ValidationResult.Invalid::class.java)
    }

    @Test
    fun `validate - non-numeric characters rejected`() {
        val result = PinValidation.validate("12345a")
        assertThat(result).isInstanceOf(PinValidation.ValidationResult.Invalid::class.java)
        val error = (result as PinValidation.ValidationResult.Invalid).error
        assertThat(error).contains("numbers")
    }

    @Test
    fun `validateSetup - matching pins succeed`() {
        val result = PinValidation.validateSetup("654321", "654321")
        assertThat(result).isEqualTo(PinValidation.ValidationResult.Valid)
    }

    @Test
    fun `validateSetup - mismatched confirmation rejected`() {
        val result = PinValidation.validateSetup("123456", "123457")
        assertThat(result).isInstanceOf(PinValidation.ValidationResult.Invalid::class.java)
        val error = (result as PinValidation.ValidationResult.Invalid).error
        assertThat(error).contains("do not match")
    }
}

