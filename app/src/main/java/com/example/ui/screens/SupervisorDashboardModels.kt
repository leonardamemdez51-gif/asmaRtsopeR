package com.example.ui.screens

import com.example.data.local.ClientEntity
import com.example.data.local.LoanEntity
import com.example.ui.DailyTaskItem

data class SupervisorFilterState(
    val dateRange: String = "HOY", // "HOY", "SEMANA", "MES", "ANIO", "TODO"
    val route: String = "TODAS",
    val collectorName: String = "TODOS",
    val zone: String = "TODAS"
)

data class SupervisorKpiMetrics(
    val expectedCollection: Double = 0.0,
    val realizedCollection: Double = 0.0,
    val cashCollection: Double = 0.0,
    val transferCollection: Double = 0.0,
    val pendingCollection: Double = 0.0,
    val efficiencyPercentage: Double = 0.0,
    val totalAssignedPortfolio: Double = 0.0,
    val overdueClientsCount: Int = 0,
    val overdueLoansCount: Int = 0,
    val totalClientsCount: Int = 0,
    val renewalsCount: Int = 0,
    val overdueAmount: Double = 0.0,
    val moraAmount: Double = 0.0,
    val totalRouteClients: Int = 0,
    val visitedClientsCount: Int = 0,
    val pendingRouteClientsCount: Int = 0,
    val averageScore: Double = 0.0,
    val totalActiveLoans: Int = 0,
    val totalCapitalPlaced: Double = 0.0
)

data class CollectorProductivity(
    val collectorId: Long,
    val collectorName: String,
    val route: String,
    val zone: String,
    val expectedAmount: Double,
    val realizedAmount: Double,
    val pendingAmount: Double,
    val efficiencyPercentage: Double,
    val visitedCount: Int,
    val pendingVisitsCount: Int,
    val activeLoansCount: Int,
    val overdueLoansCount: Int,
    val rankingPosition: Int = 0
)

data class SupervisorAlert(
    val id: String,
    val type: String, // "MORA_ALTA", "META_BAJA", "SIN_VISITAR", "CAJA_ABIERTA"
    val title: String,
    val description: String,
    val collectorName: String,
    val severity: String // "CRITICAL", "WARNING", "INFO"
)

data class SupervisorDashboardState(
    val supervisorName: String = "Supervisor General",
    val filter: SupervisorFilterState = SupervisorFilterState(),
    val kpis: SupervisorKpiMetrics = SupervisorKpiMetrics(),
    val collectorsProductivity: List<CollectorProductivity> = emptyList(),
    val ranking: List<CollectorProductivity> = emptyList(),
    val alerts: List<SupervisorAlert> = emptyList(),
    val chartCollectionByCollector: List<ChartDataPoint> = emptyList(),
    val chartEfficiencyByRoute: List<ChartDataPoint> = emptyList(),
    val chartDailyProgress: List<ChartDataPoint> = emptyList(),
    val chartRiskDistribution: List<ChartDataPoint> = emptyList(),
    val availableRoutes: List<String> = listOf("TODAS"),
    val availableCollectors: List<String> = listOf("TODOS"),
    val availableZones: List<String> = listOf("TODAS")
)
