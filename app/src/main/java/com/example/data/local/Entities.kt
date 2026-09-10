package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val lastName: String = "",
    val role: String, // "ADMINISTRADOR", "SUPERVISOR", "COBRADOR", "CONSULTA"
    val phone: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val active: Boolean = true,
    val assignedZone: String = "Zona Centro",
    val branch: String = "Sucursal Central",
    val route: String = "Ruta 01 - Centro",
    val fund: String = "Fondo Operativo General",
    val supervisorId: Long? = null,
    val supervisorName: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val notes: String = "",
    val failedAttempts: Int = 0,
    val lockedUntilMs: Long = 0L,
    val customPermissions: String = "", // Comma-separated AppPermission codes override
    val passwordResetToken: String? = null,
    val passwordResetExpiryMs: Long = 0L,
    val lastLoginMs: Long = 0L
)

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val active: Boolean = true
)

@Entity(tableName = "funds")
data class FundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double = 0.0,
    val active: Boolean = true
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folio: String = "", // CLI-000001
    val fullName: String,
    val firstName: String = "",
    val lastName: String = "",
    val maternalLastName: String = "",
    
    val curp: String,
    val ine: String,
    val rfc: String = "",
    val birthDate: String = "",
    val gender: String = "No especificado",
    val maritalStatus: String = "Soltero(a)",
    val nationality: String = "Mexicana",
    
    // Contact Info
    val phone: String = "",
    val secondaryPhone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val alternativeContactName: String = "",
    val alternativeContactRelation: String = "",
    val alternativeContactPhone: String = "",

    // Address
    val address: String = "", // Legacy full address
    val street: String = "",
    val exteriorNumber: String = "",
    val interiorNumber: String = "",
    val neighborhood: String = "", // Colonia
    val city: String = "", // Localidad
    val municipality: String = "", // Municipio
    val state: String = "", // Estado
    val zipCode: String = "",
    val addressReferences: String = "",
    val addressType: String = "PARTICULAR",
    val timeAtAddress: String = "",
    
    // Geographic Location
    val zone: String = "Zona Centro",
    val latitude: Double = 19.4326,
    val longitude: Double = -99.1332,
    val gpsAccuracy: Float = 5.0f,
    val locationCapturedAt: Long = System.currentTimeMillis(),
    val locationCapturedBy: String = "",
    val locationSource: String = "",
    val locationReference: String = "",

    // Economic Info
    val occupation: String = "Comerciante",
    val economicActivity: String = "",
    val monthlyIncome: Double = 12000.0,
    val incomeFrequency: String = "MENSUAL",
    val estimatedExpenses: Double = 0.0,
    val availableIncome: Double = 0.0,
    val businessType: String = "",
    val timeAtBusiness: String = "",
    val businessAddress: String = "",
    val employeesCount: Int = 0,
    
    // Operational Assignment
    val branch: String = "Sucursal Central",
    val supervisorId: Long? = null,
    val supervisorName: String = "",
    val collectorId: Long? = null,
    val collectorName: String = "",
    val route: String = "Ruta 01",
    val fund: String = "Fondo Operativo General",
    val assignmentStatus: String = "ASIGNADO",
    
    // Legacy References
    val referenceName1: String = "",
    val referencePhone1: String = "",
    val referenceRelation1: String = "",
    val referenceName2: String = "",
    val referencePhone2: String = "",
    val referenceRelation2: String = "",
    
    // Legacy Media
    val photoUri: String? = null,
    val ineFrontUri: String? = null,
    val ineBackUri: String? = null,
    val proofOfAddressUri: String? = null,
    val housePhotoUri: String? = null,
    val signatureUri: String? = null,

    // Status and tags
    val status: String = "ACTIVO", // "ACTIVO", "INACTIVO", "BLOQUEADO", "SUSPENDIDO", "PENDIENTE_VALIDACION"
    val blockReason: String = "",
    val blockedAt: Long? = null,
    val blockedBy: String = "",
    val tagsJson: String = "[]", // "CLIENTE PREMIUM", "ALTO RIESGO", etc
    
    // Scores
    val punctualityScore: Int = 100, // 0 to 100 score
    val riskProfile: String = "BAJO", // BAJO, MEDIO, ALTO, CRITICO
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val syncStatus: String = "SINCRONIZADO"
)

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"])]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folio: String = "",
    val clientId: Long,
    val clientName: String,
    val clientCurp: String,
    val collectorId: Long = 1,
    val collectorName: String = "Cobrador Principal",
    val planType: String, // "PLAN_20_DIAS", "PLAN_30_DIAS"
    val requestedAmount: Double = 0.0,
    val approvedAmount: Double = 0.0,
    val capital: Double, // Disbursed/Calculated Capital
    val interestRate: Double, // 0.20 or 0.30
    val interestAmount: Double,
    val totalAmount: Double,
    val dailyPayment: Double,
    val paidAmount: Double = 0.0,
    val remainingBalance: Double,
    val skipSundays: Boolean = true,
    val disbursementDate: Long = System.currentTimeMillis(),
    val startDate: Long = System.currentTimeMillis(),
    val expectedEndDate: Long = System.currentTimeMillis(),
    val status: String = "BORRADOR", // "BORRADOR", "SOLICITADO", "EN_EVALUACION", "PENDIENTE_AUTORIZACION", "APROBADO", "RECHAZADO", "DESEMBOLSADO", "ACTIVO", "ATRASADO", "LIQUIDADO", "RENOVADO", "CANCELADO"
    val isRenewal: Boolean = false,
    val previousLoanId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lateFeePercentage: Double = 0.05,
    val lateFeeType: String = "PORCENTAJE_CUOTA",
    val approvedByUserId: Long? = null,
    val approvedAt: Long? = null,
    val disbursedByUserId: Long? = null,
    val rejectionReason: String? = null,
    val cancellationReason: String? = null
)

