import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

# Fix import in the middle
content = content.replace("import androidx.room.withTransaction\nimport com.example.data.local.AppDatabase\n\nclass LoanRepository", "class LoanRepository")
content = "import androidx.room.withTransaction\nimport com.example.data.local.AppDatabase\n" + content

# Fix returns
content = content.replace("val loan = loanDao.getLoanById(loanId) ?: return@withTransaction false\n        if (loan.status == \"LIQUIDADO\" || loan.status == \"CANCELADO\") return false\n        if (amount <= 0.0) return false", 
"val loan = loanDao.getLoanById(loanId) ?: return@withTransaction false\n        if (loan.status == \"LIQUIDADO\" || loan.status == \"CANCELADO\") return@withTransaction false\n        if (amount <= 0.0) return@withTransaction false")

content = content.replace("        true\n        }\n    }\n", "        true\n        }\n    }\n}") # Wait, it said Missing } at EOF. Let me just restore the old structure if this is too complex.

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

