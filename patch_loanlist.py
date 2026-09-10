import re

with open('app/src/main/java/com/example/ui/screens/LoanListScreen.kt', 'r') as f:
    content = f.read()

old_vars = '''    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("TODOS") }'''

new_vars = '''    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val activeCash by viewModel.activeCashRegister.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("TODOS") }'''

content = content.replace(old_vars, new_vars)

old_btn = '''                                        Button(
                                            onClick = { onNavigate("process_payment/${loan.id}") },
                                            modifier = Modifier.testTag("btn_pay_${loan.id}")
                                        ) {'''

new_btn = '''                                        Button(
                                            onClick = { onNavigate("process_payment/${loan.id}") },
                                            enabled = activeCash != null && activeCash?.status == "ABIERTA",
                                            modifier = Modifier.testTag("btn_pay_${loan.id}")
                                        ) {'''

content = content.replace(old_btn, new_btn)

with open('app/src/main/java/com/example/ui/screens/LoanListScreen.kt', 'w') as f:
    f.write(content)