@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loanId"]), Index(value = ["dueDate"])]
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val clientId: Long,
    val installmentNumber: Int,
    val dueDate: Long,
    val dueDateFormatted: String, // e.g. "2026-08-05"
    val capitalComponent: Double,
    val interestComponent: Double,
    val targetAmount: Double,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double,
    val lateFee: Double = 0.0, // Mora (5% or configured rate)
    val status: String = "PENDIENTE", // "PENDIENTE", "PAGADO", "PARCIAL", "VENCIDO", "MORA"
    val lastPaymentDate: Long? = null,
    val daysOverdue: Int = 0,
    val isSkipped: Boolean = false,
    val skippedReason: String = ""
)

@Entity(
    tableName = "late_fee_history",
    foreignKeys = [
        ForeignKey(
            entity = InstallmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["installmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["installmentId"]), Index(value = ["loanId"])]
)
data class LateFeeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val installmentId: Long,
    val installmentNumber: Int,
    val amountApplied: Double,
    val appliedDate: Long = System.currentTimeMillis(),
    val status: String = "ACTIVA", // "ACTIVA", "LIQUIDADA", "EXONERADA"
    val daysOverdueAtApplication: Int = 0,
    val notes: String = ""
)

@Entity(
    tableName = "skipped_payments_log",
    foreignKeys = [
        ForeignKey(
            entity = InstallmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["installmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["installmentId"]), Index(value = ["loanId"])]
)
data class SkippedPaymentLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val installmentId: Long,
    val installmentNumber: Int,
    val date: Long = System.currentTimeMillis(),
    val collectorName: String = "",
    val reason: String = "Pago omitido"
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val installmentId: Long? = null,
    val clientId: Long,
    val clientName: String,
    val collectorId: Long,
    val collectorName: String,
    val amount: Double,
    val method: String, // "EFECTIVO", "TRANSFERENCIA", "TARJETA", "DEPÓSITO", etc.
    val paymentDate: Long = System.currentTimeMillis(),
    val receiptNumber: String,
    val notes: String = "",
    val isAdvance: Boolean = false,
    val paymentType: String = "NORMAL", // "NORMAL", "PARCIAL", "ADELANTADO", "LIQUIDACION_TOTAL", "MULTI_CUOTA"
    val proofPhotoUri: String? = null,
    val installmentsCoveredCount: Int = 1
)

@Entity(tableName = "cash_registers")
data class CashRegisterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectorId: Long,
    val collectorName: String,
    val openDate: Long = System.currentTimeMillis(),
    val closeDate: Long? = null,
    val initialCash: Double,
    val cashInflows: Double = 0.0,
    val transferInflows: Double = 0.0,
    val outflows: Double = 0.0,
    val expectedCash: Double,
    val actualCash: Double? = null,
    val discrepancy: Double? = null,
    val status: String = "ABIERTA", // "ABIERTA", "CERRADA"
    val notes: String = "",
    val branchName: String = "Sucursal Central",
    val fundName: String = "Fondo Operativo General",
    val denominationBreakdownJson: String? = null,
    val auditStatus: String = "CORRECTO" // "CORRECTO", "FALTANTE", "SOBRANTE"
)

@Entity(tableName = "cash_movements")
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashRegisterId: Long,
    val type: String, // "INGRESO_COBRO_EFECTIVO", "INGRESO_COBRO_TRANSFERENCIA", "INGRESO_MANUAL", "EGRESO_PRESTAMO", "EGRESO_GASTO", "EGRESO_RETIRO", "AJUSTE_POSITIVO", "AJUSTE_NEGATIVO"
    val amount: Double,
    val concept: String,
    val timestamp: Long = System.currentTimeMillis(),
    val registeredBy: String,
    val paymentMethod: String = "EFECTIVO", // "EFECTIVO", "TRANSFERENCIA", "OTRO"
    val category: String = "GENERAL" // "COBRO", "GASTO", "APORTACION", "RETIRO", "AJUSTE"
)

@Entity(tableName = "collection_visits")
data class CollectionVisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val clientName: String,
    val loanId: Long,
    val collectorId: Long,
    val collectorName: String,
    val visitDate: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val gpsAccuracy: Float = 5.0f,
    val locationReference: String = "",
    val result: String, // "PAGO_REALIZADO", "PROMESA_PAGO", "NO_ENCONTRADO", "RECHAZO"
    val promisedPaymentDate: Long? = null,
    val promisedAmount: Double? = null,
    val notes: String = "",
    val photoUri: String? = null,
    val syncStatus: String = "SINCRONIZADO" // "SINCRONIZADO", "PENDIENTE"
)

@Entity(tableName = "collection_routes")
data class CollectionRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // e.g. "RUT-01"
    val name: String, // e.g. "Ruta Centro Comercial"
    val branchName: String = "Sucursal Central",
    val supervisorName: String = "Lucía Ramírez",
    val collectorId: Long? = 2L,
    val collectorName: String = "Roberto Gómez",
    val zone: String = "Zona Centro",
    val isActive: Boolean = true,
    val totalClientsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "route_client_assignments",
    foreignKeys = [
        ForeignKey(
            entity = CollectionRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["routeId"]), Index(value = ["clientId"])]
)
data class RouteClientAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val clientId: Long,
    val orderIndex: Int = 1,
    val visitFrequency: String = "DIARIO", // "DIARIO", "SEMANAL", "LUN_MIE_VIE"
    val estimatedMinutes: Int = 15,
    val notes: String = ""
)

