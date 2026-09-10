import re

with open('app/src/test/java/com/example/domain/ComprehensiveBusinessRulesTest.kt', 'r') as f:
    content = f.read()

bad_string = """
    // I. Prueba Crítica de Doble Pago (Idempotencia)
    @Test
    fun test_MoraI_DoublePayment_Guard() = runBlocking {
        val clientId = db.clientDao().insertClient(ClientEntity(fullName = "Elena Gomez", curp = "GOME900101MDFRNN15", ine = "1111111111111"))
        val loanId = loanRepo.createLoan(clientId = clientId, capital = 1000.0, planTypeCode = "PLAN_20_DIAS", skipSundays = true, collectorId = 1L, collectorName = "Cobrador 1", currentUser = "admin", currentUserRole = "ADMINISTRADOR")
        loanRepo.disburseLoan(loanId, 1L, "Cobrador 1", "admin")
        
        // Register payment first time
        val r1 = loanRepo.registerSmartPayment(
            loanId = loanId, amount = 100.0, method = "EFECTIVO", collectorId = 1L, collectorName = "Cobrador 1", currentUser = "admin",
            notes = "", paymentType = "NORMAL", proofPhotoUri = null, redistributeAdvance = false, targetInstallmentIds = emptyList()
        )
        assertTrue("First payment should succeed", r1)
        
        // Register same payment immediately (simulating double click)
        val r2 = loanRepo.registerSmartPayment(
            loanId = loanId, amount = 100.0, method = "EFECTIVO", collectorId = 1L, collectorName = "Cobrador 1", currentUser = "admin",
            notes = "", paymentType = "NORMAL", proofPhotoUri = null, redistributeAdvance = false, targetInstallmentIds = emptyList()
        )
        // Should return true (idempotent success) but not duplicate the payment records
        
        val allPayments = db.paymentDao().getPaymentsByLoanSync(loanId)
        assertEquals("Should only have 1 payment recorded", 1, allPayments.size)
    }
}
"""

content = content.replace(bad_string, "}\n")

# Now append it only at the very end
content = content.strip()
if content.endswith("}"):
    content = content[:-1] + bad_string

with open('app/src/test/java/com/example/domain/ComprehensiveBusinessRulesTest.kt', 'w') as f:
    f.write(content)

