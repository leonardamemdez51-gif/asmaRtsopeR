package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        BranchEntity::class,
        FundEntity::class,
        ClientEntity::class,
        LoanEntity::class,
        InstallmentEntity::class,
        LateFeeHistoryEntity::class,
        SkippedPaymentLogEntity::class,
        PaymentEntity::class,
        CashRegisterEntity::class,
        CashMovementEntity::class,
        CollectionVisitEntity::class,
        AuditLogEntity::class,
        SystemConfigEntity::class,
        NotificationEntity::class,
        MessageTemplateEntity::class,
        ReminderConfigEntity::class,
        CommunicationLogEntity::class,
        NotificationPreferenceEntity::class,
        FcmDeviceRegistrationEntity::class,
        CollectionRouteEntity::class,
        RouteClientAssignmentEntity::class,
        GpsTrackLogEntity::class,
        DocumentTypeConfigEntity::class,
        ClientDocumentEntity::class,
        DocumentAuditLogEntity::class,
        DocumentSyncQueueEntity::class,
        ClientEvaluationEntity::class,
        ManualScoreAdjustmentEntity::class,
        ClientEvaluationConfigEntity::class,
        ClientReferenceEntity::class,
        ClientObservationEntity::class,
        ClientAlertEntity::class
    ],
    version = 20,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {


    abstract fun userDao(): UserDao
    abstract fun clientDao(): ClientDao
    abstract fun loanDao(): LoanDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun lateFeeHistoryDao(): LateFeeHistoryDao
    abstract fun skippedPaymentLogDao(): SkippedPaymentLogDao
    abstract fun paymentDao(): PaymentDao
    abstract fun cashRegisterDao(): CashRegisterDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun collectionVisitDao(): CollectionVisitDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun systemConfigDao(): SystemConfigDao
    abstract fun notificationDao(): NotificationDao
    abstract fun messageTemplateDao(): MessageTemplateDao
    abstract fun reminderConfigDao(): ReminderConfigDao
    abstract fun communicationLogDao(): CommunicationLogDao
    abstract fun notificationPreferenceDao(): NotificationPreferenceDao
    abstract fun fcmDeviceRegistrationDao(): FcmDeviceRegistrationDao
    abstract fun collectionRouteDao(): CollectionRouteDao
    abstract fun routeClientAssignmentDao(): RouteClientAssignmentDao
    abstract fun gpsTrackLogDao(): GpsTrackLogDao
    abstract fun documentDao(): DocumentDao
    abstract fun evaluationDao(): ClientEvaluationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_15_17 = object : androidx.room.migration.Migration(15, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Empty migration just to update the hash
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Empty migration just to update the hash
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rama_erp_database"
                )
                .addMigrations(MIGRATION_15_17, MIGRATION_16_17)
                .fallbackToDestructiveMigration() // Activado temporalmente por error en entorno dev local
                .addCallback(DatabaseCallback(context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    seedDatabase(database)
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    if (database.userDao().getUserByUsername("admin") == null) {
                        seedDatabase(database)
                    }
                }
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            // Seed system configuration
            db.systemConfigDao().insertOrUpdateConfig(SystemConfigEntity())
            db.evaluationDao().insertOrUpdateConfig(ClientEvaluationConfigEntity())

            // Seed default users
            val adminUser = UserEntity(
                username = "admin",
                passwordHash = com.example.core.security.SecurityUtils.hashPassword("admin123"),
                fullName = "Carlos Mendoza (Admin)",
                role = "ADMINISTRADOR",
                phone = "555-100-2000",
                email = "admin@rama.com",
                assignedZone = "Zona Centro"
            )
            val collectorUser = UserEntity(
                username = "cobrador1",
                passwordHash = com.example.core.security.SecurityUtils.hashPassword("cobrador123"),
                fullName = "Roberto Gómez",
                role = "COBRADOR",
                phone = "555-300-4000",
                email = "roberto@rama.com",
                assignedZone = "Zona Norte"
            )
            val supervisorUser = UserEntity(
                username = "supervisor1",
                passwordHash = com.example.core.security.SecurityUtils.hashPassword("super123"),
                fullName = "Lucía Ramírez",
                role = "SUPERVISOR",
                phone = "555-500-6000",
                email = "lucia@rama.com",
                assignedZone = "Zona Centro"
            )
            val consultaUser = UserEntity(
                username = "consulta1",
                passwordHash = com.example.core.security.SecurityUtils.hashPassword("consulta123"),
                fullName = "Mariana Silva",
                role = "CONSULTA",
                phone = "555-700-8000",
                email = "mariana@rama.com",
                assignedZone = "Zona Sur"
            )

            db.userDao().insertUser(adminUser)
            val collectorId = db.userDao().insertUser(collectorUser)
            db.userDao().insertUser(supervisorUser)
            db.userDao().insertUser(consultaUser)

            // Seed initial sample clients
            val client1 = ClientEntity(
                fullName = "Maria Elena Torres",
                curp = "TOEM850315MDFR01",
                ine = "1234567890123",
                rfc = "TOEM8503159A1",
                birthDate = "1985-03-15",
                phone = "555-987-6543",
                email = "maria.torres@email.com",
                address = "Av. Revolución 452, Col. San Ángel",
                zone = "Zona Centro",
                latitude = 19.3482,
                longitude = -99.1872,
                referenceName1 = "Juan Torres (Hermano)",
                referencePhone1 = "555-111-2233",
                referenceRelation1 = "Hermano",
                status = "ACTIVO",
                punctualityScore = 95
            )
            val client2 = ClientEntity(
                fullName = "Jorge Luis Hernández",
                curp = "HELJ900720MDFR02",
                ine = "9876543210987",
                rfc = "HELJ9007208B2",
                birthDate = "1990-07-20",
                phone = "555-456-7890",
                email = "jorge.hernandez@email.com",
                address = "Calle Insurgentes Sur 120, Col. Roma",
                zone = "Zona Norte",
                latitude = 19.4123,
                longitude = -99.1654,
                referenceName1 = "Rosa María López",
                referencePhone1 = "555-444-5566",
                referenceRelation1 = "Esposa",
                status = "ACTIVO",
                punctualityScore = 88
            )
            val client3 = ClientEntity(
                fullName = "Ana Sofia Morales",
                curp = "MORA931105MDFR03",
                ine = "4567891230456",
                birthDate = "1993-11-05",
                phone = "555-654-3210",
                email = "ana.morales@email.com",
                address = "Calle Hidalgo 89, Col. Del Carmen",
                zone = "Zona Sur",
                latitude = 19.3521,
                longitude = -99.1620,
                status = "ACTIVO",
                punctualityScore = 100
            )

            val clientId1 = db.clientDao().insertClient(client1)
            val clientId2 = db.clientDao().insertClient(client2)
            db.clientDao().insertClient(client3)

            // Seed initial sample loan for client 1 (Plan 20 días: $5,000 capital -> $6,000 total, $300 daily)
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val loan1 = LoanEntity(
                clientId = clientId1,
                clientName = client1.fullName,
                clientCurp = client1.curp,
                collectorId = collectorId,
                collectorName = "Roberto Gómez",
                planType = "PLAN_20_DIAS",
                capital = 5000.0,
                interestRate = 0.20,
                interestAmount = 1000.0,
                totalAmount = 6000.0,
                dailyPayment = 300.0,
                paidAmount = 1200.0,
                remainingBalance = 4800.0,
                skipSundays = true,
                disbursementDate = now - (5 * dayMs),
                startDate = now - (4 * dayMs),
                expectedEndDate = now + (18 * dayMs),
                status = "ACTIVO"
            )
            val loanId1 = db.loanDao().insertLoan(loan1)

            // Generate amortization installments for loan 1
            val installmentsList = mutableListOf<InstallmentEntity>()
            var currentDate = now - (4 * dayMs)
            for (i in 1..20) {
                // Format YYYY-MM-DD
                val javaDate = java.util.Date(currentDate)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val dateStr = sdf.format(javaDate)

                val isPaid = i <= 4
                val paidAmt = if (isPaid) 300.0 else 0.0
                val remAmt = 300.0 - paidAmt
                val statusStr = if (isPaid) "PAGADO" else if (currentDate < now) "VENCIDO" else "PENDIENTE"

                installmentsList.add(
                    InstallmentEntity(
                        loanId = loanId1,
                        clientId = clientId1,
                        installmentNumber = i,
                        dueDate = currentDate,
                        dueDateFormatted = dateStr,
                        capitalComponent = 250.0,
                        interestComponent = 50.0,
                        targetAmount = 300.0,
                        paidAmount = paidAmt,
                        remainingAmount = remAmt,
                        lateFee = if (statusStr == "VENCIDO") 15.0 else 0.0,
                        status = statusStr
                    )
                )
                currentDate += dayMs
            }
            db.installmentDao().insertInstallments(installmentsList)

            // Seed an active Cash Register
            val cashRegister = CashRegisterEntity(
                collectorId = collectorId,
                collectorName = "Roberto Gómez",
                openDate = now - (8 * 3600000L),
                initialCash = 2000.0,
                cashInflows = 1200.0,
                transferInflows = 300.0,
                outflows = 0.0,
                expectedCash = 3200.0,
                status = "ABIERTA",
                notes = "Apertura de turno matutino"
            )
            val regId = db.cashRegisterDao().insertCashRegister(cashRegister)

            db.cashMovementDao().insertCashMovement(
                CashMovementEntity(
                    cashRegisterId = regId,
                    type = "INGRESO_COBRO_EFECTIVO",
                    amount = 1200.0,
                    concept = "Cobro cuotas 1-4 Préstamo #$loanId1 Maria Elena Torres",
                    registeredBy = "Roberto Gómez"
                )
            )

            // Seed initial audit log
            db.auditLogDao().insertAuditLog(
                AuditLogEntity(
                    userId = 1,
                    username = "admin",
                    action = "INICIALIZACION_SISTEMA",
                    entityType = "SISTEMA",
                    entityId = "1",
                    ipDevice = "Android Enterprise Terminal",
                    newValues = "Base de datos inicializada con parámetros de microfinanzas RAMA"
                )
            )

            // Seed default message templates
            val defaultTemplates = listOf(
                MessageTemplateEntity(
                    code = "PAGO_PROXIMO",
                    name = "Recordatorio de Pago Próximo",
                    category = "RECORDATORIO",
                    templateText = "Hola {nombre_cliente}, le recordamos que su próximo pago de ${'$'}{cuota} para el préstamo #{numero_prestamo} vence el {fecha_pago}. Saldo pendiente: ${'$'}{saldo}. ¡Gracias por su puntualidad!"
                ),
                MessageTemplateEntity(
                    code = "PAGO_HOY",
                    name = "Pago Correspondiente Hoy",
                    category = "RECORDATORIO",
                    templateText = "Estimado(a) {nombre_cliente}, hoy es la fecha límite para su pago de ${'$'}{cuota} (Préstamo #{numero_prestamo}). Cobrador asignado: {nombre_cobrador}. Teléfono de contacto: {telefono_empresa}."
                ),
                MessageTemplateEntity(
                    code = "PAGO_VENCIDO",
                    name = "Aviso de Pago Vencido",
                    category = "COBRANZA",
                    templateText = "Aviso de Pago Vencido: {nombre_cliente}, su cuota de ${'$'}{cuota} con fecha {fecha_pago} presenta {dias_atraso} día(s) de atraso. Le solicitamos regularizar su saldo de ${'$'}{saldo} a la brevedad."
                ),
                MessageTemplateEntity(
                    code = "MORA",
                    name = "Alerta de Mora Operativa",
                    category = "COBRANZA",
                    templateText = "URGENTE - Alerta de Mora: {nombre_cliente}, su préstamo #{numero_prestamo} presenta adeudo con {dias_atraso} días de mora. Saldo total: ${'$'}{saldo}. Evite cargos adicionales abonando hoy."
                ),
                MessageTemplateEntity(
                    code = "CONFIRMACION_PAGO",
                    name = "Confirmación de Pago Recibido",
                    category = "CONFIRMACION",
                    templateText = "Confirmación de Pago: {nombre_cliente}, hemos registrado exitosamente su pago de ${'$'}{monto} para el préstamo #{numero_prestamo}. Saldo restante: ${'$'}{saldo}. ¡Gracias por su pago!"
                ),
                MessageTemplateEntity(
                    code = "PAGO_PARCIAL",
                    name = "Notificación de Pago Parcial",
                    category = "CONFIRMACION",
                    templateText = "Estimado(a) {nombre_cliente}, recibimos su abono parcial de ${'$'}{monto}. Su saldo restante actualizado para el préstamo #{numero_prestamo} es de ${'$'}{saldo}."
                ),
                MessageTemplateEntity(
                    code = "PROMESA_PAGO",
                    name = "Recordatorio de Promesa de Pago",
                    category = "RECORDATORIO",
                    templateText = "Recordatorio de Promesa: {nombre_cliente}, tenemos registrada su promesa de pago por ${'$'}{monto} para la fecha {fecha_pago}. Quedamos atentos a su cumplimiento."
                ),
                MessageTemplateEntity(
                    code = "RENOVACION_DISPONIBLE",
                    name = "Oferta de Renovación de Préstamo",
                    category = "ADMINISTRATIVO",
                    templateText = "¡Felicidades {nombre_cliente}! Cuenta con una oferta de renovación disponible por su excelente historial. Consulte con su cobrador {nombre_cobrador} al {telefono_empresa}."
                ),
                MessageTemplateEntity(
                    code = "AVISO_ADMINISTRATIVO",
                    name = "Aviso Administrativo General",
                    category = "ADMINISTRATIVO",
                    templateText = "Aviso Oficial de RAMA Microfinanzas: Estimado(a) {nombre_cliente}, {monto}. Para dudas comuníquese al {telefono_empresa}."
                )
            )
            db.messageTemplateDao().insertAllTemplates(defaultTemplates)

            // Seed default reminder config
            db.reminderConfigDao().insertOrUpdateConfig(ReminderConfigEntity())

            // Seed initial internal notifications
            db.notificationDao().insertNotification(
                NotificationEntity(
                    userId = 1,
                    username = "admin",
                    type = "AVISO_ADMINISTRATIVO",
                    title = "Bienvenido al Sistema de Comunicaciones",
                    message = "El módulo de Notificaciones, Plantillas de WhatsApp y Recordatorios ha sido activado exitosamente.",
                    priority = "BAJA",
                    navigationRoute = "notification_center"
                )
            )

            db.notificationDao().insertNotification(
                NotificationEntity(
                    userId = 1,
                    username = "admin",
                    type = "ALERTA_CAJA",
                    title = "Apertura de Caja Detectada",
                    message = "Se ha abierto el turno de caja #1 con un fondo inicial de $2,000 MXN para el cobrador Roberto Gómez.",
                    priority = "MEDIA",
                    navigationRoute = "cash_register"
                )
            )

            // Seed default Collection Route & assignments
            val sampleRoute = CollectionRouteEntity(
                code = "RUT-01",
                name = "Ruta 01 - Centro Histórico",
                branchName = "Sucursal Central",
                supervisorName = "Lucía Ramírez",
                collectorId = collectorId,
                collectorName = "Roberto Gómez",
                zone = "Zona Centro",
                isActive = true,
                totalClientsCount = 3,
                notes = "Ruta principal de cobro matutino en el centro de la ciudad"
            )
            val routeId = db.collectionRouteDao().insertOrUpdateRoute(sampleRoute)

            db.routeClientAssignmentDao().insertAssignments(
                listOf(
                    RouteClientAssignmentEntity(routeId = routeId, clientId = clientId1, orderIndex = 1, visitFrequency = "DIARIO", estimatedMinutes = 15),
                    RouteClientAssignmentEntity(routeId = routeId, clientId = clientId2, orderIndex = 2, visitFrequency = "DIARIO", estimatedMinutes = 20),
                    RouteClientAssignmentEntity(routeId = routeId, clientId = 3, orderIndex = 3, visitFrequency = "LUN_MIE_VIE", estimatedMinutes = 15)
                )
            )

            // Seed sample GPS track log
            db.gpsTrackLogDao().insertGpsLog(
                GpsTrackLogEntity(
                    collectorId = collectorId,
                    collectorName = "Roberto Gómez",
                    latitude = 19.4326,
                    longitude = -99.1332,
                    accuracyMeters = 4.2f,
                    activityType = "INICIO_JORNADA",
                    clientName = "Sucursal Central",
                    notes = "Inicio de ruta de cobranza"
                )
            )

             // Seed default document type configurations
             val defaultDocTypes = listOf(
                 DocumentTypeConfigEntity(
                     code = "FOTO_PERSONAL",
                     name = "Fotografía Personal",
                     description = "Foto clara del rostro del cliente",
                     isRequiredForClient = false,
                     isRequiredForLoan = false,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = false,
                     allowMultiple = false,
                     iconName = "AccountBox",
                     sortOrder = 1,
                     requiresValidation = false,
                     allowsPhoto = true,
                     allowsFile = true,
                     hasValidityLimit = false,
                     blocksOperationsIfMissing = false
                 ),
                 DocumentTypeConfigEntity(
                     code = "INE_FRENTE",
                     name = "INE Frente",
                     description = "Identificación oficial oficial frente",
                     isRequiredForClient = true,
                     isRequiredForLoan = true,
                     isRequiredForRenewal = true,
                     isRequiredForCollection = false,
                     allowMultiple = false,
                     iconName = "Badge",
                     sortOrder = 2,
                     requiresValidation = true,
                     allowsPhoto = true,
                     allowsFile = true,
                     hasValidityLimit = true,
                     blocksOperationsIfMissing = true
                 ),
                 DocumentTypeConfigEntity(
                     code = "INE_REVERSO",
                     name = "INE Reverso",
                     description = "Identificación oficial reverso con huella/firma",
                     isRequiredForClient = true,
                     isRequiredForLoan = true,
                     isRequiredForRenewal = true,
                     isRequiredForCollection = false,
                     allowMultiple = false,
                     iconName = "Badge",
                     sortOrder = 3,
                     requiresValidation = true,
                     allowsPhoto = true,
                     allowsFile = true,
                     hasValidityLimit = true,
                     blocksOperationsIfMissing = true
                 ),
                 DocumentTypeConfigEntity(
                     code = "CURP",
                     name = "CURP",
                     description = "Constancia oficial de CURP",
                     isRequiredForClient = false,
                     isRequiredForLoan = false,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = false,
                     allowMultiple = false,
                     iconName = "Description",
                     sortOrder = 4,
                     requiresValidation = true,
                     allowsPhoto = true,
                     allowsFile = true,
                     hasValidityLimit = false,
                     blocksOperationsIfMissing = false
                 ),
                 DocumentTypeConfigEntity(
                     code = "COMPROBANTE_DOMICILIO",
                     name = "Comprobante de Domicilio",
                     description = "Luz, agua, predial o teléfono no mayor a 3 meses",
                     isRequiredForClient = true,
                     isRequiredForLoan = true,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = false,
                     allowMultiple = false,
                     iconName = "Receipt",
                     sortOrder = 5,
                     requiresValidation = true,
                     allowsPhoto = true,
                     allowsFile = true,
                     hasValidityLimit = true,
                     blocksOperationsIfMissing = false
                 ),
                 DocumentTypeConfigEntity(
                     code = "EVIDENCIA_DOMICILIO",
                     name = "Evidencia de Domicilio",
                     description = "Fotografía fachada exterior de la vivienda con número",
                     isRequiredForClient = false,
                     isRequiredForLoan = false,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = false,
                     allowMultiple = true,
                     iconName = "House",
                     sortOrder = 6,
                     requiresValidation = false,
                     allowsPhoto = true,
                     allowsFile = false,
                     hasValidityLimit = false,
                     blocksOperationsIfMissing = false
                 ),
                 DocumentTypeConfigEntity(
                     code = "EVIDENCIA_NEGOCIO",
                     name = "Evidencia de Negocio",
                     description = "Fotografía del establecimiento comercial o puesto del cliente",
                     isRequiredForClient = false,
                     isRequiredForLoan = false,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = false,
                     allowMultiple = true,
                     iconName = "Store",
                     sortOrder = 7,
                     requiresValidation = false,
                     allowsPhoto = true,
                     allowsFile = false,
                     hasValidityLimit = false,
                     blocksOperationsIfMissing = false
                 ),
                 DocumentTypeConfigEntity(
                     code = "EVIDENCIA_VISITA",
                     name = "Evidencia de Visita",
                     description = "Fotografía geolocalizada en visita de cobro",
                     isRequiredForClient = false,
                     isRequiredForLoan = false,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = false,
                     allowMultiple = true,
                     iconName = "LocationOn",
                     sortOrder = 8,
                     requiresValidation = false,
                     allowsPhoto = true,
                     allowsFile = false,
                     hasValidityLimit = false,
                     blocksOperationsIfMissing = false
                 ),
                 DocumentTypeConfigEntity(
                     code = "COMPROBANTE_TRANSFERENCIA",
                     name = "Comprobante de Transferencia",
                     description = "Captura de pantalla o ficha de depósito/transferencia",
                     isRequiredForClient = false,
                     isRequiredForLoan = false,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = true,
                     allowMultiple = true,
                     iconName = "CreditCard",
                     sortOrder = 9,
                     requiresValidation = true,
                     allowsPhoto = true,
                     allowsFile = true,
                     hasValidityLimit = false,
                     blocksOperationsIfMissing = false
                 ),
                 DocumentTypeConfigEntity(
                     code = "FIRMA",
                     name = "Firma Digital",
                     description = "Firma autógrafa capturada en pantalla",
                     isRequiredForClient = true,
                     isRequiredForLoan = true,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = false,
                     allowMultiple = false,
                     iconName = "Draw",
                     sortOrder = 10,
                     requiresValidation = false,
                     allowsPhoto = true,
                     allowsFile = false,
                     hasValidityLimit = false,
                     blocksOperationsIfMissing = false
                 ),
                 DocumentTypeConfigEntity(
                     code = "DOCUMENTO_ADICIONAL",
                     name = "Documento Adicional",
                     description = "Garantías, escrituras o referencias extra",
                     isRequiredForClient = false,
                     isRequiredForLoan = false,
                     isRequiredForRenewal = false,
                     isRequiredForCollection = false,
                     allowMultiple = true,
                     iconName = "Folder",
                     sortOrder = 11,
                     requiresValidation = false,
                     allowsPhoto = true,
                     allowsFile = true,
                     hasValidityLimit = false,
                     blocksOperationsIfMissing = false
                 )
             )
            db.documentDao().insertDocumentTypes(defaultDocTypes)
        }
    }
}