@Entity(tableName = "gps_track_logs")
data class GpsTrackLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectorId: Long,
    val collectorName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 5.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val activityType: String = "VISITA", // "VISITA", "EN_RUTA", "ARQUEO_CAJA", "INICIO_JORNADA"
    val clientName: String = "",
    val syncStatus: String = "SINCRONIZADO", // "SINCRONIZADO", "PENDIENTE"
    val notes: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val username: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ipDevice: String = "Android Mobile Terminal",
    val previousValues: String? = null,
    val newValues: String? = null,
    val traceId: String = "TRX-2026-" + String.format(java.util.Locale.US, "%06d", (Math.abs(System.nanoTime()) % 1000000)),
    val userRole: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    val result: String = "EXITOSO",
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Entity(tableName = "system_config")
data class SystemConfigEntity(
    @PrimaryKey val id: Int = 1,
    // 1. Empresa
    val companyName: String = "RAMA Microfinanzas",
    val businessName: String = "Soluciones Financieras RAMA S.A. de C.V. SOFOM E.N.R.",
    val rfc: String = "SFR180412H89",
    val address: String = "Av. Reforma 222, Piso 10, Col. Juárez, CDMX",
    val phone: String = "55-8000-7262",
    val email: String = "contacto@ramafinanciera.com",
    val logoUrl: String = "https://rama-microfinanzas.com/assets/logo.png",
    val currency: String = "MXN ($)",
    val timeZone: String = "America/Mexico_City (GMT-6)",
    val dateFormat: String = "DD/MM/YYYY",
    val timeFormat: String = "24 horas (HH:mm)",

    // 2. Parámetros Generales
    val defaultZone: String = "Zona Centro",
    val systemLanguage: String = "Español (México)",
    val strictMode: Boolean = true,
    val autoSyncOffline: Boolean = true,
    val allowNegativeBalance: Boolean = false,
    val paymentMethodsCsv: String = "Efectivo,Transferencia,Tarjeta,Depósito",

    // 3. Planes de Préstamo
    val plan20InterestRate: Double = 0.20,
    val plan30InterestRate: Double = 0.30,
    val customLoanPlansJson: String = "[{\"code\":\"PLAN_20_DIAS\",\"name\":\"Plan 20 días\",\"days\":20,\"rate\":0.20},{\"code\":\"PLAN_30_DIAS\",\"name\":\"Plan 30 días\",\"days\":30,\"rate\":0.30}]",
    val paymentFrequency: String = "PAGO_DIARIO",
    val firstPaymentNextDay: Boolean = true,
    val allowSkipSundays: Boolean = true,
    val enableLateFees: Boolean = true,
    val lateFeePercentage: Double = 0.05,
    val maxOverdueDays: Int = 15,
    val allowRenewals: Boolean = true,
    val allowRenewalPlusLoan: Boolean = true,
    val allowMultipleLoans: Boolean = true,
    val maxLoanAmountStandard: Double = 10000.0,
    val maxLoanAmountSupervisor: Double = 25000.0,

    // 4. Intereses y Mora
    val lateFeeType: String = "PORCENTAJE_CUOTA",
    val graceDays: Int = 1,
    val interestCalculationMethod: String = "INTERES_SIMPLE",
    val penaltyCapPercentage: Double = 0.50,

    // 5. Usuarios y Seguridad
    val sessionTimeoutMinutes: Int = 30,
    val autoLockEnabled: Boolean = true,
    val maxLoginAttempts: Int = 3,
    val minPasswordLength: Int = 8,
    val requirePasswordComplexity: Boolean = true,

    // 6. Rutas y Sucursales
    val branchesListCsv: String = "Sucursal Central,Sucursal Norte,Sucursal Sur",
    val routesListCsv: String = "Ruta 01 - Centro,Ruta 02 - Mercado,Ruta 03 - Industrial,Ruta 04 - PeriSur",

    // 7. Fondos
    val fundsListCsv: String = "Fondo Operativo General,Fondo Caja Chica,Fondo Desembolsos Especiales",
    val defaultFund: String = "Fondo Operativo General",

    // 8. Notificaciones
    val enableWhatsAppAlerts: Boolean = true,
    val enablePushNotifications: Boolean = true,
    val enableEmailAlerts: Boolean = true,
    val alertTriggersCsv: String = "MORA_DETECTADA,APERTURA_CAJA,DESEMBOLSO_ALTO",

    // 9. WhatsApp
    val whatsappApiUrl: String = "https://api.whatsapp.com/v1/messages",
    val whatsappApiKey: String = "wa_live_key_998127391823719",
    val whatsappTemplatePaymentReceipt: String = "Hola {CLIENTE}, confirmamos tu pago de ${'$'}{MONTO} el {FECHA}. Folio: {FOLIO}. ¡Gracias!",
    val whatsappTemplateReminder: String = "Estimado(a) {CLIENTE}, le recordamos que hoy vence su cuota de ${'$'}{MONTO} para el préstamo #{PRESTAMO}.",
    val autoSendPaymentReceipt: Boolean = true,
    val allowWhatsAppReminder: Boolean = true,
    val allowWhatsAppPaymentReceipt: Boolean = true,
    val allowWhatsAppLateFee: Boolean = true,
    val allowWhatsAppPromise: Boolean = true,
    val allowWhatsAppRenewal: Boolean = true,
    val whatsappDefaultCountryCode: String = "52",
    val whatsappBusinessName: String = "RAMA Microfinanzas",
    val whatsappMessageSignature: String = "Atentamente, RAMA Microfinanzas.",
    val whatsappMinIntervalMinutes: Int = 5,

    // 10. Google Maps
    val googleMapsApiKey: String = "AIzaSyD_EXAMPLE_MAPS_KEY_RAMA",
    val enableGeofencing: Boolean = true,
    val gpsTrackingIntervalMinutes: Int = 5,
    val mapsEnabled: Boolean = true,
    val locationRecordingEnabled: Boolean = true,
    val clientLocationEnabled: Boolean = true,
    val visitLocationEnabled: Boolean = true,
    val minGpsAccuracyMeters: Float = 10.0f,
    val useExternalNavigation: Boolean = true,
    val gpsLocationHistoryEnabled: Boolean = true,
    val gpsPrivacyModeEnabled: Boolean = false,

    // 11. Respaldos
    val backupFrequency: String = "DIARIO",
    val backupRetentionDays: Int = 30,
    val backupLocation: String = "Nube Encriptada (AWS S3 / Cloud)",
    val lastBackupDateMs: Long = System.currentTimeMillis(),

    // 12. Auditoría
    val auditLogLevel: String = "COMPLETO",
    val auditLogRetentionDays: Int = 90,
    val logConfigChanges: Boolean = true,

    // 13. Numeraciones Automáticas
    val loanPrefix: String = "PRST-",
    val loanNextFolio: Long = 1001L,
    val receiptPrefix: String = "REC-",
    val receiptNextFolio: Long = 5001L,
    val clientPrefix: String = "CLI-",
    val clientNextFolio: Long = 2001L,
    val cashRegisterPrefix: String = "CAJ-",
    val cashRegisterNextFolio: Long = 3001L,

    // 14. Apariencia
    val themeMode: String = "SISTEMA",
    val primaryBrandColor: String = "#006874",
    val density: String = "CÓMODA",
    val fontSizeScale: Float = 1.0f
)

