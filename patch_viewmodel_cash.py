import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    content = f.read()

# Make activeCashRegister depend on the user session
old_flow = '''    val activeCashRegister: StateFlow<CashRegisterEntity?> = cashRepo.activeCashRegister
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)'''

new_flow = '''    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeCashRegister: StateFlow<CashRegisterEntity?> = userSession
        .map { it.user?.id ?: -1L }
        .flatMapLatest { userId ->
            if (userId == -1L) kotlinx.coroutines.flow.flowOf(null)
            else cashRepo.getActiveCashRegisterForCollector(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)'''

content = content.replace(old_flow, new_flow)

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(content)

