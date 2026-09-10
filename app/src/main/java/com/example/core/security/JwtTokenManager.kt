package com.example.core.security

import com.example.core.logger.AppLogger
import org.json.JSONObject
import android.util.Base64

data class JwtClaims(
    val userId: Long,
    val username: String,
    val email: String,
    val role: String,
    val permissions: List<String>,
    val issuedAtMs: Long,
    val expiresAtMs: Long,
    val tokenId: String
)

object JwtTokenManager {

    private const val DEFAULT_TOKEN_VALIDITY_MS = 60 * 60 * 1000L // 1 hour
    private const val REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L // 7 days

    fun generateAccessToken(
        userId: Long,
        username: String,
        email: String,
        role: String,
        permissions: Set<AppPermission>,
        customExpiryMs: Long? = null
    ): String {
        val now = System.currentTimeMillis()
        val expiry = now + (customExpiryMs ?: DEFAULT_TOKEN_VALIDITY_MS)
        val tokenId = SecurityUtils.generateRandomToken(16)

        val header = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }

        val payload = JSONObject().apply {
            put("sub", userId)
            put("username", username)
            put("email", email)
            put("role", role)
            put("permissions", permissions.map { it.code })
            put("iat", now / 1000)
            put("exp", expiry / 1000)
            put("jti", tokenId)
        }

        val encodedHeader = Base64.encodeToString(header.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val encodedPayload = Base64.encodeToString(payload.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val signature = SecurityUtils.generateRandomToken(24) // Simulated signature

        return "$encodedHeader.$encodedPayload.$signature"
    }

    fun generateRefreshToken(userId: Long): String {
        return "rt_${userId}_${SecurityUtils.generateRandomToken(32)}"
    }

    fun parseToken(token: String): JwtClaims? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payloadStr = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payloadStr)

            val permissionsArray = json.optJSONArray("permissions")
            val permissionsList = mutableListOf<String>()
            if (permissionsArray != null) {
                for (i in 0 until permissionsArray.length()) {
                    permissionsList.add(permissionsArray.getString(i))
                }
            }

            JwtClaims(
                userId = json.getLong("sub"),
                username = json.getString("username"),
                email = json.optString("email", ""),
                role = json.getString("role"),
                permissions = permissionsList,
                issuedAtMs = json.getLong("iat") * 1000,
                expiresAtMs = json.getLong("exp") * 1000,
                tokenId = json.optString("jti", "")
            )
        } catch (e: Exception) {
            AppLogger.e("Error parsing JWT token", e, tag = "JwtTokenManager")
            null
        }
    }

    fun isTokenExpired(token: String): Boolean {
        val claims = parseToken(token) ?: return true
        return System.currentTimeMillis() >= claims.expiresAtMs
    }
}