@Entity(tableName = "internal_notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = -1L, // -1L for global/all users, or specific user ID
    val username: String = "TODOS",
    val type: String, // "PAGO_PENDIENTE", "PAGO_VENCIDO", "PROMESA_PROXIMA", "PROMESA_INCUMPLIDA", "NUEVA_AUTORIZACION", "PRESTAMO_PENDIENTE_AUTORIZACION", "ALERTA_CAJA", "ERROR_SINCRONIZACION", "NUEVO_USUARIO", "AVISO_ADMINISTRATIVO"
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: String = "MEDIA", // "BAJA", "MEDIA", "ALTA", "URGENTE"
    val isRead: Boolean = false,
    val recipientRole: String = "TODOS", // "ADMINISTRATOR", "COBRADOR", "SUPERVISOR", "TODOS"
    val navigationRoute: String? = null,
    val relatedEntityId: String? = null
)

@Entity(tableName = "message_templates")
data class MessageTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // "PAGO_PROXIMO", "PAGO_HOY", "PAGO_VENCIDO", "MORA", "CONFIRMACION_PAGO", "PAGO_PARCIAL", "PROMESA_PAGO", "RENOVACION_DISPONIBLE", "AVISO_ADMINISTRATIVO"
    val name: String,
    val category: String = "COBRANZA", // "RECORDATORIO", "COBRANZA", "CONFIRMACION", "ADMINISTRATIVO"
    val templateText: String,
    val channel: String = "WHATSAPP", // "WHATSAPP", "SMS", "PUSH", "EMAIL"
    val enabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminder_configs")
