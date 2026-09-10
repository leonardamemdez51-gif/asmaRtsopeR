import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

old_smart = '''        if (amount <= 0.0) return@withTransaction false

        val now = System.currentTimeMillis()
        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(8)}"'''

new_smart = '''        if (amount <= 0.0) return@withTransaction false

        val now = System.currentTimeMillis()
        
        // Anti-double-payment guard
        val recentPayments = paymentDao.getRecentPaymentsByLoanSync(loanId)
        val duplicate = recentPayments.firstOrNull { it.amount == amount && (now - it.paymentDate) < 15000 }
        if (duplicate != null) {
            // Already registered recently to avoid double tap
            return@withTransaction true
        }

        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(8)}"'''

content = content.replace(old_smart, new_smart)

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

