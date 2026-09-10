import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

# Replace getActiveCashRegisterSync() with getActiveCashRegisterForCollectorSync(collectorId) 
# where collectorId is available in the function scope.

# For registerSmartPayment, it takes collectorId.
old_register_payment_1 = '''        // Update active cash register
        val activeCash = cashRegisterDao.getActiveCashRegisterSync()'''
new_register_payment_1 = '''        // Update active cash register
        val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)'''

content = content.replace(old_register_payment_1, new_register_payment_1)

old_register_payment_2 = '''        // Active cash register inflow
        val activeCash = cashRegisterDao.getActiveCashRegisterSync()'''
new_register_payment_2 = '''        // Active cash register inflow
        val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)'''
content = content.replace(old_register_payment_2, new_register_payment_2)


old_disburse_1 = '''        // Active cash register outflow
        val activeCash = cashRegisterDao.getActiveCashRegisterSync()'''
new_disburse_1 = '''        // Active cash register outflow
        val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)'''
content = content.replace(old_disburse_1, new_disburse_1)

old_disburse_2 = '''        // Outflow net cash disbursed
        if (netCashOutflow > 0.0) {
            val activeCash = cashRegisterDao.getActiveCashRegisterSync()'''
new_disburse_2 = '''        // Outflow net cash disbursed
        if (netCashOutflow > 0.0) {
            val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)'''
content = content.replace(old_disburse_2, new_disburse_2)

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

