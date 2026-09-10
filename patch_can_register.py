import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

old_state = '''    val collectorScreenState: StateFlow<CollectorScreenState> = combine(
        userSession,
        combine(_collectorSearchQuery, _collectorSelectedCategory) { q, cat -> Pair(q, cat) },
        combine(allInstallments, allLoans) { inst, l -> Pair(inst, l) },
        combine(allClientsList, allVisits, allPayments) { c, v, p -> Triple(c, v, p) }
    ) { session, queryCat, instLoans, clientsVisitsPayments ->
        val query = queryCat.first'''

new_state = '''    val collectorScreenState: StateFlow<CollectorScreenState> = combine(
        userSession,
        activeCashRegister,
        combine(_collectorSearchQuery, _collectorSelectedCategory) { q, cat -> Pair(q, cat) },
        combine(allInstallments, allLoans) { inst, l -> Pair(inst, l) },
        combine(allClientsList, allVisits, allPayments) { c, v, p -> Triple(c, v, p) }
    ) { session, cashReg, queryCat, instLoans, clientsVisitsPayments ->
        val query = queryCat.first'''

content = content.replace(old_state, new_state)

old_return = '''            canRegisterPayment = session.user?.role == "ADMINISTRADOR" || session.user?.role == "COBRADOR",
            isOfflineMode = true,
            lastSyncFormatted = "Base de datos Room Offline Lista (100% Funcional)"
        )'''

new_return = '''            canRegisterPayment = (session.user?.role == "ADMINISTRADOR" || session.user?.role == "COBRADOR") && cashReg != null && cashReg.status == "ABIERTA",
            isOfflineMode = true,
            lastSyncFormatted = "Base de datos Room Offline Lista (100% Funcional)"
        )'''

content = content.replace(old_return, new_return)

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)

