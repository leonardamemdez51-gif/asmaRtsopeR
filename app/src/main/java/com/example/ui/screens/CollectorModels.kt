package com.example.ui.screens

import com.example.data.local.ClientEntity
import com.example.data.local.InstallmentEntity
import com.example.data.local.LoanEntity

enum class CollectorTaskCategory(val displayName: String) {
    TODAS("Todas"),
    CRITICO("Crítico"),
    ATRASADO("Atrasado"),
    EN_MORA("En Mora"),
    PAGO_DE_HOY("Pago de Hoy"),
    PROMESA_DE_PAGO("Promesa de Pago"),
    PAGO_PENDIENTE("Pago Pendiente"),
    VISITA_PENDIENTE("Visita Pendiente"),
    AL_CORRIENTE("Al Corriente")
}

enum class TaskPriority(val level: Int, val label: String) {
    CRITICO(6, "Crítico"),
    ATRASADO(5, "Atrasado"),
    PAGO_HOY(4, "Pago Hoy"),
    PROMESA(3, "Promesa"),
    PENDIENTE(2, "Pendiente"),
    AL_CORRIENTE(1, "Al Corriente")
}

data class CollectorTaskItem(
    val id: Long,
    val client: ClientEntity,
    val loan: LoanEntity,
    val installment: InstallmentEntity,
    val category: CollectorTaskCategory,
    val priority: TaskPriority,
    val amountToCollect: Double,
    val overdueAmount: Double,
    val totalRemainingBalance: Double,
    val totalInstallments: Int,
    val remainingInstallmentsCount: Int,
    val preferredPaymentMethod: String,
    val lastVisitDateFormatted: String,
    val lastVisitResult: String,
    val punctualityScore: Int,
    val promisedPaymentDateFormatted: String? = null,
    val promisedAmount: Double? = null,
    val isCompletedToday: Boolean = false
)

data class CollectorDailySummary(
    val totalExpectedAmount: Double = 0.0,
    val totalCollectedAmount: Double = 0.0,
    val totalCashCollected: Double = 0.0,
    val totalTransferCollected: Double = 0.0,
    val totalPendingAmount: Double = 0.0,
    val compliancePercentage: Double = 0.0,
    val totalTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val pendingTasksCount: Int = 0,
    val overdueTasksCount: Int = 0,
    val criticalTasksCount: Int = 0
)

data class CollectorScreenState(
    val searchQuery: String = "",
    val selectedCategory: CollectorTaskCategory = CollectorTaskCategory.TODAS,
    val summary: CollectorDailySummary = CollectorDailySummary(),
    val tasks: List<CollectorTaskItem> = emptyList(),
    val canRegisterPayment: Boolean = false,
    val isOfflineMode: Boolean = true,
    val lastSyncFormatted: String = "Hace un momento (Offline Local OK)"
)
