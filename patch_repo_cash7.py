import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

content = content.replace('    fun getActiveCashRegisterForCollectorFlow(collectorId: Long)', '    override fun getActiveCashRegisterForCollectorFlow(collectorId: Long)')

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