data class ReminderConfigEntity(
    @PrimaryKey val id: Long = 1,
    val daysAdvanceNotice: Int = 1,
    val sendingTime: String = "09:00",
    val allowedDaysCsv: String = "LUNES,MARTES,MIERCOLES,JUEVES,VIERNES,SABADO",
    val targetClientTypesCsv: String = "TODOS,REGULAR,MOROSO",
    val targetLoanTypesCsv: String = "PLAN_20_DIAS,PLAN_30_DIAS",
    val moraTemplateCode: String = "MORA",
    val preventDuplicatesWithinHours: Int = 24
)

@Entity(tableName = "communication_logs")
data class CommunicationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val clientId: Long,
    val clientName: String,
    val clientPhone: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String, // "RECORDATORIO_PAGO", "PAGO_HOY", "MORA", "CONFIRMACION", "OTRO"
    val channel: String, // "WHATSAPP_APP", "WHATSAPP_API", "FCM", "INTERNO"
    val resultStatus: String, // "ABIERTO_EN_WHATSAPP", "ENVIADO_API", "INTENTO_REGISTRADO", "FALLIDO"
    val templateCodeUsed: String,
    val messageContent: String,
    val notes: String = ""
)

@Entity(tableName = "notification_preferences")
data class NotificationPreferenceEntity(
    @PrimaryKey val userId: Long,
    val enablePaymentsAlerts: Boolean = true,
    val enableOverdueAlerts: Boolean = true,
    val enablePromisesAlerts: Boolean = true,
    val enableAuthorizationsAlerts: Boolean = true,
    val enableCashAlerts: Boolean = true,
    val enableSyncAlerts: Boolean = true,
    val enableAdminAlerts: Boolean = true,
    val fcmToken: String = "",
    val fcmDeviceModel: String = "",
    val lastFcmUpdateMs: Long = 0L
)

@Entity(tableName = "fcm_device_registrations")
data class FcmDeviceRegistrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val fcmToken: String,
    val deviceId: String,
    val deviceModel: String,
    val registeredAtMs: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

