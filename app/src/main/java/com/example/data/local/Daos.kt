package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :identifier OR email = :identifier LIMIT 1")
    suspend fun getUserByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE passwordResetToken = :token LIMIT 1")
    suspend fun getUserByResetToken(token: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMINISTRADOR' AND active = 1")
    suspend fun getActiveAdminCount(): Int
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY fullName ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): ClientEntity?

    @Query("""
        SELECT * FROM clients 
        WHERE fullName LIKE '%' || :query || '%' 
           OR phone LIKE '%' || :query || '%' 
           OR curp LIKE '%' || :query || '%' 
           OR rfc LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%' 
           OR folio LIKE '%' || :query || '%'
           OR CAST(id AS TEXT) LIKE '%' || :query || '%'
        ORDER BY fullName ASC
    """)
    fun searchClients(query: String): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    // References
    @Query("SELECT * FROM client_references WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getClientReferences(clientId: Long): Flow<List<ClientReferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClientReference(reference: ClientReferenceEntity): Long

    @Delete
    suspend fun deleteClientReference(reference: ClientReferenceEntity)

    // Observations
    @Query("SELECT * FROM client_observations WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getClientObservations(clientId: Long): Flow<List<ClientObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClientObservation(observation: ClientObservationEntity): Long

    // Alerts
    @Query("SELECT * FROM client_alerts WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getClientAlerts(clientId: Long): Flow<List<ClientAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClientAlert(alert: ClientAlertEntity): Long

    @Update
    suspend fun updateClientAlert(alert: ClientAlertEntity)

    @Query("SELECT COUNT(*) FROM clients WHERE status = 'ACTIVO'")
    fun getActiveClientsCount(): Flow<Int>
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY id DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE clientId = :clientId ORDER BY id DESC")
    fun getLoansByClient(clientId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE clientId = :clientId AND status = 'ACTIVO'")
    fun getActiveLoansByClient(clientId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = 'ACTIVO' ORDER BY id DESC")
    fun getActiveLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE clientId = :clientId AND status IN ('ACTIVO', 'VENCIDO')")
    suspend fun getActiveLoansByClientSync(clientId: Long): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE clientId = :clientId")
    suspend fun getLoansByClientSync(clientId: Long): List<LoanEntity>

    @Query("SELECT COUNT(*) FROM loans WHERE clientId = :clientId AND status IN ('ACTIVO', 'VENCIDO')")
    suspend fun getActiveLoansCountByClientSync(clientId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("SELECT COUNT(*) FROM loans WHERE status = 'ACTIVO'")
    fun getActiveLoansCount(): Flow<Int>

    @Query("SELECT SUM(capital) FROM loans WHERE status = 'ACTIVO'")
    fun getTotalActiveCapital(): Flow<Double?>

    @Query("SELECT SUM(paidAmount) FROM loans")
    fun getTotalRecoveredCapital(): Flow<Double?>
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY id ASC")
    fun getAllInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun getInstallmentsByLoan(loanId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE dueDateFormatted <= :todayStr AND status IN ('PENDIENTE', 'PARCIAL', 'VENCIDO') ORDER BY dueDate ASC")
    fun getOverdueAndTodayInstallments(todayStr: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    suspend fun getInstallmentsByLoanSync(loanId: Long): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE dueDate = :dueDateMs ORDER BY loanId ASC")
    fun getInstallmentsForDate(dueDateMs: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE dueDateFormatted = :dateStr ORDER BY loanId ASC")
    fun getInstallmentsByDateString(dateStr: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE dueDateFormatted = :dateStr")
    suspend fun getInstallmentsByDateStringSync(dateStr: String): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallmentById(id: Long): InstallmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<InstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("SELECT * FROM installments WHERE loanId = :loanId AND status IN ('PENDIENTE', 'PARCIAL', 'VENCIDO') ORDER BY installmentNumber ASC")
    suspend fun getUnpaidInstallmentsForLoan(loanId: Long): List<InstallmentEntity>

    @Query("DELETE FROM installments WHERE loanId = :loanId")
    suspend fun deleteInstallmentsByLoan(loanId: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    fun getPaymentsByLoan(loanId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDate DESC LIMIT 10")
    suspend fun getRecentPaymentsByLoanSync(loanId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    suspend fun getPaymentsByLoanSync(loanId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE paymentDate >= :startOfDayMs AND paymentDate <= :endOfDayMs ORDER BY paymentDate DESC")
    fun getPaymentsForDay(startOfDayMs: Long, endOfDayMs: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT SUM(amount) FROM payments WHERE method = 'EFECTIVO' AND paymentDate >= :startMs AND paymentDate <= :endMs")
    fun getCashPaymentsSum(startMs: Long, endMs: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE method = 'TRANSFERENCIA' AND paymentDate >= :startMs AND paymentDate <= :endMs")
    fun getTransferPaymentsSum(startMs: Long, endMs: Long): Flow<Double?>
}

@Dao
interface CashRegisterDao {
    @Query("SELECT * FROM cash_registers ORDER BY id DESC")
    fun getAllCashRegisters(): Flow<List<CashRegisterEntity>>

    @Query("SELECT * FROM cash_registers WHERE status = 'ABIERTA' ORDER BY id DESC LIMIT 1")
    fun getActiveCashRegister(): Flow<CashRegisterEntity?>

    @Query("SELECT * FROM cash_registers WHERE status = 'ABIERTA' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveCashRegisterSync(): CashRegisterEntity?

    @Query("SELECT * FROM cash_registers WHERE collectorId = :collectorId AND status = 'ABIERTA' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveCashRegisterForCollectorSync(collectorId: Long): CashRegisterEntity?

    @Query("SELECT * FROM cash_registers WHERE collectorId = :collectorId AND status = 'ABIERTA' ORDER BY id DESC LIMIT 1")
    fun getActiveCashRegisterForCollectorFlow(collectorId: Long): kotlinx.coroutines.flow.Flow<CashRegisterEntity?>

    @Query("SELECT * FROM cash_registers WHERE id = :id")
    suspend fun getCashRegisterById(id: Long): CashRegisterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashRegister(cashRegister: CashRegisterEntity): Long

    @Update
    suspend fun updateCashRegister(cashRegister: CashRegisterEntity)
}

@Dao
interface CashMovementDao {
    @Query("SELECT * FROM cash_movements WHERE cashRegisterId = :registerId ORDER BY timestamp DESC")
    fun getMovementsForRegister(registerId: Long): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<CashMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovement(movement: CashMovementEntity): Long
}

@Dao
interface CollectionVisitDao {
    @Query("SELECT * FROM collection_visits ORDER BY visitDate DESC")
    fun getAllVisits(): Flow<List<CollectionVisitEntity>>

    @Query("SELECT * FROM collection_visits WHERE clientId = :clientId ORDER BY visitDate DESC")
    fun getVisitsByClient(clientId: Long): Flow<List<CollectionVisitEntity>>

    @Query("SELECT * FROM collection_visits WHERE syncStatus = 'PENDIENTE'")
    suspend fun getPendingSyncVisits(): List<CollectionVisitEntity>

    @Query("UPDATE collection_visits SET syncStatus = 'SINCRONIZADO' WHERE id = :id")
    suspend fun markVisitSynced(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: CollectionVisitEntity): Long
}

@Dao
interface CollectionRouteDao {
    @Query("SELECT * FROM collection_routes ORDER BY name ASC")
    fun getAllRoutes(): Flow<List<CollectionRouteEntity>>

    @Query("SELECT * FROM collection_routes WHERE id = :id")
    suspend fun getRouteById(id: Long): CollectionRouteEntity?

    @Query("SELECT * FROM collection_routes WHERE collectorId = :collectorId AND isActive = 1")
    fun getActiveRoutesForCollector(collectorId: Long): Flow<List<CollectionRouteEntity>>

    @Query("SELECT * FROM collection_routes WHERE zone = :zone AND isActive = 1")
    fun getRoutesByZone(zone: String): Flow<List<CollectionRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRoute(route: CollectionRouteEntity): Long

    @Delete
    suspend fun deleteRoute(route: CollectionRouteEntity)
}

@Dao
interface RouteClientAssignmentDao {
    @Query("SELECT * FROM route_client_assignments WHERE routeId = :routeId ORDER BY orderIndex ASC")
    fun getAssignmentsForRoute(routeId: Long): Flow<List<RouteClientAssignmentEntity>>

    @Query("SELECT * FROM route_client_assignments WHERE routeId = :routeId ORDER BY orderIndex ASC")
    suspend fun getAssignmentsForRouteSync(routeId: Long): List<RouteClientAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: RouteClientAssignmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<RouteClientAssignmentEntity>)

    @Query("DELETE FROM route_client_assignments WHERE routeId = :routeId")
    suspend fun clearAssignmentsForRoute(routeId: Long)

    @Query("DELETE FROM route_client_assignments WHERE routeId = :routeId AND clientId = :clientId")
    suspend fun removeClientFromRoute(routeId: Long, clientId: Long)
}

@Dao
interface GpsTrackLogDao {
    @Query("SELECT * FROM gps_track_logs ORDER BY timestamp DESC")
    fun getAllGpsTrackLogs(): Flow<List<GpsTrackLogEntity>>

    @Query("SELECT * FROM gps_track_logs WHERE collectorId = :collectorId ORDER BY timestamp DESC")
    fun getGpsTrackLogsForCollector(collectorId: Long): Flow<List<GpsTrackLogEntity>>

    @Query("SELECT * FROM gps_track_logs WHERE syncStatus = 'PENDIENTE'")
    suspend fun getPendingGpsLogs(): List<GpsTrackLogEntity>

    @Query("UPDATE gps_track_logs SET syncStatus = 'SINCRONIZADO' WHERE id = :id")
    suspend fun markGpsLogSynced(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsLog(log: GpsTrackLogEntity): Long
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}

@Dao
interface SystemConfigDao {
    @Query("SELECT * FROM system_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<SystemConfigEntity?>

    @Query("SELECT * FROM system_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): SystemConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: SystemConfigEntity)
}

@Dao
interface LateFeeHistoryDao {
    @Query("SELECT * FROM late_fee_history WHERE loanId = :loanId ORDER BY appliedDate DESC")
    fun getLateFeeHistoryForLoan(loanId: Long): Flow<List<LateFeeHistoryEntity>>

    @Query("SELECT * FROM late_fee_history WHERE loanId = :loanId ORDER BY appliedDate DESC")
    suspend fun getLateFeeHistoryForLoanSync(loanId: Long): List<LateFeeHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLateFeeHistory(entry: LateFeeHistoryEntity): Long

    @Query("UPDATE late_fee_history SET status = 'LIQUIDADA' WHERE installmentId = :installmentId AND status = 'ACTIVA'")
    suspend fun markLateFeeLiquidated(installmentId: Long)
}

@Dao
interface SkippedPaymentLogDao {
    @Query("SELECT * FROM skipped_payments_log WHERE loanId = :loanId ORDER BY date DESC")
    fun getSkippedPaymentsForLoan(loanId: Long): Flow<List<SkippedPaymentLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkippedPaymentLog(log: SkippedPaymentLogEntity): Long
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM internal_notifications WHERE userId = :userId OR userId = -1 ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM internal_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM internal_notifications WHERE (userId = :userId OR userId = -1) AND isRead = 0")
    fun getUnreadCountForUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM internal_notifications WHERE isRead = 0")
    fun getTotalUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE internal_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE internal_notifications SET isRead = 1 WHERE userId = :userId OR userId = -1")
    suspend fun markAllAsReadForUser(userId: Long)

    @Query("DELETE FROM internal_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM internal_notifications")
    suspend fun clearAllNotifications()
}

@Dao
interface MessageTemplateDao {
    @Query("SELECT * FROM message_templates ORDER BY code ASC")
    fun getAllTemplates(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE code = :code LIMIT 1")
    suspend fun getTemplateByCode(code: String): MessageTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTemplate(template: MessageTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTemplates(templates: List<MessageTemplateEntity>)
}

@Dao
interface ReminderConfigDao {
    @Query("SELECT * FROM reminder_configs WHERE id = 1 LIMIT 1")
    fun getReminderConfig(): Flow<ReminderConfigEntity?>

    @Query("SELECT * FROM reminder_configs WHERE id = 1 LIMIT 1")
    suspend fun getReminderConfigSync(): ReminderConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: ReminderConfigEntity)
}

@Dao
interface CommunicationLogDao {
    @Query("SELECT * FROM communication_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CommunicationLogEntity>>

    @Query("SELECT * FROM communication_logs WHERE clientId = :clientId ORDER BY timestamp DESC")
    fun getLogsByClient(clientId: Long): Flow<List<CommunicationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CommunicationLogEntity): Long
}

@Dao
interface NotificationPreferenceDao {
    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    fun getPreferencesForUser(userId: Long): Flow<NotificationPreferenceEntity?>

    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getPreferencesForUserSync(userId: Long): NotificationPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePreferences(pref: NotificationPreferenceEntity)
}

@Dao
interface FcmDeviceRegistrationDao {
    @Query("SELECT * FROM fcm_device_registrations WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveRegistrationsForUser(userId: Long): List<FcmDeviceRegistrationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: FcmDeviceRegistrationEntity): Long

    @Query("UPDATE fcm_device_registrations SET isActive = 0 WHERE fcmToken = :token")
    suspend fun deactivateToken(token: String)
}

