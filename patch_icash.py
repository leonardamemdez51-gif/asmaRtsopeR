import re

with open('app/src/main/java/com/example/domain/repositories/RepositoryInterfaces.kt', 'r') as f:
    content = f.read()

content = content.replace('''interface ICashRepository {
    val activeCashRegister: Flow<CashRegisterEntity?>
    val allCashRegisters: Flow<List<CashRegisterEntity>>''', '''interface ICashRepository {
    val activeCashRegister: Flow<CashRegisterEntity?>
    val allCashRegisters: Flow<List<CashRegisterEntity>>
    fun getActiveCashRegisterForCollectorFlow(collectorId: Long): Flow<CashRegisterEntity?>''')

with open('app/src/main/java/com/example/domain/repositories/RepositoryInterfaces.kt', 'w') as f:
    f.write(content)

