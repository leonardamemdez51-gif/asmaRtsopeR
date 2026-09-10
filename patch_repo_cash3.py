import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

# Fix openCashRegister
old_open = '''        // Enforce constraint: Only ONE active cash register allowed per user/collector!
        val existingActive = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
            ?: cashRegisterDao.getActiveCashRegisterSync()'''
new_open = '''        // Enforce constraint: Only ONE active cash register allowed per user/collector!
        val existingActive = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)'''
content = content.replace(old_open, new_open)

# Fix closeCashRegister
old_close = '''        val reg = cashRegisterDao.getCashRegisterById(registerId)
            ?: cashRegisterDao.getActiveCashRegisterSync()
            ?: return false'''
new_close = '''        val reg = cashRegisterDao.getCashRegisterById(registerId)
            ?: return false'''
content = content.replace(old_close, new_close)

# Fix addCashMovement
old_add = '''        val reg = cashRegisterDao.getCashRegisterById(registerId)
            ?: cashRegisterDao.getActiveCashRegisterSync()
            ?: throw IllegalStateException("No hay una caja activa para registrar el movimiento.")'''
new_add = '''        val reg = cashRegisterDao.getCashRegisterById(registerId)
            ?: throw IllegalStateException("No hay una caja activa para registrar el movimiento.")'''
content = content.replace(old_add, new_add)

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

