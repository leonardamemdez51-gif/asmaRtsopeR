package com.example.core.logger

import android.util.Log
import com.example.core.config.AppEnvironment

object AppLogger {
    private const val DEFAULT_TAG = "RAMA_ERP"

    enum class Level {
        DEBUG, INFO, WARN, ERROR, AUDIT
    }

    fun d(message: String, tag: String = DEFAULT_TAG) {
        if (AppEnvironment.currentEnvironment.enableDebugLogs) {
            Log.d(tag, " [DEBUG] $message")
        }
    }

    fun i(message: String, tag: String = DEFAULT_TAG) {
        Log.i(tag, "ℹ️ [INFO] $message")
    }

    fun w(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        if (throwable != null) {
            Log.w(tag, "⚠️ [WARN] $message", throwable)
        } else {
            Log.w(tag, "⚠️ [WARN] $message")
        }
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        if (throwable != null) {
            Log.e(tag, "🚨 [ERROR] $message", throwable)
        } else {
            Log.e(tag, "🚨 [ERROR] $message")
        }
    }

    fun audit(action: String, user: String, details: String, tag: String = DEFAULT_TAG) {
        Log.i(tag, "🔒 [AUDIT] User: '$user' | Action: '$action' | Details: $details")
    }
}
