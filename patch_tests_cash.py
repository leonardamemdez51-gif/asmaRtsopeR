import re

with open('app/src/test/java/com/example/domain/ComprehensiveBusinessRulesTest.kt', 'r') as f:
    content = f.read()

content = content.replace('db.cashRegisterDao().getActiveCashRegisterSync()', 'db.cashRegisterDao().getActiveCashRegisterForCollectorSync(1L)')
content = content.replace('loanRepo.registerPayment(', 'loanRepo.registerSmartPayment(')

with open('app/src/test/java/com/example/domain/ComprehensiveBusinessRulesTest.kt', 'w') as f:
    f.write(content)

