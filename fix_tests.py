import re

with open('app/src/test/java/com/example/domain/ComprehensiveBusinessRulesTest.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''        loanRepo = LoanRepository(
            loanDao = db.loanDao(),
            installmentDao = db.installmentDao(),''',
'''        loanRepo = LoanRepository(
            database = db,
            loanDao = db.loanDao(),
            installmentDao = db.installmentDao(),''')

with open('app/src/test/java/com/example/domain/ComprehensiveBusinessRulesTest.kt', 'w') as f:
    f.write(content)

