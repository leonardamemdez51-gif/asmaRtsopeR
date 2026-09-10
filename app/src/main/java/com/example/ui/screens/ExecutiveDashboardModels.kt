package com.example.ui.screens

import com.example.data.local.ClientEntity
import com.example.data.local.LoanEntity
import com.example.ui.DailyTaskItem

data class DashboardFilterState(
    val dateRange: String = "MES", // "HOY", "SEMANA", "MES", "ANIO", "TODO"
    val branch: String = "TODAS",
    val route: String = "TODAS",
    val collectorName: String = "TODOS",
    val supervisorName: String = "TODOS"
)

data class ExecutiveKpiMetrics(
    // Clientes
    val activeClientsCount: Int = 0,
    val newClientsCount: Int = 0,
    val blockedClientsCount: Int = 0,
    // Préstamos
    val activeLoansCount: Int = 0,
    val liquidatedLoansCount: Int = 0,
    val overdueLoansCount: Int = 0,
    // Capital
    val activePortfolioAmount: Double = 0.0,
    val overduePortfolioAmount: Double = 0.0,
    val placedCapital: Double = 0.0,
    val recoveredCapital: Double = 0.0,
    val interestCollected: Double = 0.0,
    // Cobranza
    val expectedCollection: Double = 0.0,
    val realizedCollection: Double = 0.0,
    val pendingCollection: Double = 0.0,
    val cashCollection: Double = 0.0,
    val transferCollection: Double = 0.0,
    val lateFeeAmount: Double = 0.0,
    val renewalsCount: Int = 0,
    // Operación diaria & Score
    val todayPaymentsAmount: Double = 0.0,
    val overdueInstallmentsCount: Int = 0,
    val averageClientScore: Double = 0.0
)

data class ChartDataPoint(
    val label: String,
    val value: Double,
    val count: Int = 0,
    val secondaryValue: Double = 0.0
)

data class ExecutiveChartsData(
    val monthlyCollection: List<ChartDataPoint> = emptyList(),
    val weeklyCollection: List<ChartDataPoint> = emptyList(),
    val portfolioGrowth: List<ChartDataPoint> = emptyList(),
    val distributionByZone: List<ChartDataPoint> = emptyList(),
    val distributionByCollector: List<ChartDataPoint> = emptyList(),
    val loansByPlan: List<ChartDataPoint> = emptyList(),
    val clientsByStatus: List<ChartDataPoint> = emptyList()
)

data class ExecutiveAlertsData(
    val criticalClients: List<ClientEntity> = emptyList(),
    val overdueLoans: List<LoanEntity> = emptyList(),
    val topDailyDueInstallments: List<DailyTaskItem> = emptyList(),
    val eligibleRenewalLoans: List<LoanEntity> = emptyList()
)

data class CollectorPerformance(
    val name: String,
    val expected: Double,
    val realized: Double,
    val percentage: Double,
    val clientsAssigned: Int,
    val clientsCollected: Int,
    val pending: Int,
    val late: Int
)

data class SupervisorPerformance(
    val name: String,
    val collectors: Int,
    val expected: Double,
    val realized: Double,
    val percentage: Double,
    val lateAmount: Double,
    val activePortfolio: Double
)

data class RecentActivity(
    val id: Long = 0,
    val type: String, // "NUEVO_CLIENTE", "PAGO", "RENOVACION", etc.
    val user: String,
    val date: Long,
    val status: String,
    val description: String
)

data class ExecutiveDashboardState(
    val filter: DashboardFilterState = DashboardFilterState(),
    val kpis: ExecutiveKpiMetrics = ExecutiveKpiMetrics(),
    val charts: ExecutiveChartsData = ExecutiveChartsData(),
    val alerts: ExecutiveAlertsData = ExecutiveAlertsData(),
    val collectorPerformances: List<CollectorPerformance> = emptyList(),
    val supervisorPerformances: List<SupervisorPerformance> = emptyList(),
    val recentActivities: List<RecentActivity> = emptyList(),
    val availableBranches: List<String> = listOf("TODAS", "Sucursal Central", "Sucursal Norte", "Sucursal Sur"),
    val availableRoutes: List<String> = listOf("TODAS", "Ruta 01 - Centro", "Ruta 02 - Mercado", "Ruta 03 - Industrial", "Ruta 04 - PeriSur"),
    val availableCollectors: List<String> = listOf("TODOS"),
    val availableSupervisors: List<String> = listOf("TODOS")
)
