package com.sensorlockdown.app.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CryptoManagerTest {

    @Test
    fun `generateSalt - returns 32 unique bytes`() {
        val salt1 = CryptoManager.generateSalt()
        val salt2 = CryptoManager.generateSalt()

        assertThat(salt1).hasLength(32)
        assertThat(salt2).hasLength(32)
        assertThat(salt1).isNotEqualTo(salt2)
    }

    @Test
    fun `hashPin - produces consistent hash for same pin and salt`() {
        val pin = "123456".toCharArray()
        val salt = CryptoManager.generateSalt()

        val hash1 = CryptoManager.hashPin(pin, salt)
        val hash2 = CryptoManager.hashPin(pin, salt)

        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun `hashPin - produces different hash for different pin or salt`() {
        val pin1 = "123456".toCharArray()
        val pin2 = "654321".toCharArray()
        val salt = CryptoManager.generateSalt()

        val hash1 = CryptoManager.hashPin(pin1, salt)
        val hash2 = CryptoManager.hashPin(pin2, salt)

        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun `verifyPin - returns true for valid pin and false for incorrect pin`() {
        val pin = "987654".toCharArray()
        val wrongPin = "987653".toCharArray()
        val salt = CryptoManager.generateSalt()
        val expectedHash = CryptoManager.hashPin(pin, salt)

        assertThat(CryptoManager.verifyPin(pin, salt, expectedHash)).isTrue()
        assertThat(CryptoManager.verifyPin(wrongPin, salt, expectedHash)).isFalse()
    }

    @Test
    fun `generateEmergencyRecoverySecret - formats as readable 16-char blocks`() {
        val secret = CryptoManager.generateEmergencyRecoverySecret()
        // Format: XXXX-XXXX-XXXX-XXXX (length 19 with hyphens)
        assertThat(secret).matches("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")
    }
}

