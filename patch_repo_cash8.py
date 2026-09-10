import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

old_logic = '''        when (type) {
            "INGRESO_MANUAL", "AJUSTE_POSITIVO" -> {
                if (isCash) newCashInflows += amount
                if (isTransfer) newTransferInflows += amount
            }
            "EGRESO_GASTO", "EGRESO_RETIRO", "AJUSTE_NEGATIVO" -> {
                newOutflows += amount
            }
            else -> {
                if (type.startsWith("INGRESO")) {
                    if (isCash) newCashInflows += amount else newTransferInflows += amount
                } else {
                    newOutflows += amount
                }
            }
        }'''

new_logic = '''        when (type) {
            "INGRESO_MANUAL", "AJUSTE_POSITIVO" -> {
                if (isCash) newCashInflows += amount
                if (isTransfer) newTransferInflows += amount
            }
            "EGRESO_GASTO", "EGRESO_RETIRO", "AJUSTE_NEGATIVO" -> {
                if (isCash && (reg.expectedCash - amount) < 0.0) {
                    throw IllegalStateException("El egreso de $$amount excede el saldo de caja disponible ($${reg.expectedCash}).")
                }
                newOutflows += amount
            }
            else -> {
                if (type.startsWith("INGRESO")) {
                    if (isCash) newCashInflows += amount else newTransferInflows += amount
                } else {
                    if (isCash && (reg.expectedCash - amount) < 0.0) {
                        throw IllegalStateException("El egreso de $$amount excede el saldo de caja disponible ($${reg.expectedCash}).")
                    }
                    newOutflows += amount
                }
            }
        }'''

content = content.replace(old_logic, new_logic)

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

