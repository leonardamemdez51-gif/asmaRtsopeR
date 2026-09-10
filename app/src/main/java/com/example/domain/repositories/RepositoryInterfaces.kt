package com.example.domain.repositories

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    val allUsers: Flow<List<UserEntity>>
    suspend fun login(username: String, password: String): UserEntity?
    suspend fun loginWithDetails(identifier: String, password: String, rememberMe: Boolean): com.example.core.error.Result<com.example.core.security.UserSessionState>
    suspend fun changePassword(userId: Long, oldPass: String, newPass: String): com.example.core.error.Result<Boolean>
    suspend fun requestPasswordReset(identifier: String): com.example.core.error.Result<String>
    suspend fun resetPasswordWithToken(token: String, newPass: String): com.example.core.error.Result<Boolean>
    suspend fun updateUserPermissions(userId: Long, permissions: Set<com.example.core.security.AppPermission>): com.example.core.error.Result<Boolean>
    suspend fun createUser(user: UserEntity): Long
    suspend fun updateUser(user: UserEntity)
    suspend fun deleteUser(user: UserEntity)
    suspend fun adminCreateUser(user: UserEntity, adminUsername: String): com.example.core.error.Result<Long>
    suspend fun adminUpdateUser(user: UserEntity, adminUsername: String): com.example.core.error.Result<Boolean>
    suspend fun adminSetUserStatus(userId: Long, active: Boolean, adminUsername: String): com.example.core.error.Result<Boolean>
    suspend fun adminSetUserLock(userId: Long, locked: Boolean, adminUsername: String): com.example.core.error.Result<Boolean>
    suspend fun adminResetUserPassword(userId: Long, newPassword: String, adminUsername: String): com.example.core.error.Result<Boolean>
    suspend fun adminDeleteUser(userId: Long, adminUsername: String): com.example.core.error.Result<Boolean>
}

interface IClientRepository {
    val allClients: Flow<List<ClientEntity>>
    val activeClientsCount: Flow<Int>
    fun searchClients(query: String): Flow<List<ClientEntity>>
    suspend fun getClientById(id: Long): ClientEntity?
    suspend fun insertClient(client: ClientEntity, currentUser: String): Long
    suspend fun updateClient(client: ClientEntity, currentUser: String)
    suspend fun deleteClient(client: ClientEntity, currentUser: String)

    fun getClientReferences(clientId: Long): Flow<List<ClientReferenceEntity>>
    suspend fun insertClientReference(reference: ClientReferenceEntity): Long
    suspend fun deleteClientReference(reference: ClientReferenceEntity)

    fun getClientObservations(clientId: Long): Flow<List<ClientObservationEntity>>
    suspend fun insertClientObservation(observation: ClientObservationEntity): Long

    fun getClientAlerts(clientId: Long): Flow<List<ClientAlertEntity>>
    suspend fun insertClientAlert(alert: ClientAlertEntity): Long
    suspend fun updateClientAlert(alert: ClientAlertEntity)
}

interface ILoanRepository {
    val allLoans: Flow<List<LoanEntity>>
    val activeLoans: Flow<List<LoanEntity>>
    val activeLoansCount: Flow<Int>
    val totalActiveCapital: Flow<Double?>
    val totalRecoveredCapital: Flow<Double?>
    val allPayments: Flow<List<PaymentEntity>>
    fun getLoansByClient(clientId: Long): Flow<List<LoanEntity>>
    suspend fun getLoansByClientSync(clientId: Long): List<LoanEntity>
    suspend fun getLoanById(id: Long): LoanEntity?
    fun getInstallmentsByLoan(loanId: Long): Flow<List<InstallmentEntity>>
    suspend fun getInstallmentsByLoanSync(loanId: Long): List<InstallmentEntity>
    
    suspend fun createLoan(
        clientId: Long,
        capital: Double,
        planTypeCode: String,
        skipSundays: Boolean,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        currentUserRole: String = "AGENTE",
        isRenewal: Boolean = false,
        previousLoanId: Long? = null
    ): Long

    suspend fun editLoanBeforeDisbursement(
        loanId: Long,
        newCapital: Double,
        newPlanTypeCode: String,
        newSkipSundays: Boolean,
        currentUser: String,
        currentUserRole: String = "AGENTE"
    ): Boolean

    suspend fun cancelLoanBeforeDisbursement(
        loanId: Long,
        currentUser: String,
        reason: String = "Cancelado por usuario"
    ): Boolean

    suspend fun authorizeLoan(
        loanId: Long,
        currentUser: String,
        currentUserRole: String = "SUPERVISOR"
    ): Boolean

    suspend fun disburseLoan(
        loanId: Long,
        collectorId: Long,
        collectorName: String,
        currentUser: String
    ): Boolean

    suspend fun renewLoan(
        clientId: Long,
        previousLoanId: Long,
        capital: Double,
        planTypeCode: String,
        skipSundays: Boolean,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        currentUserRole: String = "AGENTE"
    ): Long

    suspend fun renewPlusLoan(
        clientId: Long,
        previousLoanId: Long,
        newCapital: Double,
        extraCapital: Double,
        planTypeCode: String,
        skipSundays: Boolean,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        currentUserRole: String = "AGENTE"
    ): Long

    suspend fun registerPayment(
        loanId: Long,
        amount: Double,
        method: String,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        notes: String = ""
    ): Boolean

