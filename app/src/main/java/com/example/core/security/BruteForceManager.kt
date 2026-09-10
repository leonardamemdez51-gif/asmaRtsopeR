package com.example.core.security

import com.example.core.logger.AppLogger

object BruteForceManager {

    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 60 * 1000L // 60 seconds lockout for rapid recovery in demo

    fun isUserLocked(failedAttempts: Int, lockedUntilMs: Long): Boolean {
        if (failedAttempts < MAX_FAILED_ATTEMPTS) return false
        val now = System.currentTimeMillis()
        return now < lockedUntilMs
    }

    fun getRemainingLockoutSeconds(lockedUntilMs: Long): Int {
        val now = System.currentTimeMillis()
        val diffMs = lockedUntilMs - now
        return if (diffMs > 0) (diffMs / 1000).toInt() + 1 else 0
    }

    fun calculateNewLockoutTime(): Long {
        return System.currentTimeMillis() + LOCKOUT_DURATION_MS
    }
}
