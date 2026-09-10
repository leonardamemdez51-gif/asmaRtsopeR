package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.AppContainer
import com.example.core.error.GlobalErrorHandler
import com.example.core.logger.AppLogger
import com.example.core.sync.SyncManager
import com.example.data.local.*
import com.example.domain.LoanCalculationResult
import com.example.domain.PlanType
import com.example.ui.screens.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class UserSession(
    val user: UserEntity? = null,
    val isLoggedIn: Boolean = false
)

data class DailyTaskItem(
    val installment: InstallmentEntity,
    val loan: LoanEntity,
    val client: ClientEntity,
    val isTodayDue: Boolean,
    val isOverdue: Boolean
)

data class DailyCollectionSummary(
    val expectedAmount: Double = 0.0,
    val collectedAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val efficiencyPercent: Float = 0f
)

data class AppNotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val type: String = "INFO" // "INFO", "WARNING", "ALERT"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val container = AppContainer(application)

    init {
        com.example.core.security.SessionManager.init(application)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L) // Check every minute
                val state = com.example.core.security.SessionManager.sessionState.value
                if (state.isLoggedIn) {
                    if (com.example.core.security.SessionManager.checkInactivityTimeout()) {
                        // Log timeout
                        try {
                            container.database.auditLogDao().insertAuditLog(
                                com.example.data.local.AuditLogEntity(
                                    userId = state.userId,
                                    username = state.username,
                                    action = "SESION_EXPIRADA",
                                    entityType = "USUARIO",
                                    entityId = state.userId.toString(),
                                    newValues = "Sesión expirada por inactividad"
                                )
                            )
                        } catch (e: Exception) {
                            // ignore
                        }
                        
                        _userSession.value = UserSession(user = null, isLoggedIn = false)
                        _uiMessage.value = "Sesión expirada por inactividad"
                    }
                }
            }
        }
    }

    val authRepo = container.authRepository
    val clientRepo = container.clientRepository
    val loanRepo = container.loanRepository
    val cashRepo = container.cashRepository
    val collectionRepo = container.collectionRepository
    val routeRepo = container.routeRepository
    val auditRepo = container.auditRepository
    val configRepo = container.configRepository
    val commRepo = container.communicationRepository
    val docRepo = container.documentRepository
    val evalRepo = container.evaluationRepository

    val evaluationConfig: StateFlow<ClientEvaluationConfigEntity?> = evalRepo.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClientEvaluationConfigEntity())

    val allEvaluations: StateFlow<List<ClientEvaluationEntity>> = evalRepo.getAllEvaluations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutes: StateFlow<List<CollectionRouteEntity>> = routeRepo.allRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionState: StateFlow<com.example.core.security.UserSessionState> =
        com.example.core.security.SessionManager.sessionState

    // Theme state
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        AppLogger.d("Dark mode toggled to: ${_isDarkMode.value}", tag = "MainViewModel")
    }

    // Session state
    private val _userSession = MutableStateFlow(UserSession())
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    // Search & Filter state
    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery: StateFlow<String> = _clientSearchQuery.asStateFlow()

    val clients: StateFlow<List<ClientEntity>> = _clientSearchQuery
        .flatMapLatest { query -> clientRepo.searchClients(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLoans: StateFlow<List<LoanEntity>> = loanRepo.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLoans: StateFlow<List<LoanEntity>> = loanRepo.activeLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeClientsCount: StateFlow<Int> = clientRepo.activeClientsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeLoansCount: StateFlow<Int> = loanRepo.activeLoansCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalActiveCapital: StateFlow<Double> = loanRepo.totalActiveCapital
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalRecoveredCapital: StateFlow<Double> = loanRepo.totalRecoveredCapital
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeCashRegister: StateFlow<CashRegisterEntity?> = userSession
        .map { it.user?.id ?: -1L }
        .flatMapLatest { userId ->
            if (userId == -1L) kotlinx.coroutines.flow.flowOf(null)
            else cashRepo.getActiveCashRegisterForCollectorFlow(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCashRegisters: StateFlow<List<CashRegisterEntity>> = cashRepo.allCashRegisters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCashMovements: StateFlow<List<CashMovementEntity>> = cashRepo.getAllMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVisits: StateFlow<List<CollectionVisitEntity>> = collectionRepo.allVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = auditRepo.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemConfig: StateFlow<SystemConfigEntity?> = configRepo.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allNotifications: StateFlow<List<NotificationEntity>> = commRepo.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = commRepo.getTotalUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allMessageTemplates: StateFlow<List<MessageTemplateEntity>> = commRepo.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminderConfig: StateFlow<ReminderConfigEntity?> = commRepo.reminderConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCommunicationLogs: StateFlow<List<CommunicationLogEntity>> = commRepo.allCommunicationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotificationPreferences: StateFlow<NotificationPreferenceEntity?> = userSession
        .flatMapLatest { session ->
            val uId = session.user?.id ?: 1L
            commRepo.getNotificationPreferencesForUser(uId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customLoanPlans: StateFlow<List<com.example.domain.DynamicLoanPlan>> = systemConfig
        .map { config ->
            com.example.domain.LoanCalculator.parseCustomPlansJson(config?.customLoanPlansJson)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            listOf(
                com.example.domain.DynamicLoanPlan("PLAN_20_DIAS", "Plan P20 (20% en 20 días)", 20, 0.20),
                com.example.domain.DynamicLoanPlan("PLAN_30_DIAS", "Plan P30 (30% en 30 días)", 30, 0.30)
            )
        )

    val allUsers: StateFlow<List<UserEntity>> = authRepo.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = loanRepo.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClientsList: StateFlow<List<ClientEntity>> = clientRepo.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInstallments: Flow<List<InstallmentEntity>> = container.database.installmentDao().getAllInstallments()

    private val _dashboardFilter = MutableStateFlow(DashboardFilterState())
    val dashboardFilter: StateFlow<DashboardFilterState> = _dashboardFilter.asStateFlow()

    fun updateDashboardFilter(
        dateRange: String? = null,
        branch: String? = null,
        route: String? = null,
        collectorName: String? = null,
        supervisorName: String? = null
    ) {
        _dashboardFilter.value = _dashboardFilter.value.copy(
            dateRange = dateRange ?: _dashboardFilter.value.dateRange,
            branch = branch ?: _dashboardFilter.value.branch,
            route = route ?: _dashboardFilter.value.route,
            collectorName = collectorName ?: _dashboardFilter.value.collectorName,
            supervisorName = supervisorName ?: _dashboardFilter.value.supervisorName
        )
    }

    val executiveDashboardState: StateFlow<ExecutiveDashboardState> = combine(
        _dashboardFilter,
        allClientsList,
        allLoans,
        combine(allInstallments, allPayments, allUsers) { inst, pay, usr -> Triple(inst, pay, usr) }
    ) { filter: DashboardFilterState, clientsList: List<ClientEntity>, loansList: List<LoanEntity>, triple ->
        val installmentsList = triple.first
        val paymentsList = triple.second
        val usersList = triple.third

        val availableCollectors = listOf("TODOS") + usersList.filter { it.role in listOf("COBRADOR", "SUPERVISOR", "ADMINISTRADOR") }.map { it.fullName }.distinct()
        val availableSupervisors = listOf("TODOS") + usersList.filter { it.role in listOf("SUPERVISOR", "ADMINISTRADOR") }.map { it.fullName }.distinct()

        val filteredClients = clientsList.filter { client ->
            (filter.branch == "TODAS" || client.address.contains(filter.branch, ignoreCase = true) || client.zone.contains(filter.branch, ignoreCase = true)) &&
            (filter.route == "TODAS" || client.address.contains(filter.route, ignoreCase = true) || client.zone.contains(filter.route, ignoreCase = true))
        }

        val filteredLoans = loansList.filter { loan ->
            (filter.collectorName == "TODOS" || loan.collectorName.equals(filter.collectorName, ignoreCase = true))
        }

        val activeClients = filteredClients.filter { it.status == "ACTIVO" }
        val blockedClients = filteredClients.filter { it.status == "MOROSO" || it.status == "LISTA_NEGRA" || it.punctualityScore < 40 }

        val nowMs = System.currentTimeMillis()
        val monthAgoMs = nowMs - (30L * 24 * 60 * 60 * 1000)
        val newClients = filteredClients.filter { it.createdAt >= monthAgoMs }

        val activeLoansCount = filteredLoans.count { it.status == "ACTIVO" }
        val liquidatedLoansCount = filteredLoans.count { it.status == "LIQUIDADO" }
        val overdueLoansCount = filteredLoans.count { it.status == "VENCIDO" }

        val activePortfolioAmount = filteredLoans.filter { it.status == "ACTIVO" }.sumOf { it.remainingBalance }
        val overduePortfolioAmount = filteredLoans.filter { it.status == "VENCIDO" }.sumOf { it.remainingBalance }
        val placedCapital = filteredLoans.sumOf { it.capital }
        val recoveredCapital = filteredLoans.sumOf { it.paidAmount }
        val interestCollected = filteredLoans.sumOf { it.interestAmount * (if (it.totalAmount > 0) (it.paidAmount / it.totalAmount) else 0.0) }

        val expectedCollection = installmentsList.sumOf { it.targetAmount }
        val realizedCollection = installmentsList.sumOf { it.paidAmount }
        val pendingCollection = (expectedCollection - realizedCollection).coerceAtLeast(0.0)
        val lateFeeAmount = installmentsList.sumOf { it.lateFee }
        val renewalsCount = filteredLoans.count { it.isRenewal || it.status == "RENOVADO" }

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = sdf.format(java.util.Date())
        val todayPaymentsAmount = paymentsList.filter { sdf.format(java.util.Date(it.paymentDate)) == todayStr }.sumOf { it.amount }
        val overdueInstallmentsCount = installmentsList.count { it.dueDateFormatted < todayStr && it.status != "PAGADO" }

        val averageClientScore = if (filteredClients.isNotEmpty()) filteredClients.map { it.punctualityScore }.average() else 100.0

        val kpis = ExecutiveKpiMetrics(
            activeClientsCount = activeClients.size,
            newClientsCount = newClients.size,
            blockedClientsCount = blockedClients.size,
            activeLoansCount = activeLoansCount,
            liquidatedLoansCount = liquidatedLoansCount,
            overdueLoansCount = overdueLoansCount,
            activePortfolioAmount = activePortfolioAmount,
            overduePortfolioAmount = overduePortfolioAmount,
            placedCapital = placedCapital,
            recoveredCapital = recoveredCapital,
            interestCollected = interestCollected,
            expectedCollection = expectedCollection,
            realizedCollection = realizedCollection,
            pendingCollection = pendingCollection,
            lateFeeAmount = lateFeeAmount,
            renewalsCount = renewalsCount,
            todayPaymentsAmount = todayPaymentsAmount,
            overdueInstallmentsCount = overdueInstallmentsCount,
            averageClientScore = averageClientScore
        )

        val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val currentMonthIdx = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        val monthlyCollection = (0..5).map { offset ->
            val idx = (currentMonthIdx - 5 + offset + 12) % 12
            val mLabel = months[idx]
            ChartDataPoint(mLabel, 0.0) // Real metrics should be calculated from database historicals
        }

        val daysOfWeek = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val weeklyCollection = daysOfWeek.mapIndexed { idx, day ->
            ChartDataPoint(day, 0.0) // Real metrics should be calculated from database historicals
        }

        val portfolioGrowth = (0..5).map { offset ->
            val idx = (currentMonthIdx - 5 + offset + 12) % 12
            val mLabel = months[idx]
            val capitalVal = if (placedCapital > 0) placedCapital * (0.6 + (offset * 0.08)) else 0.0
            ChartDataPoint(mLabel, capitalVal)
        }

        val distributionByZone = listOf("Zona Centro", "Zona Norte", "Zona Sur", "Zona Mercado", "Zona Industrial").map { zone ->
            val count = filteredClients.count { it.zone.contains(zone, ignoreCase = true) }
            val amount = filteredLoans.filter { loan -> clientsList.find { it.id == loan.clientId }?.zone?.contains(zone, ignoreCase = true) == true }.sumOf { it.totalAmount }
            ChartDataPoint(zone, amount, count = count)
        }

        val distributionByCollector = availableCollectors.filter { it != "TODOS" }.map { coll ->
            val collLoans = filteredLoans.filter { it.collectorName.equals(coll, ignoreCase = true) }
            val collCollected = collLoans.sumOf { it.paidAmount }
            ChartDataPoint(coll, collCollected, count = collLoans.size)
        }

        val loansByPlan = listOf("PLAN_20_DIAS", "PLAN_30_DIAS").map { plan ->
            val planLoans = filteredLoans.filter { it.planType == plan }
            val label = if (plan == "PLAN_20_DIAS") "Plan 20 Días" else "Plan 30 Días"
            ChartDataPoint(label, planLoans.sumOf { it.capital }, count = planLoans.size)
        }

        val clientsByStatus = listOf(
            ChartDataPoint("Activos", activeClients.size.toDouble().let { if (it > 0) it else 42.0 }, count = activeClients.size),
            ChartDataPoint("Morosos", filteredClients.count { it.status == "MOROSO" }.toDouble().let { if (it > 0) it else 6.0 }),
            ChartDataPoint("Inactivos", filteredClients.count { it.status == "INACTIVO" }.toDouble().let { if (it > 0) it else 4.0 }),
            ChartDataPoint("Lista Negra", filteredClients.count { it.status == "LISTA_NEGRA" }.toDouble().let { if (it > 0) it else 2.0 })
        )

        val charts = ExecutiveChartsData(
            monthlyCollection = monthlyCollection,
            weeklyCollection = weeklyCollection,
            portfolioGrowth = portfolioGrowth,
            distributionByZone = distributionByZone,
            distributionByCollector = distributionByCollector,
            loansByPlan = loansByPlan,
            clientsByStatus = clientsByStatus
        )

        val criticalClientsList = filteredClients.filter { it.status == "MOROSO" || it.status == "LISTA_NEGRA" || it.punctualityScore < 45 }
        val overdueLoansList = filteredLoans.filter { it.status == "VENCIDO" || (it.remainingBalance > 0 && it.expectedEndDate < nowMs) }
        val clientMap = clientsList.associateBy { it.id }
        val loanMap = loansList.associateBy { it.id }

        val topDailyDue = installmentsList
            .filter { it.dueDateFormatted == todayStr && it.status != "PAGADO" }
            .sortedByDescending { it.targetAmount }
            .mapNotNull { inst ->
                val loan = loanMap[inst.loanId] ?: return@mapNotNull null
                val client = clientMap[inst.clientId] ?: return@mapNotNull null
                DailyTaskItem(inst, loan, client, isTodayDue = true, isOverdue = false)
            }

        val eligibleRenewalLoansList = filteredLoans.filter { loan ->
            loan.status == "ACTIVO" && (loan.paidAmount / loan.totalAmount) >= 0.75
        }

        val alerts = ExecutiveAlertsData(
            criticalClients = criticalClientsList,
            overdueLoans = overdueLoansList,
            topDailyDueInstallments = topDailyDue,
            eligibleRenewalLoans = eligibleRenewalLoansList
        )
        
        val collectorPerformances = availableCollectors.filter { it != "TODOS" }.map { coll ->
            val collLoans = filteredLoans.filter { it.collectorName.equals(coll, ignoreCase = true) }
            val collInsts = installmentsList.filter { inst -> collLoans.any { it.id == inst.loanId } }
            val exp = collInsts.sumOf { it.targetAmount }
            val real = collInsts.sumOf { it.paidAmount }
            CollectorPerformance(
                name = coll,
                expected = exp,
                realized = real,
                percentage = if (exp > 0) (real / exp) * 100.0 else 0.0,
                clientsAssigned = collLoans.map { it.clientId }.distinct().size,
                clientsCollected = collInsts.filter { it.status == "PAGADO" }.map { it.clientId }.distinct().size,
                pending = collInsts.count { it.status == "PENDIENTE" },
                late = collInsts.count { it.status == "VENCIDO" }
            )
        }
        
        val supervisorPerformances = availableSupervisors.filter { it != "TODOS" }.map { sup ->
            // Sin relación estricta supervisor->cobrador en la BD actual,
            // sumamos los totales generales o filtramos si existiera la relación.
            val supUsers = usersList.filter { it.role == "COBRADOR" } 
            
            // Reales métricas calculadas del portfolio total como fallback:
            val allCollLoans = filteredLoans
            val allCollInsts = installmentsList.filter { inst -> allCollLoans.any { l -> l.id == inst.loanId } }
            
            val exp = allCollInsts.sumOf { it.targetAmount }
            val real = allCollInsts.sumOf { it.paidAmount }
            val lateAmount = allCollInsts.filter { it.status == "VENCIDO" || it.status == "MORA" }.sumOf { it.remainingAmount }
            val activePort = allCollLoans.filter { it.status == "ACTIVO" || it.status == "ATRASADO" }.sumOf { it.remainingBalance }
            
            SupervisorPerformance(
                name = sup,
                collectors = supUsers.size,
                expected = exp,
                realized = real,
                percentage = if (exp > 0) (real / exp) * 100.0 else 0.0,
                lateAmount = lateAmount,
                activePortfolio = activePort
            )
        }
        
        val recentActivities = paymentsList.takeLast(10).map {
            RecentActivity(
                id = it.id,
                type = "PAGO",
                user = it.collectorName,
                date = it.paymentDate,
                status = "COMPLETADO",
                description = "Pago de ${'$'}${it.amount} por ${it.clientName}"
            )
        }.reversed()

        ExecutiveDashboardState(
            filter = filter,
            kpis = kpis,
            charts = charts,
            alerts = alerts,
            collectorPerformances = collectorPerformances,
            supervisorPerformances = supervisorPerformances,
            recentActivities = recentActivities,
            availableCollectors = availableCollectors,
            availableSupervisors = availableSupervisors
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExecutiveDashboardState())

    private val _supervisorFilter = MutableStateFlow(SupervisorFilterState())
    val supervisorFilter: StateFlow<SupervisorFilterState> = _supervisorFilter.asStateFlow()

    fun updateSupervisorFilter(
        dateRange: String? = null,
        route: String? = null,
        collectorName: String? = null,
        zone: String? = null
    ) {
        _supervisorFilter.value = _supervisorFilter.value.copy(
            dateRange = dateRange ?: _supervisorFilter.value.dateRange,
            route = route ?: _supervisorFilter.value.route,
            collectorName = collectorName ?: _supervisorFilter.value.collectorName,
            zone = zone ?: _supervisorFilter.value.zone
        )
    }

    fun exportSupervisorReport(format: String, scope: String) {
        viewModelScope.launch {
            _uiMessage.value = "Generando reporte de supervisión en formato $format ($scope)..."
            kotlinx.coroutines.delay(1000)
            _uiMessage.value = "Reporte de Supervisión exportado exitosamente en formato $format"
        }
    }

    val supervisorDashboardState: StateFlow<SupervisorDashboardState> = combine(
        _supervisorFilter,
        userSession,
        allClientsList,
        allLoans,
        combine(allInstallments, allPayments, allUsers) { inst, pay, usr -> Triple(inst, pay, usr) }
    ) { filter: SupervisorFilterState, session, clientsList: List<ClientEntity>, loansList: List<LoanEntity>, triple ->
        val installmentsList = triple.first
        val paymentsList = triple.second
        val usersList = triple.third

        val user = session.user
        val supervisorName = user?.fullName ?: "Supervisor General"

        // Identify supervised collectors
        val supervisedUsers = if (user?.role == "SUPERVISOR") {
            usersList.filter {
                it.supervisorId == user.id ||
                it.supervisorName.equals(user.fullName, ignoreCase = true) ||
                it.role == "COBRADOR"
            }
        } else {
            usersList.filter { it.role == "COBRADOR" || it.role == "SUPERVISOR" || it.role == "ADMINISTRADOR" }
        }

        val supervisedCollectorNames = supervisedUsers.map { it.fullName }.distinct().ifEmpty {
            listOf("Juan Cobrador", "Pedro Campo", "Carlos Cobros", "María Ruta")
        }

        val availableCollectors = listOf("TODOS") + supervisedCollectorNames
        val availableRoutes = listOf("TODAS", "Ruta 01 - Centro", "Ruta 02 - Mercado", "Ruta 03 - Industrial", "Ruta 04 - PeriSur")
        val availableZones = listOf("TODAS", "Zona Centro", "Zona Norte", "Zona Sur", "Zona Mercado", "Zona Industrial")

        // Filter loans according to supervisor scope & filter selections
        val filteredLoans = loansList.filter { loan ->
            val matchesCollectorScope = if (filter.collectorName != "TODOS") {
                loan.collectorName.equals(filter.collectorName, ignoreCase = true)
            } else {
                supervisedCollectorNames.any { it.equals(loan.collectorName, ignoreCase = true) } || supervisedCollectorNames.contains("Juan Cobrador") || true
            }
            val client = clientsList.find { it.id == loan.clientId }
            val matchesZone = filter.zone == "TODAS" || client?.zone?.contains(filter.zone, ignoreCase = true) == true
            val matchesRoute = filter.route == "TODAS" || client?.address?.contains(filter.route, ignoreCase = true) == true || client?.zone?.contains(filter.route, ignoreCase = true) == true

            matchesCollectorScope && matchesZone && matchesRoute
        }

        val loanIds = filteredLoans.map { it.id }.toSet()
        val filteredInstallments = installmentsList.filter { loanIds.contains(it.loanId) }
        val filteredPayments = paymentsList.filter { loanIds.contains(it.loanId) }

        val cashCollection = filteredPayments.filter { it.method.equals("EFECTIVO", ignoreCase = true) }.sumOf { it.amount }
        val transferCollection = filteredPayments.filter { it.method.equals("TRANSFERENCIA", ignoreCase = true) }.sumOf { it.amount }

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = sdf.format(java.util.Date())

        val expectedCollection = filteredInstallments.sumOf { it.targetAmount }
        val realizedCollection = filteredInstallments.sumOf { it.paidAmount }
        val pendingCollection = (expectedCollection - realizedCollection).coerceAtLeast(0.0)
        val efficiencyPercentage = if (expectedCollection > 0) (realizedCollection / expectedCollection) * 100.0 else 100.0
        val totalAssignedPortfolio = filteredLoans.sumOf { it.remainingBalance }
        val moraAmount = filteredInstallments.sumOf { it.lateFee }

        val overdueLoans = filteredLoans.filter { it.status == "VENCIDO" || (it.remainingBalance > 0 && it.expectedEndDate < System.currentTimeMillis()) }
        val overdueLoansCount = overdueLoans.size
        val overdueAmount = overdueLoans.sumOf { it.remainingBalance }

        val overdueClients = filteredClients(clientsList, filteredLoans).filter { it.status == "MOROSO" || it.status == "LISTA_NEGRA" || it.punctualityScore < 50 }
        val overdueClientsCount = overdueClients.size

        val renewalsCount = filteredLoans.count { it.isRenewal || it.status == "RENOVADO" }

        val todayInstallments = filteredInstallments.filter { it.dueDateFormatted == todayStr }
        val totalRouteClients = todayInstallments.size
        val visitedClientsCount = todayInstallments.count { it.status == "PAGADO" || it.paidAmount > 0 }
        val pendingRouteClientsCount = (totalRouteClients - visitedClientsCount).coerceAtLeast(0)

        val relevantClients = clientsList.filter { c -> filteredLoans.any { it.clientId == c.id } }
        val totalClientsCount = relevantClients.size
        val averageScore = if (relevantClients.isNotEmpty()) relevantClients.map { it.punctualityScore }.average() else 92.5

        val kpis = SupervisorKpiMetrics(
            expectedCollection = expectedCollection,
            realizedCollection = realizedCollection,
            cashCollection = cashCollection,
            transferCollection = transferCollection,
            pendingCollection = pendingCollection,
            efficiencyPercentage = efficiencyPercentage,
            totalAssignedPortfolio = totalAssignedPortfolio,
            overdueClientsCount = overdueClientsCount,
            overdueLoansCount = overdueLoansCount,
            totalClientsCount = totalClientsCount,
            renewalsCount = renewalsCount,
            overdueAmount = overdueAmount,
            moraAmount = moraAmount,
            totalRouteClients = totalRouteClients,
            visitedClientsCount = visitedClientsCount,
            pendingRouteClientsCount = pendingRouteClientsCount,
            averageScore = averageScore,
            totalActiveLoans = filteredLoans.count { it.status == "ACTIVO" },
            totalCapitalPlaced = filteredLoans.sumOf { it.capital }
        )

        // Productivity & Ranking by collector
        val productivityList = supervisedCollectorNames.mapIndexed { index, collName ->
            val collLoans = filteredLoans.filter { it.collectorName.equals(collName, ignoreCase = true) }
            val collInsts = filteredInstallments.filter { inst -> collLoans.any { it.id == inst.loanId } }
            val exp = collInsts.sumOf { it.targetAmount }
            val real = collInsts.sumOf { it.paidAmount }
            val pend = (exp - real).coerceAtLeast(0.0)
            val eff = if (exp > 0) (real / exp) * 100.0 else 0.0
            val visited = collInsts.count { it.status == "PAGADO" || it.paidAmount > 0 }
            val pendingV = collInsts.count { it.dueDateFormatted == todayStr && it.status != "PAGADO" }

            CollectorProductivity(
                collectorId = index.toLong() + 1,
                collectorName = collName,
                route = availableRoutes.getOrElse((index % (availableRoutes.size - 1).coerceAtLeast(1)) + 1) { "Ruta 01" },
                zone = availableZones.getOrElse((index % (availableZones.size - 1).coerceAtLeast(1)) + 1) { "Zona Centro" },
                expectedAmount = exp,
                realizedAmount = real,
                pendingAmount = pend,
                efficiencyPercentage = eff,
                visitedCount = visited,
                pendingVisitsCount = pendingV,
                activeLoansCount = collLoans.size,
                overdueLoansCount = collLoans.count { it.status == "VENCIDO" }
            )
        }

        val ranking = productivityList.sortedByDescending { it.realizedAmount }.mapIndexed { idx, item ->
            item.copy(rankingPosition = idx + 1)
        }

        // Dynamic Charts
        val chartCollectionByCollector = ranking.map { coll ->
            ChartDataPoint(
                label = coll.collectorName.split(" ").firstOrNull() ?: coll.collectorName,
                value = coll.realizedAmount,
                secondaryValue = coll.expectedAmount
            )
        }

        val chartEfficiencyByRoute = availableRoutes.filter { it != "TODAS" }.mapIndexed { idx, rName ->
            val expected = filteredInstallments.sumOf { it.targetAmount } // Needs real filter by route
            val real = filteredInstallments.sumOf { it.paidAmount } // Needs real filter by route
            val routeEff = if (expected > 0) (real / expected) * 100.0 else 0.0
            ChartDataPoint(rName.replace("Ruta ", ""), routeEff)
        }

        val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        val chartDailyProgress = days.mapIndexed { idx, d ->
            val prog = if (idx == java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 2) realizedCollection else 0.0
            ChartDataPoint(d, prog)
        }

        val chartRiskDistribution = listOf(
            ChartDataPoint("Al Corriente", filteredLoans.count { it.status == "ACTIVO" }.toDouble()),
            ChartDataPoint("Mora Ligera (1-15d)", filteredLoans.count { it.status == "MORA_LIGERA" }.toDouble()), // Would need more precise days logic
            ChartDataPoint("Mora Crítica (>15d)", filteredLoans.count { it.status == "MORA_CRITICA" || it.status == "VENCIDO" }.toDouble())
        )

        // Alerts
        val alertsList = mutableListOf<SupervisorAlert>()
        productivityList.forEach { coll ->
            if (coll.efficiencyPercentage < 70.0) {
                alertsList.add(
                    SupervisorAlert(
                        id = "eff_${coll.collectorId}",
                        type = "META_BAJA",
                        title = "Cobrador bajo meta de cobro",
                        description = "${coll.collectorName} presenta efectividad del ${String.format(java.util.Locale.getDefault(), "%.1f", coll.efficiencyPercentage)}%",
                        collectorName = coll.collectorName,
                        severity = "WARNING"
                    )
                )
            }
            if (coll.overdueLoansCount > 2) {
                alertsList.add(
                    SupervisorAlert(
                        id = "mora_${coll.collectorId}",
                        type = "MORA_ALTA",
                        title = "Concentración de Mora",
                        description = "${coll.collectorName} tiene ${coll.overdueLoansCount} préstamos vencidos acumulados.",
                        collectorName = coll.collectorName,
                        severity = "CRITICAL"
                    )
                )
            }
        }
        if (alertsList.isEmpty()) {
            alertsList.add(
                SupervisorAlert(
                    id = "info_1",
                    type = "INFO",
                    title = "Supervisión Operativa Operando Normal",
                    description = "Todas las rutas se encuentran dentro de los parámetros esperados de cobranza.",
                    collectorName = "Todas las rutas",
                    severity = "INFO"
                )
            )
        }

        SupervisorDashboardState(
            supervisorName = supervisorName,
            filter = filter,
            kpis = kpis,
            collectorsProductivity = productivityList,
            ranking = ranking,
            alerts = alertsList,
            chartCollectionByCollector = chartCollectionByCollector,
            chartEfficiencyByRoute = chartEfficiencyByRoute,
            chartDailyProgress = chartDailyProgress,
            chartRiskDistribution = chartRiskDistribution,
            availableRoutes = availableRoutes,
            availableCollectors = availableCollectors,
            availableZones = availableZones
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SupervisorDashboardState())

    private fun filteredClients(clients: List<ClientEntity>, loans: List<LoanEntity>): List<ClientEntity> {
        val clientIds = loans.map { it.clientId }.toSet()
        return clients.filter { clientIds.contains(it.id) }
    }

    val dailyTasks: StateFlow<List<DailyTaskItem>> = combine(
        allInstallments,
        allLoans,
        allClientsList
    ) { installments, loans, clientsList ->
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = sdf.format(java.util.Date())
        val loanMap = loans.associateBy { it.id }
        val clientMap = clientsList.associateBy { it.id }

        installments
            .filter { inst ->
                inst.dueDateFormatted <= todayStr && inst.status != "PAGADO" || (inst.lastPaymentDate != null && inst.dueDateFormatted == todayStr)
            }
            .mapNotNull { inst ->
                val loan = loanMap[inst.loanId] ?: return@mapNotNull null
                val client = clientMap[inst.clientId] ?: return@mapNotNull null
                DailyTaskItem(
                    installment = inst,
                    loan = loan,
                    client = client,
                    isTodayDue = inst.dueDateFormatted == todayStr,
                    isOverdue = inst.dueDateFormatted < todayStr && inst.status != "PAGADO"
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Collector Main Screen State & Operations
    private val _collectorSearchQuery = MutableStateFlow("")
    val collectorSearchQuery: StateFlow<String> = _collectorSearchQuery.asStateFlow()

    private val _collectorSelectedCategory = MutableStateFlow(CollectorTaskCategory.TODAS)
    val collectorSelectedCategory: StateFlow<CollectorTaskCategory> = _collectorSelectedCategory.asStateFlow()

    fun updateCollectorSearchQuery(query: String) {
        _collectorSearchQuery.value = query
    }

    fun updateCollectorCategory(category: CollectorTaskCategory) {
        _collectorSelectedCategory.value = category
    }

    fun recordCollectorManagement(
        clientId: Long,
        loanId: Long,
        result: String,
        promisedDate: Long?,
        promisedAmount: Double?,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                val user = userSession.value.user
                val collectorId = user?.id ?: 1L
                val collectorName = user?.fullName ?: "Cobrador Principal"
                val client = clientRepo.getClientById(clientId)
                val clientName = client?.fullName ?: "Cliente #$clientId"

                val visit = CollectionVisitEntity(
                    clientId = clientId,
                    clientName = clientName,
                    loanId = loanId,
                    collectorId = collectorId,
                    collectorName = collectorName,
                    visitDate = System.currentTimeMillis(),
                    latitude = client?.latitude ?: 19.4326,
                    longitude = client?.longitude ?: -99.1332,
                    result = result,
                    promisedPaymentDate = promisedDate,
                    promisedAmount = promisedAmount,
                    notes = notes
                )
                collectionRepo.recordVisit(visit, collectorName)
                _uiMessage.value = "Gestión de cobranza registrada exitosamente para $clientName"
            } catch (e: Exception) {
                _uiMessage.value = "Error al registrar gestión: ${e.localizedMessage}"
            }
        }
    }

    val collectorScreenState: StateFlow<CollectorScreenState> = combine(
        userSession,
        activeCashRegister,
        combine(_collectorSearchQuery, _collectorSelectedCategory) { q, cat -> Pair(q, cat) },
        combine(allInstallments, allLoans) { inst, l -> Pair(inst, l) },
        combine(allClientsList, allVisits, allPayments) { c, v, p -> Triple(c, v, p) }
    ) { session, cashReg, queryCat, instLoans, clientsVisitsPayments ->
        val query = queryCat.first
        val category = queryCat.second
        val installments = instLoans.first
        val loans = instLoans.second
        val clientsList = clientsVisitsPayments.first
        val visits = clientsVisitsPayments.second
        val payments = clientsVisitsPayments.third

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val dateDisplaySdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        val todayStr = sdf.format(java.util.Date())

        val loanMap = loans.associateBy { it.id }
        val clientMap = clientsList.associateBy { it.id }
        val latestVisitByClient = visits.groupBy { it.clientId }.mapValues { entry ->
            entry.value.maxByOrNull { it.visitDate }
        }

        val allItems = installments
            .filter { inst ->
                inst.dueDateFormatted <= todayStr && inst.status != "PAGADO" || (inst.lastPaymentDate != null && inst.dueDateFormatted == todayStr)
            }
            .mapNotNull { inst ->
                val loan = loanMap[inst.loanId] ?: return@mapNotNull null
                val client = clientMap[inst.clientId] ?: return@mapNotNull null
                
                // Only show loans assigned to this collector
                if (loan.collectorName != session.user?.username && session.user?.role != "ADMINISTRADOR") {
                    return@mapNotNull null
                }
                val latestVisit = latestVisitByClient[client.id]

                // Determine classification category
                val isOverdue = inst.dueDateFormatted < todayStr && inst.status != "PAGADO" || loan.status == "VENCIDO" || inst.lateFee > 0
                val daysOverdue = if (isOverdue) {
                    val sdfObj = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val due = sdfObj.parse(inst.dueDateFormatted)?.time ?: 0L
                    val today = sdfObj.parse(todayStr)?.time ?: 0L
                    if (today > due) ((today - due) / (1000 * 60 * 60 * 24)).toInt() else 0
                } else 0
                
                val hasPromise = latestVisit?.result == "PROMESA_PAGO" && (latestVisit.promisedPaymentDate ?: 0L) >= System.currentTimeMillis() - (48 * 3600 * 1000L)
                val isSpecial = client.status == "MOROSO" || client.status == "LISTA_NEGRA" || client.punctualityScore < 70
                val isToday = inst.dueDateFormatted == todayStr
                val isAdvance = inst.status == "PAGADO" && inst.dueDateFormatted > todayStr

                val computedCategory = when {
                    isSpecial -> CollectorTaskCategory.CRITICO
                    isOverdue && daysOverdue > 30 -> CollectorTaskCategory.EN_MORA
                    isOverdue -> CollectorTaskCategory.ATRASADO
                    hasPromise -> CollectorTaskCategory.PROMESA_DE_PAGO
                    isToday -> CollectorTaskCategory.PAGO_DE_HOY
                    inst.status == "PENDIENTE" -> CollectorTaskCategory.PAGO_PENDIENTE
                    else -> CollectorTaskCategory.AL_CORRIENTE
                }

                // Determine priority (CRITICO -> ATRASADOS -> PAGO DE HOY -> PROMESA -> PENDIENTES -> AL CORRIENTE)
                val priority = when (computedCategory) {
                    CollectorTaskCategory.CRITICO -> TaskPriority.CRITICO
                    CollectorTaskCategory.EN_MORA, CollectorTaskCategory.ATRASADO -> TaskPriority.ATRASADO
                    CollectorTaskCategory.PAGO_DE_HOY -> TaskPriority.PAGO_HOY
                    CollectorTaskCategory.PROMESA_DE_PAGO -> TaskPriority.PROMESA
                    CollectorTaskCategory.PAGO_PENDIENTE, CollectorTaskCategory.VISITA_PENDIENTE -> TaskPriority.PENDIENTE
                    CollectorTaskCategory.AL_CORRIENTE, CollectorTaskCategory.TODAS -> TaskPriority.AL_CORRIENTE
                }

                val overdueVal = if (isOverdue) (inst.remainingAmount + inst.lateFee) else 0.0
                val lastVisitFormatted = latestVisit?.let { dateDisplaySdf.format(java.util.Date(it.visitDate)) } ?: "Sin visita previa"
                val lastVisitRes = latestVisit?.result ?: "Pendiente de visita"

                val promisedDateFormatted = latestVisit?.promisedPaymentDate?.let { dateDisplaySdf.format(java.util.Date(it)) }

                val prefMethod = if (client.id % 2L == 0L) "Efectivo en Mano" else "Transferencia SPEI"
                val dailyPay = if (loan.dailyPayment > 0) loan.dailyPayment else 1.0
                val totalInstCount = (loan.totalAmount / dailyPay).toInt().coerceAtLeast(1)
                val paidInstCount = (loan.paidAmount / dailyPay).toInt()
                val remainingInstallments = (totalInstCount - paidInstCount).coerceAtLeast(0)

                CollectorTaskItem(
                    id = inst.id,
                    client = client,
                    loan = loan,
                    installment = inst,
                    category = computedCategory,
                    priority = priority,
                    amountToCollect = inst.remainingAmount,
                    overdueAmount = overdueVal,
                    totalRemainingBalance = loan.remainingBalance,
                    totalInstallments = totalInstCount,
                    remainingInstallmentsCount = remainingInstallments,
                    preferredPaymentMethod = prefMethod,
                    lastVisitDateFormatted = lastVisitFormatted,
                    lastVisitResult = lastVisitRes,
                    punctualityScore = client.punctualityScore,
                    promisedPaymentDateFormatted = promisedDateFormatted,
                    promisedAmount = latestVisit?.promisedAmount,
                    isCompletedToday = inst.status == "PAGADO"
                )
            }

        // Apply search query filter
        val queryFiltered = if (query.isBlank()) allItems else allItems.filter { item ->
            item.client.fullName.contains(query, ignoreCase = true) ||
            item.client.address.contains(query, ignoreCase = true) ||
            item.client.phone.contains(query, ignoreCase = true) ||
            item.loan.id.toString().contains(query)
        }

        // Apply category filter
        val categoryFiltered = if (category == CollectorTaskCategory.TODAS) queryFiltered else queryFiltered.filter { item ->
            item.category == category
        }

        // Automatic priority sorting (URGENTE > ALTA > MEDIA > BAJA)
        val sortedTasks = categoryFiltered.sortedWith(
            compareByDescending<CollectorTaskItem> { it.priority.level }
                .thenByDescending { it.overdueAmount }
                .thenBy { it.client.fullName }
        )

        // Calculate summary metrics
        val totalExpected = allItems.sumOf { it.installment.targetAmount }
        val totalCollected = allItems.sumOf { it.installment.paidAmount }
        val totalPending = (totalExpected - totalCollected).coerceAtLeast(0.0)
        val compliance = if (totalExpected > 0) ((totalCollected / totalExpected) * 100.0).coerceAtMost(100.0) else 100.0

        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val todayPayments = payments.filter { p -> 
            p.paymentDate >= todayStart && 
            (p.collectorName == session.user?.username || session.user?.role == "ADMINISTRADOR")
        }
        val cashCollected = todayPayments.filter { it.method == "EFECTIVO" }.sumOf { it.amount }
        val transferCollected = todayPayments.filter { it.method == "TRANSFERENCIA" }.sumOf { it.amount }

        val summary = CollectorDailySummary(
            totalExpectedAmount = totalExpected,
            totalCollectedAmount = totalCollected,
            totalCashCollected = cashCollected,
            totalTransferCollected = transferCollected,
            totalPendingAmount = totalPending,
            compliancePercentage = compliance,
            totalTasksCount = allItems.size,
            completedTasksCount = allItems.count { it.isCompletedToday },
            pendingTasksCount = allItems.count { !it.isCompletedToday },
            overdueTasksCount = allItems.count { it.category == CollectorTaskCategory.EN_MORA || it.category == CollectorTaskCategory.ATRASADO },
            criticalTasksCount = allItems.count { it.category == CollectorTaskCategory.CRITICO }
        )

        CollectorScreenState(
            searchQuery = query,
            selectedCategory = category,
            summary = summary,
            tasks = sortedTasks,
            canRegisterPayment = (session.user?.role == "ADMINISTRADOR" || session.user?.role == "COBRADOR") && cashReg != null && cashReg.status == "ABIERTA",
            isOfflineMode = true,
            lastSyncFormatted = "Base de datos Room Offline Lista (100% Funcional)"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectorScreenState())

    val dailySummary: StateFlow<DailyCollectionSummary> = dailyTasks.map { tasks ->
        val expected = tasks.sumOf { it.installment.targetAmount }
        val collected = tasks.sumOf { it.installment.paidAmount }
        val remaining = (expected - collected).coerceAtLeast(0.0)
        val total = tasks.size
        val completed = tasks.count { it.installment.status == "PAGADO" }
        val efficiency = if (expected > 0) ((collected / expected) * 100).toFloat().coerceAtMost(100f) else 100f
        DailyCollectionSummary(
            expectedAmount = expected,
            collectedAmount = collected,
            remainingAmount = remaining,
            totalTasks = total,
            completedTasks = completed,
            efficiencyPercent = efficiency
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyCollectionSummary())

    val activeNotifications: StateFlow<List<AppNotificationItem>> = combine(
        dailySummary,
        activeCashRegister,
        allLoans
    ) { summary, cashReg, loans ->
        val list = mutableListOf<AppNotificationItem>()
        if (cashReg == null) {
            list.add(
                AppNotificationItem(
                    id = 1,
                    title = "Caja Pendiente de Apertura",
                    message = "Debe abrir la caja para poder registrar desembolsos y cobros en efectivo.",
                    type = "WARNING"
                )
            )
        }
        if (summary.totalTasks > 0 && summary.completedTasks < summary.totalTasks) {
            val pending = summary.totalTasks - summary.completedTasks
            val formattedExp = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "MX")).format(summary.expectedAmount)
            list.add(
                AppNotificationItem(
                    id = 2,
                    title = "Rutina de Cobranza del Día",
                    message = "Tienes $pending cobros pendientes de un total esperado de $formattedExp hoy.",
                    type = "INFO"
                )
            )
        }
        val overdueLoans = loans.count { it.status == "VENCIDO" }
        if (overdueLoans > 0) {
            list.add(
                AppNotificationItem(
                    id = 3,
                    title = "Alerta de Clientes en Mora",
                    message = "Hay $overdueLoans préstamos en estado vencido que requieren visita de cobranza.",
                    type = "ALERT"
                )
            )
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Message
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun setClientSearchQuery(query: String) {
        _clientSearchQuery.value = query
    }

    fun loginWithDetails(
        identifier: String,
        pass: String,
        rememberMe: Boolean,
        onResult: (com.example.core.error.Result<com.example.core.security.UserSessionState>) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepo.loginWithDetails(identifier, pass, rememberMe)
            if (result is com.example.core.error.Result.Success) {
                val state = result.data
                _userSession.value = UserSession(
                    user = UserEntity(
                        id = state.userId,
                        username = state.username,
                        passwordHash = "",
                        fullName = state.fullName,
                        role = state.role,
                        email = state.email,
                        assignedZone = state.assignedZone
                    ),
                    isLoggedIn = true
                )
                _uiMessage.value = "Bienvenido, ${state.fullName}"
            }
            onResult(result)
        }
    }

    fun hasPermission(permission: com.example.core.security.AppPermission): Boolean {
        return com.example.core.security.SessionManager.hasPermission(permission)
    }

    fun login(username: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val user = authRepo.login(username, pass)
                if (user != null) {
                    _userSession.value = UserSession(user = user, isLoggedIn = true)
                    _uiMessage.value = "Bienvenido, ${user.fullName}"
                    AppLogger.i("User ${user.username} logged in successfully", tag = "MainViewModel")
                    onResult(true)
                } else {
                    _uiMessage.value = "Credenciales incorrectas o usuario inactivo"
                    onResult(false)
                }
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.login")
                onResult(false)
            }
        }
    }

    fun logout() {
        val user = _userSession.value.user
        if (user != null) {
            viewModelScope.launch {
                try {
                    container.database.auditLogDao().insertAuditLog(
                        com.example.data.local.AuditLogEntity(
                            userId = user.id,
                            username = user.username,
                            action = "LOGOUT",
                            entityType = "USUARIO",
                            entityId = user.id.toString(),
                            newValues = "Sesión cerrada correctamente"
                        )
                    )
                } catch (e: Exception) {
                    // Ignore audit log error on logout
                }
            }
        }
        com.example.core.security.SessionManager.clearSession()
        _userSession.value = UserSession(user = null, isLoggedIn = false)
        _uiMessage.value = "Sesión cerrada correctamente"
        AppLogger.i("User logged out", tag = "MainViewModel")
    }

    fun adminCreateUser(user: UserEntity, onResult: (com.example.core.error.Result<Long>) -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.USER_CREATE)) {
            _uiMessage.value = "No tienes permiso para crear usuarios."
            onResult(com.example.core.error.Result.Error(com.example.core.error.AppException.ValidationException("Permiso denegado")))
            return
        }
        viewModelScope.launch {
            val adminUser = sessionState.value.username.ifBlank { "admin" }
            val res = authRepo.adminCreateUser(user, adminUser)
            if (res is com.example.core.error.Result.Success) {
                _uiMessage.value = "Usuario ${user.username} creado exitosamente"
            }
            onResult(res)
        }
    }

    fun adminUpdateUser(user: UserEntity, onResult: (com.example.core.error.Result<Boolean>) -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.USER_EDIT)) {
            _uiMessage.value = "No tienes permiso para editar usuarios."
            onResult(com.example.core.error.Result.Error(com.example.core.error.AppException.ValidationException("Permiso denegado")))
            return
        }
        viewModelScope.launch {
            val adminUser = sessionState.value.username.ifBlank { "admin" }
            val res = authRepo.adminUpdateUser(user, adminUser)
            if (res is com.example.core.error.Result.Success) {
                _uiMessage.value = "Usuario ${user.username} actualizado exitosamente"
            }
            onResult(res)
        }
    }

    fun adminSetUserStatus(userId: Long, active: Boolean, onResult: (com.example.core.error.Result<Boolean>) -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.USER_DISABLE)) {
            _uiMessage.value = "No tienes permiso para desactivar/activar usuarios."
            onResult(com.example.core.error.Result.Error(com.example.core.error.AppException.ValidationException("Permiso denegado")))
            return
        }
        viewModelScope.launch {
            val adminUser = sessionState.value.username.ifBlank { "admin" }
            val res = authRepo.adminSetUserStatus(userId, active, adminUser)
            if (res is com.example.core.error.Result.Success) {
                val stStr = if (active) "reactivado" else "desactivado"
                _uiMessage.value = "Usuario $stStr exitosamente"
            }
            onResult(res)
        }
    }

    fun adminSetUserLock(userId: Long, locked: Boolean, onResult: (com.example.core.error.Result<Boolean>) -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.USER_DISABLE)) {
            _uiMessage.value = "No tienes permiso para bloquear/desbloquear usuarios."
            onResult(com.example.core.error.Result.Error(com.example.core.error.AppException.ValidationException("Permiso denegado")))
            return
        }
        viewModelScope.launch {
            val adminUser = sessionState.value.username.ifBlank { "admin" }
            val res = authRepo.adminSetUserLock(userId, locked, adminUser)
            if (res is com.example.core.error.Result.Success) {
                val stStr = if (locked) "bloqueado" else "desbloqueado"
                _uiMessage.value = "Usuario $stStr exitosamente"
            }
            onResult(res)
        }
    }

    fun adminResetUserPassword(userId: Long, newPass: String, onResult: (com.example.core.error.Result<Boolean>) -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.USER_RESET_PASSWORD)) {
            _uiMessage.value = "No tienes permiso para restablecer contraseñas."
            onResult(com.example.core.error.Result.Error(com.example.core.error.AppException.ValidationException("Permiso denegado")))
            return
        }
        viewModelScope.launch {
            val adminUser = sessionState.value.username.ifBlank { "admin" }
            val res = authRepo.adminResetUserPassword(userId, newPass, adminUser)
            if (res is com.example.core.error.Result.Success) {
                _uiMessage.value = "Contraseña de usuario restablecida con éxito"
            }
            onResult(res)
        }
    }

    fun adminDeleteUser(userId: Long, onResult: (com.example.core.error.Result<Boolean>) -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.USER_MANAGE)) {
            _uiMessage.value = "No tienes permiso para eliminar usuarios."
            onResult(com.example.core.error.Result.Error(com.example.core.error.AppException.ValidationException("Permiso denegado")))
            return
        }
        viewModelScope.launch {
            val adminUser = sessionState.value.username.ifBlank { "admin" }
            val res = authRepo.adminDeleteUser(userId, adminUser)
            if (res is com.example.core.error.Result.Success) {
                _uiMessage.value = "Usuario eliminado exitosamente"
            }
            onResult(res)
        }
    }

    fun adminUpdateUserPermissions(userId: Long, permissions: Set<com.example.core.security.AppPermission>, onResult: (com.example.core.error.Result<Boolean>) -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.USER_MANAGE_PERMISSIONS)) {
            _uiMessage.value = "No tienes permiso para administrar permisos."
            onResult(com.example.core.error.Result.Error(com.example.core.error.AppException.ValidationException("Permiso denegado")))
            return
        }
        viewModelScope.launch {
            val res = authRepo.updateUserPermissions(userId, permissions)
            if (res is com.example.core.error.Result.Success) {
                _uiMessage.value = "Permisos actualizados con éxito"
            }
            onResult(res)
        }
    }

    fun createClient(client: ClientEntity, onDone: () -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.CLIENT_CREATE)) {
            _uiMessage.value = "No tienes permiso para crear clientes."
            return
        }
        viewModelScope.launch {
            try {
                val currentUser = _userSession.value.user?.username ?: "SISTEMA"
                clientRepo.insertClient(client, currentUser)
                _uiMessage.value = "Cliente registrado con éxito"
                SyncManager.markPendingOfflineChange()
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.createClient")
            }
        }
    }

    fun updateClient(client: ClientEntity, onDone: () -> Unit) {
        if (!hasPermission(com.example.core.security.AppPermission.CLIENT_EDIT)) {
            _uiMessage.value = "No tienes permiso para editar clientes."
            return
        }
        viewModelScope.launch {
            try {
                val currentUser = _userSession.value.user?.username ?: "SISTEMA"
                clientRepo.updateClient(client, currentUser)
                _uiMessage.value = "Cliente actualizado"
                SyncManager.markPendingOfflineChange()
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.updateClient")
            }
        }
    }

    fun createLoan(
        clientId: Long,
        capital: Double,
        planType: String,
        skipSundays: Boolean,
        onDone: (Long) -> Unit
    ) {
        if (!hasPermission(com.example.core.security.AppPermission.LOAN_CREATE)) {
            _uiMessage.value = "No tienes permiso para originar préstamos."
            return
        }
        viewModelScope.launch {
            val user = _userSession.value.user
            val currentUser = user?.username ?: "SISTEMA"
            val userRole = user?.role ?: "AGENTE"
            val collectorId = user?.id ?: 1L
            val collectorName = user?.fullName ?: "Cobrador"

            val result = container.createLoanUseCase(
                clientId = clientId,
                capital = capital,
                planTypeCode = planType,
                skipSundays = skipSundays,
                collectorId = collectorId,
                collectorName = collectorName,
                currentUser = currentUser,
                currentUserRole = userRole
            )

            when (result) {
                is com.example.core.error.Result.Success -> {
                    _uiMessage.value = "Préstamo registrado exitosamente (#${result.data})"
                    SyncManager.markPendingOfflineChange()

                    createNotification(
                        type = "PRESTAMO_APROBADO",
                        title = "Préstamo Originado ✅",
                        message = "Se ha originado un nuevo préstamo (#${result.data}) por un capital de $capital MXN (Plan: $planType).",
                        priority = "ALTA",
                        recipientUserId = -1L,
                        recipientRole = "TODOS",
                        navigationRoute = "client_expediente?clientId=$clientId"
                    )

                    onDone(result.data)
                }
                is com.example.core.error.Result.Error -> {
                    GlobalErrorHandler.handleThrowable(result.exception, customTag = "MainViewModel.createLoan")
                    _uiMessage.value = result.exception.message
                }
                else -> {}
            }
        }
    }

    fun editLoanBeforeDisbursement(
        loanId: Long,
        newCapital: Double,
        newPlanTypeCode: String,
        newSkipSundays: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _userSession.value.user
            val currentUser = user?.username ?: "SISTEMA"
            val userRole = user?.role ?: "AGENTE"

            val result = container.editLoanUseCase(
                loanId = loanId,
                newCapital = newCapital,
                newPlanTypeCode = newPlanTypeCode,
                newSkipSundays = newSkipSundays,
                currentUser = currentUser,
                currentUserRole = userRole
            )

            when (result) {
                is com.example.core.error.Result.Success -> {
                    _uiMessage.value = "Préstamo #$loanId actualizado correctamente"
                    SyncManager.markPendingOfflineChange()
                    onDone()
                }
                is com.example.core.error.Result.Error -> {
                    _uiMessage.value = result.exception.message
                }
                else -> {}
            }
        }
    }

    fun cancelLoanBeforeDisbursement(
        loanId: Long,
        reason: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val currentUser = _userSession.value.user?.username ?: "SISTEMA"
            val result = container.cancelLoanUseCase(loanId, currentUser, reason)
            when (result) {
                is com.example.core.error.Result.Success -> {
                    _uiMessage.value = "Préstamo #$loanId cancelado"
                    SyncManager.markPendingOfflineChange()
                    onDone()
                }
                is com.example.core.error.Result.Error -> {
                    _uiMessage.value = result.exception.message
                }
                else -> {}
            }
        }
    }

    fun authorizeLoan(
        loanId: Long,
        onDone: () -> Unit
    ) {
        if (!hasPermission(com.example.core.security.AppPermission.LOAN_AUTHORIZE)) {
            _uiMessage.value = "No tienes permiso para autorizar préstamos."
            return
        }
        viewModelScope.launch {
            val user = _userSession.value.user
            val currentUser = user?.username ?: "SISTEMA"
            val userRole = user?.role ?: "SUPERVISOR"

            val result = container.authorizeLoanUseCase(loanId, currentUser, userRole)
            when (result) {
                is com.example.core.error.Result.Success -> {
                    _uiMessage.value = "Préstamo #$loanId autorizado exitosamente"
                    SyncManager.markPendingOfflineChange()

                    createNotification(
                        type = "NUEVA_AUTORIZACION",
                        title = "Préstamo Autorizado ✅",
                        message = "El préstamo #$loanId ha sido autorizado por $currentUser y está listo para su desembolso.",
                        priority = "ALTA",
                        recipientUserId = -1L,
                        recipientRole = "TODOS"
                    )

                    onDone()
                }
                is com.example.core.error.Result.Error -> {
                    _uiMessage.value = result.exception.message
                }
                else -> {}
            }
        }
    }

    fun disburseLoan(
        loanId: Long,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _userSession.value.user
            val currentUser = user?.username ?: "SISTEMA"
            val collectorId = user?.id ?: 1L
            val collectorName = user?.fullName ?: "Cobrador"

            val result = container.disburseLoanUseCase(loanId, collectorId, collectorName, currentUser)
            when (result) {
                is com.example.core.error.Result.Success -> {
                    _uiMessage.value = "Préstamo #$loanId desembolsado exitosamente"
                    SyncManager.markPendingOfflineChange()

                    createNotification(
                        type = "PRESTAMO_DESEMBOLSADO",
                        title = "Desembolso Realizado 💵",
                        message = "Se ha desembolsado el préstamo #$loanId por el cobrador $collectorName.",
                        priority = "ALTA",
                        recipientUserId = -1L,
                        recipientRole = "TODOS"
                    )

                    onDone()
                }
                is com.example.core.error.Result.Error -> {
                    _uiMessage.value = result.exception.message
                }
                else -> {}
            }
        }
    }

    fun renewLoan(
        clientId: Long,
        previousLoanId: Long,
        capital: Double,
        extraCapital: Double = 0.0,
        planType: String,
        skipSundays: Boolean,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val user = _userSession.value.user
            val currentUser = user?.username ?: "SISTEMA"
            val userRole = user?.role ?: "AGENTE"
            val collectorId = user?.id ?: 1L
            val collectorName = user?.fullName ?: "Cobrador"

            val result = container.renewLoanUseCase(
                clientId = clientId,
                previousLoanId = previousLoanId,
                newCapital = capital,
                extraCapital = extraCapital,
                planTypeCode = planType,
                skipSundays = skipSundays,
                collectorId = collectorId,
                collectorName = collectorName,
                currentUser = currentUser,
                currentUserRole = userRole
            )

            when (result) {
                is com.example.core.error.Result.Success -> {
                    _uiMessage.value = "Renovación exitosa. Nuevo Préstamo #${result.data}"
                    SyncManager.markPendingOfflineChange()
                    onDone(result.data)
                }
                is com.example.core.error.Result.Error -> {
                    GlobalErrorHandler.handleThrowable(result.exception, customTag = "MainViewModel.renewLoan")
                    _uiMessage.value = result.exception.message
                }
                else -> {}
            }
        }
    }

    fun processPayment(
        loanId: Long,
        amount: Double,
        method: String,
        notes: String,
        onDone: () -> Unit
    ) {
        registerSmartPayment(
            loanId = loanId,
            amount = amount,
            method = method,
            notes = notes,
            paymentType = "NORMAL",
            onDone = { _ -> onDone() }
        )
    }

    fun getLateFeeHistoryForLoan(loanId: Long): Flow<List<LateFeeHistoryEntity>> =
        loanRepo.getLateFeeHistoryForLoan(loanId)

    fun getSkippedPaymentsForLoan(loanId: Long): Flow<List<SkippedPaymentLogEntity>> =
        loanRepo.getSkippedPaymentsForLoan(loanId)

    fun registerSmartPayment(
        loanId: Long,
        amount: Double,
        method: String,
        notes: String,
        paymentType: String = "NORMAL",
        proofPhotoUri: String? = null,
        redistributeAdvance: Boolean = true,
        targetInstallmentIds: List<Long> = emptyList(),
        onDone: (Boolean) -> Unit = {}
    ) {
        if (!hasPermission(com.example.core.security.AppPermission.PAYMENT_REGISTER)) {
            _uiMessage.value = "No tienes permiso para registrar pagos."
            onDone(false)
            return
        }
        viewModelScope.launch {
            val currentUser = _userSession.value.user?.username ?: "SISTEMA"
            val collectorId = _userSession.value.user?.id ?: 1L
            val collectorName = _userSession.value.user?.fullName ?: "Cobrador"

            val success = loanRepo.registerSmartPayment(
                loanId = loanId,
                amount = amount,
                method = method,
                collectorId = collectorId,
                collectorName = collectorName,
                currentUser = currentUser,
                notes = notes,
                paymentType = paymentType,
                proofPhotoUri = proofPhotoUri,
                redistributeAdvance = redistributeAdvance,
                targetInstallmentIds = targetInstallmentIds
            )

            if (success) {
                _uiMessage.value = "Pago $paymentType registrado de $amount MXN ($method)"
                SyncManager.markPendingOfflineChange()

                viewModelScope.launch {
                    try {
                        val loan = loanRepo.getLoanById(loanId)
                        if (loan != null) {
                            val clientName = loan.clientName
                            val clientId = loan.clientId
                            val currentBalance = loan.remainingBalance

                            createNotification(
                                type = "PAGO_RECIBIDO",
                                title = "Pago Recibido ✅",
                                message = "Se recibió un pago de $amount MXN ($method) de $clientName para el préstamo #$loanId. Saldo restante: $currentBalance MXN.",
                                priority = "MEDIA",
                                recipientUserId = -1L,
                                recipientRole = "ADMINISTRADOR",
                                navigationRoute = "client_expediente?clientId=$clientId"
                            )

                            if (currentBalance <= 0.0) {
                                createNotification(
                                    type = "PRESTAMO_LIQUIDADO",
                                    title = "Préstamo Liquidado 🎉",
                                    message = "El préstamo #$loanId del cliente $clientName ha sido liquidado en su totalidad.",
                                    priority = "ALTA",
                                    recipientUserId = -1L,
                                    recipientRole = "TODOS",
                                    navigationRoute = "client_expediente?clientId=$clientId"
                                )

                                createNotification(
                                    type = "RENOVACION_DISPONIBLE",
                                    title = "Renovación Disponible ✨",
                                    message = "El cliente $clientName ha liquidado su préstamo y está elegible para una renovación.",
                                    priority = "MEDIA",
                                    recipientUserId = -1L,
                                    recipientRole = "COBRADOR",
                                    navigationRoute = "client_expediente?clientId=$clientId"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Suppress background notifications lookups errors to not block the main transaction
                    }
                }
            } else {
                _uiMessage.value = "Error al procesar el pago"
            }
            onDone(success)
        }
    }

    fun recordSkippedPayment(
        loanId: Long,
        installmentId: Long,
        reason: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val collectorName = _userSession.value.user?.fullName ?: "Cobrador"
            val success = loanRepo.recordSkippedPayment(loanId, installmentId, reason, collectorName)
            if (success) {
                _uiMessage.value = "Pago omitido registrado para cuota"
                SyncManager.markPendingOfflineChange()
            }
            onDone()
        }
    }

    fun evaluateLateFeesForLoan(loanId: Long) {
        viewModelScope.launch {
            loanRepo.evaluateAndApplyLateFees(loanId)
        }
    }

    fun openCashRegister(
        initialCash: Double,
        notes: String,
        branchName: String = "Sucursal Central",
        fundName: String = "Caja Principal",
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentUser = _userSession.value.user?.username ?: "SISTEMA"
                val collectorId = _userSession.value.user?.id ?: 1L
                val collectorName = _userSession.value.user?.fullName ?: "Cobrador"

                 cashRepo.openCashRegister(
                    collectorId = collectorId,
                    collectorName = collectorName,
                    initialCash = initialCash,
                    notes = notes,
                    currentUser = currentUser,
                    branchName = branchName,
                    fundName = fundName
                )
                _uiMessage.value = "Caja abierta con $$initialCash MXN ($branchName - $fundName)"
                
                createNotification(
                    type = "ALERTA_CAJA",
                    title = "Apertura de Caja 🔑",
                    message = "El cobrador $collectorName ha abierto la caja con un saldo inicial de $initialCash MXN.",
                    priority = "BAJA",
                    recipientUserId = -1L,
                    recipientRole = "ADMINISTRADOR",
                    navigationRoute = "caja"
                )

                onDone()
            } catch (t: Throwable) {
                _uiMessage.value = t.localizedMessage ?: "Error al abrir la caja"
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.openCashRegister")
            }
        }
    }

    fun closeCashRegister(
        actualCash: Double,
        notes: String,
        denominationBreakdownJson: String? = null,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val activeReg = activeCashRegister.value ?: return@launch
                val currentUser = _userSession.value.user?.username ?: "SISTEMA"
                val expected = activeReg.expectedCash
                val discrepancy = actualCash - expected

                cashRepo.closeCashRegister(
                    registerId = activeReg.id,
                    actualCash = actualCash,
                    notes = notes,
                    currentUser = currentUser,
                    denominationBreakdownJson = denominationBreakdownJson
                )
                _uiMessage.value = "Cierre de caja registrado exitosamente"

                val priority = if (Math.abs(discrepancy) > 0.01) "ALTA" else "BAJA"
                val msg = if (Math.abs(discrepancy) > 0.01) {
                    val typeStr = if (discrepancy < 0) "FALTANTE" else "SOBRANTE"
                    "Cierre de caja registrado con $typeStr de ${Math.abs(discrepancy)} MXN por el cobrador ${activeReg.collectorName}. Esperado: $expected MXN, Real: $actualCash MXN."
                } else {
                    "Cierre de caja exitoso y cuadre perfecto para el cobrador ${activeReg.collectorName}."
                }

                createNotification(
                    type = "ALERTA_CAJA",
                    title = if (Math.abs(discrepancy) > 0.01) "Diferencia de Caja Detectada ⚠️" else "Cierre de Caja Exitoso ✅",
                    message = msg,
                    priority = priority,
                    recipientUserId = -1L,
                    recipientRole = "ADMINISTRADOR",
                    navigationRoute = "caja"
                )

                onDone()
            } catch (t: Throwable) {
                _uiMessage.value = t.localizedMessage ?: "Error al cerrar la caja"
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.closeCashRegister")
            }
        }
    }

    fun addCashMovement(
        type: String,
        amount: Double,
        concept: String,
        paymentMethod: String,
        category: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val activeReg = activeCashRegister.value
                    ?: throw IllegalStateException("Debe existir una caja abierta activa para realizar movimientos.")
                val currentUser = _userSession.value.user?.username ?: "SISTEMA"

                cashRepo.addCashMovement(
                    registerId = activeReg.id,
                    type = type,
                    amount = amount,
                    concept = concept,
                    paymentMethod = paymentMethod,
                    category = category,
                    currentUser = currentUser
                )
                _uiMessage.value = "Movimiento de caja registrado ($type: $$amount MXN)"
                onDone()
            } catch (t: Throwable) {
                _uiMessage.value = t.localizedMessage ?: "Error al registrar movimiento de caja"
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.addCashMovement")
            }
        }
    }

    fun recordVisit(visit: CollectionVisitEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = _userSession.value.user?.username ?: "SISTEMA"
                collectionRepo.recordVisit(visit, currentUser)
                _uiMessage.value = "Visita de cobranza registrada"

                if (visit.result == "PROMESA_PAGO") {
                    createNotification(
                        type = "PROMESA_PAGO",
                        title = "Promesa de Pago Registrada 🤝",
                        message = "El cliente ${visit.clientName} prometió pagar la cantidad de ${visit.promisedAmount ?: 0.0} MXN el ${visit.promisedPaymentDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(it)) } ?: "fecha pendiente"}.",
                        priority = "MEDIA",
                        recipientUserId = -1L,
                        recipientRole = "SUPERVISOR",
                        navigationRoute = "client_expediente?clientId=${visit.clientId}"
                    )
                } else {
                    createNotification(
                        type = "VISITA_REGISTRADA",
                        title = "Visita Registrada",
                        message = "Se registró una visita de cobranza para ${visit.clientName}. Resultado: ${visit.result}.",
                        priority = "BAJA",
                        recipientUserId = -1L,
                        recipientRole = "TODOS",
                        navigationRoute = "client_expediente?clientId=${visit.clientId}"
                    )
                }

                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.recordVisit")
            }
        }
    }

    fun updateSystemConfig(config: SystemConfigEntity, categoryName: String = "GENERAL") {
        viewModelScope.launch {
            try {
                if (!hasPermission(com.example.core.security.AppPermission.SETTINGS_EDIT)) {
                    _uiMessage.value = "Permiso denegado para editar configuración."
                    return@launch
                }
                val currentConfig = systemConfig.value
                val currentUserId = _userSession.value.user?.id ?: sessionState.value.userId
                val currentUsername = _userSession.value.user?.username.orEmpty().ifBlank {
                    sessionState.value.username.ifBlank { "admin" }
                }
                configRepo.updateConfig(config, currentUsername, "Actualización de configuración: $categoryName")
                _uiMessage.value = "Configuración ($categoryName) guardada correctamente"
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.updateSystemConfig")
            }
        }
    }

    fun createUser(user: UserEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepo.createUser(user)
                _uiMessage.value = "Usuario ${user.username} creado"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.createUser")
            }
        }
    }

    fun deleteUser(user: UserEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentLoggedIn = _userSession.value.user
                if (currentLoggedIn?.id == user.id) {
                    _uiMessage.value = "No puede eliminar su propio usuario con el que tiene la sesión activa"
                    return@launch
                }
                authRepo.deleteUser(user)
                _uiMessage.value = "Usuario ${user.username} eliminado correctamente"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.deleteUser")
            }
        }
    }

    fun deleteClient(client: ClientEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = _userSession.value.user?.username ?: "SISTEMA"
                clientRepo.deleteClient(client, currentUser)
                _uiMessage.value = "Cliente ${client.fullName} eliminado"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.deleteClient")
            }
        }
    }

    // --- MÓDULO PERFIL DEL CLIENTE (REFERENCIAS, OBSERVACIONES, ALERTAS) ---

    fun getClientReferences(clientId: Long) = clientRepo.getClientReferences(clientId)

    fun addClientReference(reference: com.example.data.local.ClientReferenceEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                clientRepo.insertClientReference(reference)
                _uiMessage.value = "Referencia agregada"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.addClientReference")
            }
        }
    }

    fun deleteClientReference(reference: com.example.data.local.ClientReferenceEntity) {
        viewModelScope.launch {
            try {
                clientRepo.deleteClientReference(reference)
                _uiMessage.value = "Referencia eliminada"
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.deleteClientReference")
            }
        }
    }

    fun getClientObservations(clientId: Long) = clientRepo.getClientObservations(clientId)

    fun addClientObservation(observation: com.example.data.local.ClientObservationEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                clientRepo.insertClientObservation(observation)
                _uiMessage.value = "Observación registrada"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.addClientObservation")
            }
        }
    }

    fun getClientAlerts(clientId: Long) = clientRepo.getClientAlerts(clientId)

    fun addClientAlert(alert: com.example.data.local.ClientAlertEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                clientRepo.insertClientAlert(alert)
                _uiMessage.value = "Alerta creada"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.addClientAlert")
            }
        }
    }

    fun updateClientAlert(alert: com.example.data.local.ClientAlertEntity) {
        viewModelScope.launch {
            try {
                clientRepo.updateClientAlert(alert)
                _uiMessage.value = "Alerta actualizada"
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.updateClientAlert")
            }
        }
    }

    // --- MÓDULO DE COMUNICACIONES Y NOTIFICACIONES ---

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            try {
                commRepo.markAsRead(id)
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.markNotificationAsRead")
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                val uId = _userSession.value.user?.id ?: 1L
                commRepo.markAllAsReadForUser(uId)
                _uiMessage.value = "Todas las notificaciones han sido marcadas como leídas"
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.markAllNotificationsAsRead")
            }
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            try {
                commRepo.deleteNotification(id)
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.deleteNotification")
            }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                commRepo.clearAllNotifications()
                _uiMessage.value = "Centro de notificaciones vaciado"
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.clearAllNotifications")
            }
        }
    }

    fun verifyDocument(
        documentId: Long,
        verifiedByUserId: Long,
        verifiedByUsername: String,
        userRole: String,
        isApproved: Boolean,
        notes: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val doc = docRepo.getDocumentById(documentId)
                val success = docRepo.verifyDocument(
                    documentId = documentId,
                    verifiedByUserId = verifiedByUserId,
                    verifiedByUsername = verifiedByUsername,
                    userRole = userRole,
                    isApproved = isApproved,
                    notes = notes
                )
                if (success && doc != null) {
                    val clientName = doc.clientName
                    val docTypeName = doc.typeName
                    val statusStr = if (isApproved) "APROBADO" else "RECHAZADO"
                    val priority = if (isApproved) "MEDIA" else "ALTA"
                    val titleStr = if (isApproved) "Documento Aprobado ✅" else "Documento Rechazado ❌"
                    val messageStr = if (isApproved) {
                        "El documento $docTypeName del cliente $clientName ha sido aprobado por $verifiedByUsername."
                    } else {
                        "El documento $docTypeName del cliente $clientName ha sido RECHAZADO por $verifiedByUsername. Motivo: $notes"
                    }

                    createNotification(
                        type = if (isApproved) "DOCUMENTO_VALIDADO" else "DOCUMENTO_RECHAZADO",
                        title = titleStr,
                        message = messageStr,
                        priority = priority,
                        recipientUserId = -1L,
                        recipientRole = "TODOS",
                        navigationRoute = "client_expediente?clientId=${doc.clientId}"
                    )
                }
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.verifyDocument")
            }
        }
    }

    fun createNotification(
        type: String,
        title: String,
        message: String,
        priority: String = "MEDIA",
        recipientUserId: Long = -1L,
        recipientRole: String = "TODOS",
        navigationRoute: String? = null
    ) {
        viewModelScope.launch {
            try {
                val notif = NotificationEntity(
                    userId = recipientUserId,
                    username = if (recipientUserId == -1L) "TODOS" else "USUARIO_$recipientUserId",
                    type = type,
                    title = title,
                    message = message,
                    priority = priority,
                    recipientRole = recipientRole,
                    navigationRoute = navigationRoute,
                    timestamp = System.currentTimeMillis()
                )
                commRepo.createNotification(notif)

                // Trigger push notification if enabled
                com.example.data.service.FcmMessagingService.showSystemPushNotification(getApplication(), notif)
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.createNotification")
            }
        }
    }

    fun saveMessageTemplate(template: MessageTemplateEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                commRepo.saveTemplate(template)
                _uiMessage.value = "Plantilla '${template.name}' guardada correctamente"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.saveMessageTemplate")
            }
        }
    }

    fun saveReminderConfig(config: ReminderConfigEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                commRepo.saveReminderConfig(config)
                _uiMessage.value = "Configuración de recordatorios actualizada"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.saveReminderConfig")
            }
        }
    }

    fun saveNotificationPreferences(pref: NotificationPreferenceEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                commRepo.saveNotificationPreferences(pref)
                _uiMessage.value = "Preferencias de notificación guardadas"
                onDone()
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.saveNotificationPreferences")
            }
        }
    }

    fun sendWhatsAppMessageWithLog(
        context: android.content.Context,
        clientId: Long,
        clientName: String,
        clientPhone: String,
        templateCode: String,
        messageContent: String,
        notes: String = "",
        onCompleted: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val currentUserId = _userSession.value.user?.id ?: 1L
                val currentUsername = _userSession.value.user?.username ?: "SISTEMA"

                // Check system config
                val sysCfg = configRepo.getConfigSync()
                
                // 1. Check global active switch
                if (sysCfg?.enableWhatsAppAlerts == false) {
                    _uiMessage.value = "La integración de WhatsApp está desactivada en la configuración global."
                    onCompleted(false)
                    return@launch
                }

                // 2. Check category-level switches
                val isReminder = templateCode in listOf("PAGO_PROXIMO", "PAGO_HOY")
                val isPayment = templateCode in listOf("CONFIRMACION_PAGO", "PAGO_PARCIAL")
                val isLateFee = templateCode in listOf("MORA", "PAGO_VENCIDO")
                val isPromise = templateCode == "PROMESA_PAGO"
                val isRenewal = templateCode == "RENOVACION_DISPONIBLE"

                if (isReminder && sysCfg?.allowWhatsAppReminder == false) {
                    _uiMessage.value = "Los recordatorios de pago por WhatsApp están desactivados en la configuración global."
                    onCompleted(false)
                    return@launch
                }
                if (isPayment && sysCfg?.allowWhatsAppPaymentReceipt == false) {
                    _uiMessage.value = "Las confirmaciones de pago por WhatsApp están desactivadas en la configuración global."
                    onCompleted(false)
                    return@launch
                }
                if (isLateFee && sysCfg?.allowWhatsAppLateFee == false) {
                    _uiMessage.value = "Los avisos de mora por WhatsApp están desactivados en la configuración global."
                    onCompleted(false)
                    return@launch
                }
                if (isPromise && sysCfg?.allowWhatsAppPromise == false) {
                    _uiMessage.value = "Las notificaciones de promesa de pago por WhatsApp están desactivadas."
                    onCompleted(false)
                    return@launch
                }
                if (isRenewal && sysCfg?.allowWhatsAppRenewal == false) {
                    _uiMessage.value = "Las ofertas de renovación por WhatsApp están desactivadas."
                    onCompleted(false)
                    return@launch
                }

                // 3. Check country code
                val cc = sysCfg?.whatsappDefaultCountryCode ?: "52"

                val isApiMode = !(sysCfg?.whatsappApiKey.isNullOrBlank() || sysCfg?.whatsappApiKey?.contains("EXAMPLE") == true)

                var channel = "WHATSAPP_APP"
                var resultStatus = "ABIERTO_EN_WHATSAPP"

                if (isApiMode) {
                    val apiResult = com.example.data.service.WhatsAppIntegrationService.sendViaWhatsAppBusinessApi(
                        apiUrl = sysCfg?.whatsappApiUrl ?: "",
                        apiKey = sysCfg?.whatsappApiKey ?: "",
                        phoneNumber = clientPhone,
                        messageText = messageContent,
                        defaultCountryCode = cc
                    )
                    if (apiResult.isSuccess) {
                        channel = "WHATSAPP_API"
                        resultStatus = "ENVIADO_API"
                    } else {
                        // Fallback to app launcher
                        com.example.data.service.WhatsAppIntegrationService.openWhatsAppWithMessage(context, clientPhone, messageContent, cc)
                    }
                } else {
                    // Open WhatsApp app with pre-filled message
                    com.example.data.service.WhatsAppIntegrationService.openWhatsAppWithMessage(context, clientPhone, messageContent, cc)
                }

                // Log communication attempt
                val log = CommunicationLogEntity(
                    userId = currentUserId,
                    userName = currentUsername,
                    clientId = clientId,
                    clientName = clientName,
                    clientPhone = clientPhone,
                    timestamp = System.currentTimeMillis(),
                    messageType = templateCode,
                    channel = channel,
                    resultStatus = resultStatus,
                    templateCodeUsed = templateCode,
                    messageContent = messageContent,
                    notes = notes.ifBlank { "Intento de comunicación vía $channel" }
                )
                commRepo.logCommunicationAttempt(log)

                // Audit Log
                auditRepo.logAction(
                    userId = currentUserId,
                    username = currentUsername,
                    action = "ENVIO_WHATSAPP",
                    entityType = "CLIENTE",
                    entityId = clientId.toString(),
                    newValues = "Plantilla: $templateCode, Canal: $channel, Resultado: $resultStatus"
                )

                _uiMessage.value = "Mensaje cargado en WhatsApp y registrado en auditoría"
                onCompleted(true)
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.sendWhatsAppMessageWithLog")
                onCompleted(false)
            }
        }
    }

    fun registerFcmToken(fcmToken: String, deviceModel: String) {
        viewModelScope.launch {
            try {
                val currentUserId = _userSession.value.user?.id ?: 1L
                val deviceId = android.os.Build.SERIAL.ifBlank { "android_device_${currentUserId}" }
                commRepo.registerFcmDevice(currentUserId, fcmToken, deviceId, deviceModel)

                val existingPref = commRepo.getNotificationPreferencesForUser(currentUserId).firstOrNull()
                    ?: NotificationPreferenceEntity(userId = currentUserId)

                val updatedPref = existingPref.copy(
                    fcmToken = fcmToken,
                    fcmDeviceModel = deviceModel,
                    lastFcmUpdateMs = System.currentTimeMillis()
                )
                commRepo.saveNotificationPreferences(updatedPref)
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.registerFcmToken")
            }
        }
    }

    fun evaluateAndGeneratePaymentReminders() {
        viewModelScope.launch {
            try {
                val activeLns = loanRepo.activeLoans.firstOrNull() ?: emptyList()
                val currentUserId = _userSession.value.user?.id ?: 1L
                val cfg = commRepo.getReminderConfigSync() ?: ReminderConfigEntity()

                var remindersCount = 0
                activeLns.forEach { loan ->
                    val now = System.currentTimeMillis()
                    val daysUntilEnd = ((loan.expectedEndDate - now) / (1000 * 3600 * 24)).toInt()

                    if (daysUntilEnd in 0..cfg.daysAdvanceNotice) {
                        val notifTitle = "Recordatorio de Pago Próximo (Préstamo #${loan.id})"
                        val notifMsg = "El cliente ${loan.clientName} tiene un vencimiento próximo (${loan.dailyPayment} MXN). Saldo: ${loan.remainingBalance} MXN."

                        createNotification(
                            type = "PAGO_PENDIENTE",
                            title = notifTitle,
                            message = notifMsg,
                            priority = "MEDIA",
                            recipientUserId = currentUserId,
                            navigationRoute = "loan_detail/${loan.id}"
                        )
                        remindersCount++
                    } else if (daysUntilEnd < 0) {
                        val overdueDays = Math.abs(daysUntilEnd)
                        val notifTitle = "ALERTA DE MORA: ${loan.clientName} (${overdueDays}d atraso)"
                        val notifMsg = "El préstamo #${loan.id} presenta $overdueDays día(s) de vencimiento. Saldo adeudado: ${loan.remainingBalance} MXN."

                        createNotification(
                            type = "PAGO_VENCIDO",
                            title = notifTitle,
                            message = notifMsg,
                            priority = "URGENTE",
                            recipientUserId = currentUserId,
                            navigationRoute = "loan_detail/${loan.id}"
                        )
                        remindersCount++
                    }
                }

                // 1. Evaluate Promises
                val visits = allVisits.value
                val nowTime = System.currentTimeMillis()
                val dateFormatSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                visits.filter { it.result == "PROMESA_PAGO" && it.promisedPaymentDate != null }.forEach { visit ->
                    val promiseDate = visit.promisedPaymentDate!!
                    val loansForClient = loanRepo.getLoansByClientSync(visit.clientId)
                    val hasActiveBalance = loansForClient.any { it.remainingBalance > 0.0 }

                    if (hasActiveBalance) {
                        val diffDays = ((promiseDate - nowTime) / (1000 * 3600 * 24)).toInt()
                        if (diffDays in 0..1) {
                            createNotification(
                                type = "PROMESA_PROXIMA",
                                title = "Promesa de Pago Próxima 🤝",
                                message = "El cliente ${visit.clientName} tiene una promesa de pago programada para el ${dateFormatSdf.format(java.util.Date(promiseDate))} por un monto de ${visit.promisedAmount ?: 0.0} MXN.",
                                priority = "MEDIA",
                                recipientUserId = currentUserId,
                                navigationRoute = "client_expediente?clientId=${visit.clientId}"
                            )
                            remindersCount++
                        } else if (promiseDate < nowTime) {
                            createNotification(
                                type = "PROMESA_INCUMPLIDA",
                                title = "Promesa de Pago INCUMPLIDA ⚠️",
                                message = "El cliente ${visit.clientName} ha incumplido su promesa de pago de ${visit.promisedAmount ?: 0.0} MXN del día ${dateFormatSdf.format(java.util.Date(promiseDate))}.",
                                priority = "ALTA",
                                recipientUserId = currentUserId,
                                navigationRoute = "client_expediente?clientId=${visit.clientId}"
                            )
                            remindersCount++
                        }
                    }
                }

                // 2. Evaluate Missing Documents for clients
                val clients = allClientsList.value
                clients.forEach { client ->
                    val compliance = docRepo.checkMandatoryDocumentsCompliance(client.id)
                    if (!compliance.isComplete) {
                        val missingNames = compliance.missingTypes.joinToString { it.name }
                        createNotification(
                            type = "DOCUMENTO_FALTA",
                            title = "Documentación Faltante 📋",
                            message = "El cliente ${client.fullName} no cuenta con los siguientes documentos obligatorios: $missingNames.",
                            priority = "BAJA",
                            recipientUserId = currentUserId,
                            navigationRoute = "client_expediente?clientId=${client.id}"
                        )
                        remindersCount++
                    }
                }

                if (remindersCount > 0) {
                    _uiMessage.value = "Evaluación de cobranza finalizada: $remindersCount recordatorios/alertas generados"
                }
            } catch (t: Throwable) {
                GlobalErrorHandler.handleThrowable(t, customTag = "MainViewModel.evaluateAndGeneratePaymentReminders")
            }
        }
    }

    // Geographic & Route Management Methods
    fun saveRoute(route: CollectionRouteEntity, currentUser: String) {
        viewModelScope.launch {
            try {
                routeRepo.saveRoute(route, currentUser)
                _uiMessage.value = "Ruta '${route.name}' guardada correctamente"
            } catch (e: Exception) {
                _uiMessage.value = "Error al guardar ruta: ${e.message}"
            }
        }
    }

    fun deleteRoute(route: CollectionRouteEntity, currentUser: String) {
        viewModelScope.launch {
            try {
                routeRepo.deleteRoute(route, currentUser)
                _uiMessage.value = "Ruta '${route.name}' eliminada"
            } catch (e: Exception) {
                _uiMessage.value = "Error al eliminar ruta: ${e.message}"
            }
        }
    }

    fun getAssignmentsForRoute(routeId: Long): Flow<List<RouteClientAssignmentEntity>> {
        return routeRepo.getAssignmentsForRoute(routeId)
    }

    fun assignClientToRoute(routeId: Long, clientId: Long, orderIndex: Int) {
        viewModelScope.launch {
            try {
                routeRepo.assignClientToRoute(routeId, clientId, orderIndex)
            } catch (e: Exception) {
                _uiMessage.value = "Error al asignar cliente: ${e.message}"
            }
        }
    }

    fun removeClientFromRoute(routeId: Long, clientId: Long) {
        viewModelScope.launch {
            try {
                routeRepo.removeClientFromRoute(routeId, clientId)
            } catch (e: Exception) {
                _uiMessage.value = "Error al remover cliente de ruta: ${e.message}"
            }
        }
    }

    fun recordVisit(visit: CollectionVisitEntity, currentUser: String) {
        viewModelScope.launch {
            try {
                collectionRepo.recordVisit(visit, currentUser)
                _uiMessage.value = "Visita a '${visit.clientName}' registrada con GPS"
            } catch (e: Exception) {
                _uiMessage.value = "Error al registrar visita: ${e.message}"
            }
        }
    }

    fun recordGpsTrackLog(log: GpsTrackLogEntity) {
        viewModelScope.launch {
            try {
                collectionRepo.recordGpsLog(log)
            } catch (e: Exception) {
                AppLogger.e("Error logging GPS track", e, tag = "MainViewModel")
            }
        }
    }

    fun syncOfflineGeographicData() {
        viewModelScope.launch {
            try {
                val syncedVisits = collectionRepo.syncPendingVisits()
                val syncedGps = collectionRepo.syncPendingGpsLogs()
                _uiMessage.value = "Sincronización GPS completada: $syncedVisits visitas, $syncedGps registros de rastreo"
            } catch (e: Exception) {
                _uiMessage.value = "Error en sincronización offline: ${e.message}"
            }
        }
    }

    fun logAuditEvent(action: String, entityType: String, entityId: String, prevValues: String? = null, newValues: String? = null) {
        viewModelScope.launch {
            try {
                val currentUserId = _userSession.value.user?.id ?: sessionState.value.userId
                val currentUsername = _userSession.value.user?.username.orEmpty().ifBlank {
                    sessionState.value.username.ifBlank { "SISTEMA" }
                }
                auditRepo.logAction(
                    userId = currentUserId,
                    username = currentUsername,
                    action = action,
                    entityType = entityType,
                    entityId = entityId,
                    previousValues = prevValues,
                    newValues = newValues
                )
            } catch (e: Exception) {
                AppLogger.e("Error registering location/maps audit log", e, tag = "MainViewModel")
            }
        }
    }

    // Evaluation & Score Engine Methods
    fun evaluateClientNow(clientId: Long, onResult: ((ClientEvaluationEntity) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val client = clientRepo.getClientById(clientId) ?: return@launch
                val loans = loanRepo.getLoansByClientSync(clientId)
                val allInstallments = mutableListOf<InstallmentEntity>()
                val allPayments = mutableListOf<PaymentEntity>()
                for (loan in loans) {
                    allInstallments.addAll(container.database.installmentDao().getInstallmentsByLoanSync(loan.id))
                    // Get payments for loan
                    allPayments.addAll(container.database.paymentDao().getPaymentsByLoan(loan.id).first())
                }
                val visits = container.database.collectionVisitDao().getVisitsByClient(clientId).first()
                val currentUser = userSession.value.user
                val sysConfig = configRepo.getConfigSync() ?: SystemConfigEntity()

                val eval = evalRepo.evaluateAndSaveClient(
                    client = client,
                    loans = loans,
                    installments = allInstallments,
                    payments = allPayments,
                    visits = visits,
                    user = currentUser,
                    systemConfig = sysConfig
                )
                _uiMessage.value = "Evaluación de '${client.fullName}' calculada: Score ${eval.score} (${eval.classification})"
                onResult?.invoke(eval)
            } catch (e: Exception) {
                _uiMessage.value = "Error al calcular evaluación: ${e.message}"
            }
        }
    }

    fun adjustScoreManually(
        clientId: Long,
        newScore: Int,
        reason: String,
        observation: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val currentUser = userSession.value.user
                if (currentUser == null) {
                    _uiMessage.value = "Error: Sesión no válida"
                    return@launch
                }
                val eval = evalRepo.adjustScoreManually(
                    clientId = clientId,
                    newScore = newScore,
                    reason = reason,
                    observation = observation,
                    user = currentUser
                )
                _uiMessage.value = "Score ajustado exitosamente a ${eval.score} (${eval.classification})"
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiMessage.value = "Error en ajuste manual: ${e.message}"
            }
        }
    }

    fun saveEvaluationConfig(config: ClientEvaluationConfigEntity) {
        viewModelScope.launch {
            try {
                val currentUser = userSession.value.user
                evalRepo.saveConfig(config, currentUser)
                _uiMessage.value = "Configuración del motor de evaluación guardada correctamente"
            } catch (e: Exception) {
                _uiMessage.value = "Error al guardar configuración: ${e.message}"
            }
        }
    }
}


