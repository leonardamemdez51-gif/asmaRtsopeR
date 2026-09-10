import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

# Replace getActiveCashRegisterSync() with getActiveCashRegisterForCollectorSync(collectorId) 
old_val = '''        if (initialStatus == "ACTIVO") {
            val installments = LoanCalculator.generateInstallments(
                loanId = loanId,
                clientId = clientId,
                capital = capital,
                interestAmount = calc.interestAmount,
                totalAmount = calc.totalAmount,
                dailyPayment = calc.dailyPayment,
                totalDays = calc.totalDays,
                startDateMs = now,
                skipSundays = skipSundays
            )
            installmentDao.insertInstallments(installments)

            val activeCash = cashRegisterDao.getActiveCashRegisterSync()'''
new_val = '''        if (initialStatus == "ACTIVO") {
            val installments = LoanCalculator.generateInstallments(
                loanId = loanId,
                clientId = clientId,
                capital = capital,
                interestAmount = calc.interestAmount,
                totalAmount = calc.totalAmount,
                dailyPayment = calc.dailyPayment,
                totalDays = calc.totalDays,
                startDateMs = now,
                skipSundays = skipSundays
            )
            installmentDao.insertInstallments(installments)

            val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)'''

content = content.replace(old_val, new_val)

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

