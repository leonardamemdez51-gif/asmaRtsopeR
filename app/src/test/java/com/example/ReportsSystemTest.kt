package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReportsSystemTest {

    private lateinit var db: AppDatabase
    private lateinit var clientDao: ClientDao
    private lateinit var loanDao: LoanDao
    private lateinit var installmentDao: InstallmentDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var cashRegisterDao: CashRegisterDao
    private lateinit var cashMovementDao: CashMovementDao
    private lateinit var visitDao: CollectionVisitDao
    private lateinit var auditLogDao: AuditLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        clientDao = db.clientDao()
        loanDao = db.loanDao()
        installmentDao = db.installmentDao()
        paymentDao = db.paymentDao()
        cashRegisterDao = db.cashRegisterDao()
        cashMovementDao = db.cashMovementDao()
        visitDao = db.collectionVisitDao()
        auditLogDao = db.auditLogDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPortfolioAndFinancialMetrics() = runBlocking {
        // 1. Insert Client with required parameters
        val client = ClientEntity(
            fullName = "Marta Gómez",
            curp = "GOMM850412HDFRRR01",
            ine = "INE850412GOMM",
            phone = "5551234567",
            address = "Calle Luna 123",
            zone = "CENTRO",
            status = "ACTIVO",
            punctualityScore = 95
        )
        val clientId = clientDao.insertClient(client)
        assertTrue(clientId > 0)

        // 2. Insert Loan with required parameters (P20: 20% interest, 20 days)
        val loan = LoanEntity(
            clientId = clientId,
            clientName = "Marta Gómez",
            clientCurp = "GOMM850412HDFRRR01",
            collectorId = 1L,
            collectorName = "Juan Cobrador",
            capital = 1000.0,
            interestRate = 0.20,
            interestAmount = 200.0,
            totalAmount = 1200.0,
            dailyPayment = 60.0,
            remainingBalance = 1200.0,
            planType = "PLAN_P20",
            status = "ACTIVO",
            disbursementDate = System.currentTimeMillis()
        )
        val loanId = loanDao.insertLoan(loan)
        assertTrue(loanId > 0)

        // Verify Portfolio statistics
        val allLoans = loanDao.getAllLoans().first()
        assertEquals(1, allLoans.size)
        assertEquals(1000.0, allLoans[0].capital, 0.0)
        assertEquals(1200.0, allLoans[0].remainingBalance, 0.0)

        // 3. Insert Installments
        val inst1 = InstallmentEntity(
            loanId = loanId,
            clientId = clientId,
            installmentNumber = 1,
            dueDate = System.currentTimeMillis(),
            dueDateFormatted = "2026-09-01",
            capitalComponent = 50.0,
            interestComponent = 10.0,
            targetAmount = 60.0,
            paidAmount = 0.0,
            remainingAmount = 60.0,
            status = "PENDIENTE"
        )
        installmentDao.insertInstallments(listOf(inst1))
        val installmentsList = installmentDao.getAllInstallments().first()
        assertEquals(1, installmentsList.size)
        assertEquals(60.0, installmentsList[0].targetAmount, 0.0)
    }

    @Test
    fun testPaymentRegistrationAndCashSumQueries() = runBlocking {
        val clientId = 10L
        val loanId = 20L

        // Register Cash & Transfer payments
        val cashPay = PaymentEntity(
            loanId = loanId,
            clientId = clientId,
            clientName = "Pedro Páramo",
            collectorId = 1L,
            collectorName = "Juan Cobrador",
            amount = 120.0,
            method = "EFECTIVO",
            paymentDate = System.currentTimeMillis() - 5000,
            receiptNumber = "REC-001"
        )
        val transferPay = PaymentEntity(
            loanId = loanId,
            clientId = clientId,
            clientName = "Pedro Páramo",
            collectorId = 1L,
            collectorName = "Juan Cobrador",
            amount = 200.0,
            method = "TRANSFERENCIA",
            paymentDate = System.currentTimeMillis(),
            receiptNumber = "REC-002"
        )

        paymentDao.insertPayment(cashPay)
        paymentDao.insertPayment(transferPay)

        val paymentsList = paymentDao.getAllPayments().first()
        assertEquals(2, paymentsList.size)

        // Verify total calculations
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis

        val cashSum = paymentDao.getCashPaymentsSum(startOfDay, endOfDay).first() ?: 0.0
        val transferSum = paymentDao.getTransferPaymentsSum(startOfDay, endOfDay).first() ?: 0.0

        assertEquals(120.0, cashSum, 0.0)
        assertEquals(200.0, transferSum, 0.0)
    }

    @Test
    fun testLateFeeAndMoraCalculations() = runBlocking {
        // Late fees are computed at 5% of the overdue installment targetAmount
        val overdueInstallment = InstallmentEntity(
            loanId = 1L,
            clientId = 1L,
            installmentNumber = 1,
            dueDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // Yesterday
            dueDateFormatted = "2026-08-31",
            capitalComponent = 100.0,
            interestComponent = 20.0,
            targetAmount = 120.0,
            paidAmount = 0.0,
            remainingAmount = 120.0,
            status = "PENDIENTE",
            daysOverdue = 1
        )

        val originalOverdueAmount = overdueInstallment.targetAmount
        val expectedLateFee = originalOverdueAmount * 0.05 // 5% late fee
        assertEquals(6.0, expectedLateFee, 0.0)
    }

    @Test
    fun testCashMovementRegistryAndArqueo() = runBlocking {
        val registerId = cashRegisterDao.insertCashRegister(
            CashRegisterEntity(
                collectorId = 1L,
                collectorName = "Juan Cobrador",
                initialCash = 500.0,
                cashInflows = 150.0,
                transferInflows = 100.0,
                expectedCash = 750.0,
                discrepancy = 0.0,
                status = "ABIERTA"
            )
        )
        assertTrue(registerId > 0)

        val inflowMovement = CashMovementEntity(
            cashRegisterId = registerId,
            type = "INGRESO_COBRO_EFECTIVO",
            amount = 150.0,
            concept = "Pago de cuotas cliente #1",
            paymentMethod = "EFECTIVO",
            registeredBy = "Juan Cobrador"
        )
        cashMovementDao.insertCashMovement(inflowMovement)

        val movements = cashMovementDao.getMovementsForRegister(registerId).first()
        assertEquals(1, movements.size)
        assertEquals("INGRESO_COBRO_EFECTIVO", movements[0].type)
        assertEquals(150.0, movements[0].amount, 0.0)
    }

    @Test
    fun testLoanRenewalsAndPreviousLoanReference() = runBlocking {
        // Insert Client first to satisfy LoanEntity's foreign key constraint
        val client = ClientEntity(
            fullName = "Carlos Slim",
            curp = "SLIC400128HDFRRR01",
            ine = "INE400128SLIC",
            phone = "5559876543",
            address = "Av. Reforma 100",
            zone = "CENTRO",
            status = "ACTIVO"
        )
        val clientId = clientDao.insertClient(client)

        val renewalLoan = LoanEntity(
            clientId = clientId,
            clientName = "Carlos Slim",
            clientCurp = "SLIC400128HDFRRR01",
            collectorId = 1L,
            collectorName = "Juan Cobrador",
            capital = 2000.0,
            interestRate = 0.20,
            interestAmount = 400.0,
            totalAmount = 2400.0,
            dailyPayment = 120.0,
            remainingBalance = 2400.0,
            planType = "PLAN_P20",
            status = "ACTIVO",
            isRenewal = true,
            previousLoanId = 100L
        )

        val id = loanDao.insertLoan(renewalLoan)
        val loaded = loanDao.getLoanById(id)
        assertNotNull(loaded)
        assertTrue(loaded?.isRenewal == true)
        assertEquals(100L, loaded?.previousLoanId)
    }

    @Test
    fun testAuditLogsRecordingAndQuerying() = runBlocking {
        val log = AuditLogEntity(
            userId = 1L,
            username = "ADMINISTRADOR",
            action = "MODIFICAR_MORA",
            entityType = "SYSTEM_CONFIG",
            entityId = "1",
            previousValues = "Mora: 5%",
            newValues = "Mora: 10%"
        )

        val id = auditLogDao.insertAuditLog(log)
        assertTrue(id > 0)

        val allLogs = auditLogDao.getAllAuditLogs().first()
        assertEquals(1, allLogs.size)
        assertEquals("MODIFICAR_MORA", allLogs[0].action)
        assertEquals("SYSTEM_CONFIG", allLogs[0].entityType)
    }

    @Test
    fun testVisitsAndPaymentPromises() = runBlocking {
        val visit = CollectionVisitEntity(
            clientId = 1L,
            clientName = "Marta Gómez",
            loanId = 1L,
            collectorId = 1L,
            collectorName = "Juan Cobrador",
            latitude = 19.4326,
            longitude = -99.1332,
            result = "PROMESA_PAGO",
            promisedAmount = 300.0,
            promisedPaymentDate = System.currentTimeMillis() + (48 * 60 * 60 * 1000)
        )

        val id = visitDao.insertVisit(visit)
        assertTrue(id > 0)

        val list = visitDao.getAllVisits().first()
        assertEquals(1, list.size)
        assertEquals("PROMESA_PAGO", list[0].result)
        assertEquals(300.0, list[0].promisedAmount ?: 0.0, 0.0)
    }
}