    fun getLateFeeHistoryForLoan(loanId: Long): Flow<List<LateFeeHistoryEntity>>
    fun getSkippedPaymentsForLoan(loanId: Long): Flow<List<SkippedPaymentLogEntity>>
    suspend fun recordSkippedPayment(loanId: Long, installmentId: Long, reason: String, collectorName: String): Boolean
    suspend fun registerSmartPayment(
        loanId: Long,
        amount: Double,
        method: String,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        notes: String = "",
        paymentType: String = "NORMAL",
        proofPhotoUri: String? = null,
        redistributeAdvance: Boolean = true,
        targetInstallmentIds: List<Long> = emptyList()
    ): Boolean
    suspend fun evaluateAndApplyLateFees(loanId: Long): Boolean
}

interface ICashRepository {
    val activeCashRegister: Flow<CashRegisterEntity?>
    val allCashRegisters: Flow<List<CashRegisterEntity>>
    fun getActiveCashRegisterForCollectorFlow(collectorId: Long): Flow<CashRegisterEntity?>
    fun getMovementsForRegister(registerId: Long): Flow<List<CashMovementEntity>>
    fun getAllMovements(): Flow<List<CashMovementEntity>>
    suspend fun getActiveCashRegisterForCollector(collectorId: Long): CashRegisterEntity?
    suspend fun openCashRegister(
        collectorId: Long,
        collectorName: String,
        initialCash: Double,
        notes: String,
        currentUser: String,
        branchName: String = "Sucursal Central",
        fundName: String = "Fondo Operativo General"
    ): Long
    suspend fun closeCashRegister(
        registerId: Long,
        actualCash: Double,
        notes: String,
        currentUser: String,
        denominationBreakdownJson: String? = null
    ): Boolean
    suspend fun addCashMovement(
        registerId: Long,
        type: String,
        amount: Double,
        concept: String,
        paymentMethod: String,
        category: String,
        currentUser: String
    ): Boolean
}

interface ICollectionRepository {
    val allVisits: Flow<List<CollectionVisitEntity>>
    fun getVisitsByClient(clientId: Long): Flow<List<CollectionVisitEntity>>
    suspend fun recordVisit(visit: CollectionVisitEntity, currentUser: String): Long
    suspend fun getPendingSyncVisits(): List<CollectionVisitEntity>
    suspend fun syncPendingVisits(): Int

    val allGpsLogs: Flow<List<GpsTrackLogEntity>>
    fun getGpsLogsForCollector(collectorId: Long): Flow<List<GpsTrackLogEntity>>
    suspend fun recordGpsLog(log: GpsTrackLogEntity): Long
    suspend fun syncPendingGpsLogs(): Int
}

interface IRouteRepository {
    val allRoutes: Flow<List<CollectionRouteEntity>>
    fun getActiveRoutesForCollector(collectorId: Long): Flow<List<CollectionRouteEntity>>
    fun getRoutesByZone(zone: String): Flow<List<CollectionRouteEntity>>
    suspend fun getRouteById(id: Long): CollectionRouteEntity?
    suspend fun saveRoute(route: CollectionRouteEntity, currentUser: String): Long
    suspend fun deleteRoute(route: CollectionRouteEntity, currentUser: String)

    fun getAssignmentsForRoute(routeId: Long): Flow<List<RouteClientAssignmentEntity>>
    suspend fun getAssignmentsForRouteSync(routeId: Long): List<RouteClientAssignmentEntity>
    suspend fun saveAssignmentsForRoute(routeId: Long, assignments: List<RouteClientAssignmentEntity>): Boolean
    suspend fun assignClientToRoute(routeId: Long, clientId: Long, orderIndex: Int): Long
    suspend fun removeClientFromRoute(routeId: Long, clientId: Long)
}

interface IAuditRepository {
    val allAuditLogs: Flow<List<AuditLogEntity>>
    suspend fun logAction(
        userId: Long,
        username: String,
        action: String,
        entityType: String,
        entityId: String,
        previousValues: String? = null,
        newValues: String? = null,
        traceId: String? = null,
        userRole: String? = null,
        reason: String? = null,
        notes: String? = null,
        result: String = "EXITOSO",
        latitude: Double? = null,
        longitude: Double? = null
    ): Long
}

interface IConfigRepository {
    val config: Flow<SystemConfigEntity?>
    suspend fun getConfigSync(): SystemConfigEntity?
    suspend fun updateConfig(config: SystemConfigEntity, modifiedBy: String, notes: String)
}

interface ICommunicationRepository {
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    fun getUnreadCountForUser(userId: Long): Flow<Int>
    fun getTotalUnreadCount(): Flow<Int>
    suspend fun createNotification(notification: NotificationEntity): Long
    suspend fun markAsRead(id: Long)
    suspend fun markAllAsReadForUser(userId: Long)
    suspend fun deleteNotification(id: Long)
    suspend fun clearAllNotifications()

    val allTemplates: Flow<List<MessageTemplateEntity>>
    suspend fun getTemplateByCode(code: String): MessageTemplateEntity?
    suspend fun saveTemplate(template: MessageTemplateEntity): Long

    val reminderConfig: Flow<ReminderConfigEntity?>
    suspend fun getReminderConfigSync(): ReminderConfigEntity?
    suspend fun saveReminderConfig(config: ReminderConfigEntity)

    val allCommunicationLogs: Flow<List<CommunicationLogEntity>>
    fun getCommunicationLogsByClient(clientId: Long): Flow<List<CommunicationLogEntity>>
    suspend fun logCommunicationAttempt(log: CommunicationLogEntity): Long

    fun getNotificationPreferencesForUser(userId: Long): Flow<NotificationPreferenceEntity?>
    suspend fun saveNotificationPreferences(pref: NotificationPreferenceEntity)

    suspend fun registerFcmDevice(userId: Long, fcmToken: String, deviceId: String, deviceModel: String): Long
}

