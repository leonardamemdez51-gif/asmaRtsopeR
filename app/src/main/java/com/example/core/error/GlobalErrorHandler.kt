package com.example.core.error

import com.example.core.logger.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalErrorHandler {
    private val _errorState = MutableStateFlow(AppErrorState())
    val errorState: StateFlow<AppErrorState> = _errorState.asStateFlow()

    fun handleThrowable(throwable: Throwable, customTag: String = "GlobalErrorHandler"): AppException {
        val appException = when (throwable) {
            is AppException -> throwable
            is java.io.IOException -> AppException.NetworkException(throwable.localizedMessage ?: "Error de red")
            is android.database.sqlite.SQLiteException -> AppException.DatabaseException(
                "Error de almacenamiento de base de datos",
                throwable
            )
            else -> AppException.UnknownException(throwable.localizedMessage ?: "Error no identificado", throwable)
        }

        AppLogger.e("[${appException.errorCode}] ${appException.message}", throwable, tag = customTag)

        _errorState.value = AppErrorState(
            hasError = true,
            errorCode = appException.errorCode,
            userFriendlyMessage = appException.message
        )

        return appException
    }

    fun clearError() {
        _errorState.value = AppErrorState(hasError = false)
    }

    fun notifyUserMessage(message: String, code: String = "INFO") {
        AppLogger.i("Notification to User: $message", tag = "GlobalErrorHandler")
        _errorState.value = AppErrorState(
            hasError = true,
            errorCode = code,
            userFriendlyMessage = message
        )
    }
}
