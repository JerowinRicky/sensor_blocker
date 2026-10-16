package com.sensorlockdown.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * Production-grade cryptographic operations manager.
 * Enforces hardware-backed Android Keystore integration, PBKDF2WithHmacSHA256 PIN derivation,
 * constant-time comparisons, and high-entropy secret generation.
 *
 * Plaintext PINs and private keys are never retained in persistent memory or logs.
 */
object CryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "SensorLockdownMasterKey"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 32

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure 32-byte (256-bit) salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Derives a cryptographic hash of a PIN using PBKDF2WithHmacSHA256 with 100,000 iterations.
     * Uses CharArray to allow explicit in-memory sanitization if desired.
     */
    fun hashPin(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Constant-time comparison between two byte arrays to prevent timing side-channel attacks.
     */
    fun verifyPin(enteredPin: CharArray, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val computedHash = hashPin(enteredPin, salt)
        return MessageDigest.isEqual(computedHash, expectedHash)
    }

    /**
     * Generates a high-entropy, cryptographically distinct emergency recovery secret.
     * Format: 24-character Base32/Alphanumeric string separated into readable blocks.
     */
    fun generateEmergencyRecoverySecret(): String {
        val randomBytes = ByteArray(15)
        secureRandom.nextBytes(randomBytes)
        val clean = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
            .replace("-", "X")
            .replace("_", "Y")
            .uppercase()
            .take(16)
        // Format as XXXX-XXXX-XXXX-XXXX
        return clean.chunked(4).joinToString("-")
    }

    /**
     * Initializes or retrieves the hardware-backed AES-256-GCM Master Key from Android Keystore.
     */
    fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_LENGTH_BITS)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts plaintext bytes using AES-GCM and the Android Keystore Master Key.
     * Output format: [12 bytes IV] + [Ciphertext + Auth Tag]
     */
    fun encryptWithMasterKey(plaintext: ByteArray): ByteArray {
        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    /**
     * Decrypts ciphertext bytes using AES-GCM and the Android Keystore Master Key.
     */
    fun decryptWithMasterKey(encryptedPayload: ByteArray): ByteArray {
        require(encryptedPayload.size > GCM_IV_LENGTH_BYTES) { "Invalid encrypted payload" }
        val secretKey = getOrCreateMasterKey()
        val iv = encryptedPayload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = encryptedPayload.copyOfRange(GCM_IV_LENGTH_BYTES, encryptedPayload.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }
}

