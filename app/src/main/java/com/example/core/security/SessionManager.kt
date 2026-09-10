package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import com.example.core.logger.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSessionState(
    val isLoggedIn: Boolean = false,
    val userId: Long = 0L,
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "CONSULTA",
    val assignedZone: String = "Zona Centro",
    val permissions: Set<AppPermission> = emptySet(),
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val lastActiveTimestampMs: Long = System.currentTimeMillis()
)

object SessionManager {

    private const val PREFS_NAME = "rama_session_prefs"
    private const val KEY_REMEMBER_ME = "key_remember_me"
    private const val KEY_SAVED_USERNAME = "key_saved_username"
    private const val KEY_SAVED_TOKEN = "key_saved_token"
    private const val KEY_SAVED_REFRESH = "key_saved_refresh"
    private const val INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes inactivity timeout

    private val _sessionState = MutableStateFlow(UserSessionState())
    val sessionState: StateFlow<UserSessionState> = _sessionState.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        AppLogger.i("SessionManager initialized", tag = "SessionManager")
    }

    fun startSession(
        userId: Long,
        username: String,
        fullName: String,
        email: String,
        role: String,
        zone: String,
        permissions: Set<AppPermission>,
        accessToken: String,
        refreshToken: String,
        rememberMe: Boolean
    ) {
        val newState = UserSessionState(
            isLoggedIn = true,
            userId = userId,
            username = username,
            fullName = fullName,
            email = email,
            role = role,
            assignedZone = zone,
            permissions = permissions,
            accessToken = accessToken,
            refreshToken = refreshToken,
            lastActiveTimestampMs = System.currentTimeMillis()
        )
        _sessionState.value = newState

        prefs?.edit()?.apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe) {
                putString(KEY_SAVED_USERNAME, username)
                putString(KEY_SAVED_TOKEN, accessToken)
                putString(KEY_SAVED_REFRESH, refreshToken)
            } else {
                remove(KEY_SAVED_USERNAME)
                remove(KEY_SAVED_TOKEN)
                remove(KEY_SAVED_REFRESH)
            }
            apply()
        }
        AppLogger.i("Session started for $username. RememberMe: $rememberMe", tag = "SessionManager")
    }

    fun updateLastActive() {
        if (_sessionState.value.isLoggedIn) {
            _sessionState.value = _sessionState.value.copy(
                lastActiveTimestampMs = System.currentTimeMillis()
            )
        }
    }

    fun checkInactivityTimeout(): Boolean {
        val state = _sessionState.value
        if (!state.isLoggedIn) return false

        val idleTime = System.currentTimeMillis() - state.lastActiveTimestampMs
        if (idleTime > INACTIVITY_TIMEOUT_MS) {
            AppLogger.w("Session expired due to inactivity ($idleTime ms)", tag = "SessionManager")
            clearSession()
            return true
        }
        return false
    }

    fun clearSession() {
        _sessionState.value = UserSessionState(isLoggedIn = false)
        prefs?.edit()?.apply {
            remove(KEY_SAVED_TOKEN)
            remove(KEY_SAVED_REFRESH)
            apply()
        }
        AppLogger.i("Session cleared", tag = "SessionManager")
    }

    fun getRememberedUsername(): String? {
        return if (prefs?.getBoolean(KEY_REMEMBER_ME, false) == true) {
            prefs?.getString(KEY_SAVED_USERNAME, null)
        } else null
    }

    fun getRememberedToken(): String? {
        return if (prefs?.getBoolean(KEY_REMEMBER_ME, false) == true) {
            prefs?.getString(KEY_SAVED_TOKEN, null)
        } else null
    }

    fun hasPermission(permission: AppPermission): Boolean {
        val state = _sessionState.value
        if (!state.isLoggedIn) return false
        if (state.role.equals("ADMINISTRADOR", ignoreCase = true)) return true
        return state.permissions.contains(permission)
    }
}
