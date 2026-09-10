package com.example.core.sync

import com.example.core.logger.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncState {
    IDLE, SYNCING, SUCCESS, ERROR_OFFLINE
}

data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val pendingChangesCount: Int = 0,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

object SyncManager {
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun triggerSync(onCompleted: (Boolean) -> Unit = {}) {
        _syncStatus.value = _syncStatus.value.copy(state = SyncState.SYNCING)
        AppLogger.i("Starting local & remote database synchronization...", tag = "SyncManager")

        // Simulate background sync process (ready for REST API / Cloud integration)
        _syncStatus.value = SyncStatus(
            state = SyncState.SUCCESS,
            pendingChangesCount = 0,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        onCompleted(true)
    }

    fun markPendingOfflineChange() {
        val current = _syncStatus.value.pendingChangesCount
        _syncStatus.value = _syncStatus.value.copy(pendingChangesCount = current + 1)
        AppLogger.d("Marked offline transaction. Total pending: ${current + 1}", tag = "SyncManager")
    }
}
