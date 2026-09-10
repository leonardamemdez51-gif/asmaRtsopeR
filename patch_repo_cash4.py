import re

with open('app/src/main/java/com/example/data/local/Daos.kt', 'r') as f:
    content = f.read()

# Add flow version of getActiveCashRegisterForCollector
new_query = '''    @Query("SELECT * FROM cash_registers WHERE collectorId = :collectorId AND status = 'ABIERTA' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveCashRegisterForCollectorSync(collectorId: Long): CashRegisterEntity?

    @Query("SELECT * FROM cash_registers WHERE collectorId = :collectorId AND status = 'ABIERTA' ORDER BY id DESC LIMIT 1")
    fun getActiveCashRegisterForCollectorFlow(collectorId: Long): kotlinx.coroutines.flow.Flow<CashRegisterEntity?>'''

content = content.replace('''    @Query("SELECT * FROM cash_registers WHERE collectorId = :collectorId AND status = 'ABIERTA' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveCashRegisterForCollectorSync(collectorId: Long): CashRegisterEntity?''', new_query)

with open('app/src/main/java/com/example/data/local/Daos.kt', 'w') as f:
    f.write(content)


with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

new_method = '''    fun getActiveCashRegisterForCollectorFlow(collectorId: Long): kotlinx.coroutines.flow.Flow<CashRegisterEntity?>'''

content = content.replace('''    val allCashRegisters: Flow<List<CashRegisterEntity>>''', '''    val allCashRegisters: Flow<List<CashRegisterEntity>>
''' + new_method)

content = content.replace('''    override val allCashRegisters: Flow<List<CashRegisterEntity>> = cashRegisterDao.getAllCashRegisters()''', '''    override val allCashRegisters: Flow<List<CashRegisterEntity>> = cashRegisterDao.getAllCashRegisters()

    override fun getActiveCashRegisterForCollectorFlow(collectorId: Long): kotlinx.coroutines.flow.Flow<CashRegisterEntity?> =
        cashRegisterDao.getActiveCashRegisterForCollectorFlow(collectorId)''')

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)


with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('cashRepo.getActiveCashRegisterForCollector(userId)', 'cashRepo.getActiveCashRegisterForCollectorFlow(userId)')

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)

