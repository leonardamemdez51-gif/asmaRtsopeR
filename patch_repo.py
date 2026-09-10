import re

# 1. Patch Repositories.kt
with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    repo_content = f.read()

repo_content = repo_content.replace(
    '''class LoanRepository(
    private val loanDao: LoanDao,
    private val installmentDao: InstallmentDao,
    private val lateFeeHistoryDao: LateFeeHistoryDao,
    private val skippedPaymentLogDao: SkippedPaymentLogDao,
    private val paymentDao: PaymentDao,
    private val cashRegisterDao: CashRegisterDao,
    private val cashMovementDao: CashMovementDao,
    private val clientDao: ClientDao,
    private val auditLogDao: AuditLogDao,
    private val configDao: SystemConfigDao
) : ILoanRepository {''',
    '''import androidx.room.withTransaction
import com.example.data.local.AppDatabase

class LoanRepository(
    private val database: AppDatabase,
    private val loanDao: LoanDao,
    private val installmentDao: InstallmentDao,
    private val lateFeeHistoryDao: LateFeeHistoryDao,
    private val skippedPaymentLogDao: SkippedPaymentLogDao,
    private val paymentDao: PaymentDao,
    private val cashRegisterDao: CashRegisterDao,
    private val cashMovementDao: CashMovementDao,
    private val clientDao: ClientDao,
    private val auditLogDao: AuditLogDao,
    private val configDao: SystemConfigDao
) : ILoanRepository {'''
)

# registerSmartPayment wrapping
old_smart = '''    override suspend fun registerSmartPayment(
        loanId: Long,
        amount: Double,
        method: String,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        notes: String,
        paymentType: String,
        proofPhotoUri: String?,
        redistributeAdvance: Boolean,
        targetInstallmentIds: List<Long>
    ): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false'''

new_smart = '''    override suspend fun registerSmartPayment(
        loanId: Long,
        amount: Double,
        method: String,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        notes: String,
        paymentType: String,
        proofPhotoUri: String?,
        redistributeAdvance: Boolean,
        targetInstallmentIds: List<Long>
    ): Boolean {
        return database.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: return@withTransaction false'''
repo_content = repo_content.replace(old_smart, new_smart)

repo_content = repo_content.replace(
'''        auditLogDao.insertAuditLog(auditLog)

        true
    }''',
'''        auditLogDao.insertAuditLog(auditLog)

        true
        }
    }'''
)

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(repo_content)

# 2. Patch AppContainer.kt
with open('app/src/main/java/com/example/core/di/AppContainer.kt', 'r') as f:
    container_content = f.read()

container_content = container_content.replace(
'''        LoanRepository(
            database.loanDao(),''',
'''        LoanRepository(
            database,
            database.loanDao(),'''
)

with open('app/src/main/java/com/example/core/di/AppContainer.kt', 'w') as f:
    f.write(container_content)

