import re

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                action = "REGISTRO_PAGO_INTELIGENTE",
                entityType = "PAGO",
                entityId = receiptNo,
                newValues = "Pago $paymentType de $amount MXN ($method) para Préstamo #$loanId. Redistribución: $redistributeAdvance. Recibo: $receiptNo. Comprobante: ${proofPhotoUri ?: "Ninguno"}"
            )
        )

        return true
    }''',
'''                action = "REGISTRO_PAGO_INTELIGENTE",
                entityType = "PAGO",
                entityId = receiptNo,
                newValues = "Pago $paymentType de $amount MXN ($method) para Préstamo #$loanId. Redistribución: $redistributeAdvance. Recibo: $receiptNo. Comprobante: ${proofPhotoUri ?: "Ninguno"}"
            )
        )

        true
        }
    }''')

with open('app/src/main/java/com/example/data/repository/Repositories.kt', 'w') as f:
    f.write(content)

