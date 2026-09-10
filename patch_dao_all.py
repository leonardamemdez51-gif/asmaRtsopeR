import re

with open('app/src/main/java/com/example/data/local/Daos.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    suspend fun getRecentPaymentsByLoanSync(loanId: Long): List<PaymentEntity>''',
'''    suspend fun getRecentPaymentsByLoanSync(loanId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    suspend fun getPaymentsByLoanSync(loanId: Long): List<PaymentEntity>'''
)

with open('app/src/main/java/com/example/data/local/Daos.kt', 'w') as f:
    f.write(content)

