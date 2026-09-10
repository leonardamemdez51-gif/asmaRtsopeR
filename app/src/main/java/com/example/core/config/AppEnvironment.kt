package com.example.core.config

enum class AppEnvironment(
    val envName: String,
    val apiBaseUrl: String,
    val enableDebugLogs: Boolean,
    val isOfflineSyncEnabled: Boolean,
    val maxLoanLimit: Double
) {
    DEVELOPMENT(
        envName = "Desarrollo",
        apiBaseUrl = "https://dev-api.rama-erp.com/v1/",
        enableDebugLogs = true,
        isOfflineSyncEnabled = true,
        maxLoanLimit = 50000.0
    ),
    STAGING(
        envName = "Pruebas",
        apiBaseUrl = "https://staging-api.rama-erp.com/v1/",
        enableDebugLogs = true,
        isOfflineSyncEnabled = true,
        maxLoanLimit = 100000.0
    ),
    PRODUCTION(
        envName = "Producción",
        apiBaseUrl = "https://api.rama-erp.com/v1/",
        enableDebugLogs = false,
        isOfflineSyncEnabled = true,
        maxLoanLimit = 200000.0
    );

    companion object {
        var currentEnvironment: AppEnvironment = DEVELOPMENT
    }
}

object AppConfig {
    const val APP_NAME = "RAMA ERP Microfinanzas"
    const val APP_VERSION = "1.1.1"
    const val DEFAULT_CURRENCY_CODE = "MXN"
    const val DEFAULT_ZONE = "Zona Centro"
    const val DEFAULT_LATE_FEE_PERCENTAGE = 0.05
    const val MAX_OFFLINE_QUEUE_ITEMS = 500

    fun getActiveEnvironment(): AppEnvironment = AppEnvironment.currentEnvironment

    fun setEnvironment(env: AppEnvironment) {
        AppEnvironment.currentEnvironment = env
    }
}
