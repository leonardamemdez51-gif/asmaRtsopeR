package com.example.core.error

sealed class AppException(
    override val message: String,
    val errorCode: String = "ERR_GENERIC",
    override val cause: Throwable? = null
) : Exception(message, cause) {

    class AuthenticationException(message: String = "Credenciales inválidas o usuario inactivo") :
        AppException(message, "ERR_AUTH")

    class DatabaseException(message: String, cause: Throwable? = null) :
        AppException(message, "ERR_DB", cause)

    class ValidationException(message: String) :
        AppException(message, "ERR_VALIDATION")

    class CashRegisterException(message: String) :
        AppException(message, "ERR_CASH_REGISTER")

    class InsufficientFundsException(message: String = "Fondos insuficientes en la caja activa") :
        AppException(message, "ERR_INSUFFICIENT_FUNDS")

    class NetworkException(message: String = "Error de conexión o sincronización de red") :
        AppException(message, "ERR_NETWORK")

    class UnknownException(message: String = "Ha ocurrido un error inesperado", cause: Throwable? = null) :
        AppException(message, "ERR_UNKNOWN", cause)
}

data class AppErrorState(
    val hasError: Boolean = false,
    val errorCode: String? = null,
    val userFriendlyMessage: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
