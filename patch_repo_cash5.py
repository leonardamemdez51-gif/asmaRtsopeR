import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

# For createLoan
old_cl = '''        if (initialStatus == "ACTIVO") {
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

            val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
            if (activeCash != null) {
                val updatedOutflows = activeCash.outflows + capital'''

new_cl = '''        if (initialStatus == "ACTIVO") {
            val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
            if (activeCash != null && (activeCash.expectedCash - capital) < 0.0) {
                // Should not happen as UI should prevent it, but enforce business rule
                // Assuming "permitir desembolsos" has no explicit config yet, we strictly protect against negative cash
                throw IllegalStateException("El desembolso de $$capital excede el saldo de caja disponible ($${activeCash.expectedCash}).")
            }

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

            if (activeCash != null) {
                val updatedOutflows = activeCash.outflows + capital'''
content = content.replace(old_cl, new_cl)

# For disburseLoan
old_dl = '''        val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
        if (activeCash != null) {
            val updatedOutflows = activeCash.outflows + loan.capital'''
new_dl = '''        val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
        if (activeCash != null) {
            if ((activeCash.expectedCash - loan.capital) < 0.0) {
                throw IllegalStateException("El desembolso de $${loan.capital} excede el saldo de caja disponible ($${activeCash.expectedCash}).")
            }
            val updatedOutflows = activeCash.outflows + loan.capital'''
content = content.replace(old_dl, new_dl)


# For renewLoan
old_rl = '''        // Outflow net cash disbursed
        if (netCashOutflow > 0.0) {
            val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
            if (activeCash != null) {
                val updatedOutflows = activeCash.outflows + netCashOutflow'''
new_rl = '''        // Outflow net cash disbursed
        if (netCashOutflow > 0.0) {
            val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
            if (activeCash != null) {
                if ((activeCash.expectedCash - netCashOutflow) < 0.0) {
                    throw IllegalStateException("El efectivo neto a entregar ($$netCashOutflow) excede el saldo de caja disponible ($${activeCash.expectedCash}).")
                }
                val updatedOutflows = activeCash.outflows + netCashOutflow'''
content = content.replace(old_rl, new_rl)

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

