import re

with open('app/src/main/java/com/example/ui/screens/LoanDetailScreen.kt', 'r') as f:
    content = f.read()

old_vars = '''    val skippedLogs by viewModel.getSkippedPaymentsForLoan(loanId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val userRole = userSession.user?.role?.uppercase() ?: "AGENTE"'''

new_vars = '''    val skippedLogs by viewModel.getSkippedPaymentsForLoan(loanId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val activeCash by viewModel.activeCashRegister.collectAsStateWithLifecycle()

    val userRole = userSession.user?.role?.uppercase() ?: "AGENTE"'''

content = content.replace(old_vars, new_vars)


old_button = '''                                    Button(
                                        onClick = { onNavigate("process_payment/$loanId") },
                                        modifier = Modifier.weight(1f).testTag("btn_pay_from_detail")
                                    ) {'''

new_button = '''                                    Button(
                                        onClick = { onNavigate("process_payment/$loanId") },
                                        enabled = activeCash != null && activeCash?.status == "ABIERTA",
                                        modifier = Modifier.weight(1f).testTag("btn_pay_from_detail")
                                    ) {'''

content = content.replace(old_button, new_button)

with open('app/src/main/java/com/example/ui/screens/LoanDetailScreen.kt', 'w') as f:
    f.write(content)

