package com.example.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import android.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_BYTE_SIZE = 16

    fun hashPassword(password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTE_SIZE)
        random.nextBytes(salt)

        val hash = pbkdf2(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        return "pbkdf2:$ITERATION_COUNT:$saltBase64:$hashBase64"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false

        // Check legacy seed plain-text passwords (e.g. "admin123", "cobrador123")
        if (!storedHash.startsWith("pbkdf2:")) {
            return password == storedHash || sha256(password) == storedHash
        }

        return try {
            val parts = storedHash.split(":")
            if (parts.size != 4) return false

            val iterations = parts[1].toInt()
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expectedHash = Base64.decode(parts[3], Base64.NO_WRAP)

            val actualHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.size * 8)
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (e: Exception) {
            false
        }
    }

    fun generateRandomToken(length: Int = 32): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLengthBits: Int): ByteArray {
        val spec: KeySpec = PBEKeySpec(password, salt, iterations, keyLengthBits)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
