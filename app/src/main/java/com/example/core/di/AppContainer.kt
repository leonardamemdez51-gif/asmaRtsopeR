package com.example.core.di

import android.content.Context
import com.example.core.config.AppConfig
import com.example.core.config.AppEnvironment
import com.example.core.error.GlobalErrorHandler
import com.example.core.logger.AppLogger
import com.example.core.notifications.PushNotificationManager
import com.example.data.local.AppDatabase
import com.example.data.repository.*
import com.example.domain.repositories.*
import com.example.domain.usecases.*

interface IAppContainer {
    val database: AppDatabase
    val authRepository: IAuthRepository
    val clientRepository: IClientRepository
    val loanRepository: ILoanRepository
    val cashRepository: ICashRepository
    val collectionRepository: ICollectionRepository
    val routeRepository: IRouteRepository
    val auditRepository: IAuditRepository
    val configRepository: IConfigRepository
    val communicationRepository: ICommunicationRepository
    val documentRepository: DocumentRepository
    val evaluationRepository: EvaluationRepository

    val calculateLoanUseCase: CalculateLoanUseCase
    val createLoanUseCase: CreateLoanUseCase
    val editLoanUseCase: EditLoanUseCase
    val cancelLoanUseCase: CancelLoanUseCase
    val authorizeLoanUseCase: AuthorizeLoanUseCase
    val disburseLoanUseCase: DisburseLoanUseCase
    val renewLoanUseCase: RenewLoanUseCase
    val processPaymentUseCase: ProcessPaymentUseCase
    val scoreClientUseCase: ScoreClientUseCase
}

class AppContainer(context: Context) : IAppContainer {

    init {
        AppLogger.i("Initializing RAMA ERP AppContainer in environment: ${AppConfig.getActiveEnvironment().envName}")
        PushNotificationManager.init(context)
    }

    override val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val authRepository: IAuthRepository by lazy {
        AuthRepository(database.userDao(), database.auditLogDao())
    }

    override val clientRepository: IClientRepository by lazy {
        ClientRepository(database.clientDao(), database.auditLogDao())
    }

    override val loanRepository: ILoanRepository by lazy {
        LoanRepository(
            database,
            database.loanDao(),
            database.installmentDao(),
            database.lateFeeHistoryDao(),
            database.skippedPaymentLogDao(),
            database.paymentDao(),
            database.cashRegisterDao(),
            database.cashMovementDao(),
            database.clientDao(),
            database.auditLogDao(),
            database.systemConfigDao()
        )
    }

    override val cashRepository: ICashRepository by lazy {
        CashRepository(database.cashRegisterDao(), database.cashMovementDao(), database.auditLogDao())
    }

    override val collectionRepository: ICollectionRepository by lazy {
        CollectionRepository(
            database.collectionVisitDao(),
            database.gpsTrackLogDao(),
            database.auditLogDao()
        )
    }

    override val routeRepository: IRouteRepository by lazy {
        RouteRepository(
            database.collectionRouteDao(),
            database.routeClientAssignmentDao(),
            database.auditLogDao()
        )
    }

    override val auditRepository: IAuditRepository by lazy {
        AuditRepository(database.auditLogDao())
    }

    override val configRepository: IConfigRepository by lazy {
        ConfigRepository(database.systemConfigDao(), database.auditLogDao())
    }

    override val communicationRepository: ICommunicationRepository by lazy {
        CommunicationRepository(
            database.notificationDao(),
            database.messageTemplateDao(),
            database.reminderConfigDao(),
            database.communicationLogDao(),
            database.notificationPreferenceDao(),
            database.fcmDeviceRegistrationDao()
        )
    }

    override val documentRepository: DocumentRepository by lazy {
        DocumentRepository(database)
    }

    override val evaluationRepository: EvaluationRepository by lazy {
        EvaluationRepository(database.evaluationDao(), database.auditLogDao())
    }

    override val calculateLoanUseCase: CalculateLoanUseCase by lazy {
        CalculateLoanUseCase(configRepository)
    }

    override val createLoanUseCase: CreateLoanUseCase by lazy {
        CreateLoanUseCase(loanRepository, clientRepository)
    }

    override val editLoanUseCase: EditLoanUseCase by lazy {
        EditLoanUseCase(loanRepository)
    }

    override val cancelLoanUseCase: CancelLoanUseCase by lazy {
        CancelLoanUseCase(loanRepository)
    }

    override val authorizeLoanUseCase: AuthorizeLoanUseCase by lazy {
        AuthorizeLoanUseCase(loanRepository)
    }

    override val disburseLoanUseCase: DisburseLoanUseCase by lazy {
        DisburseLoanUseCase(loanRepository)
    }

    override val renewLoanUseCase: RenewLoanUseCase by lazy {
        RenewLoanUseCase(loanRepository)
    }

    override val processPaymentUseCase: ProcessPaymentUseCase by lazy {
        ProcessPaymentUseCase(loanRepository)
    }

    override val scoreClientUseCase: ScoreClientUseCase by lazy {
        ScoreClientUseCase()
    }
}
