import re

with open('app/src/test/java/com/example/domain/ComprehensiveBusinessRulesTest.kt', 'r') as f:
    content = f.read()

new_tests = """
    // 41. PRUEBA CRÍTICA DE PAGO (Idempotencia y suma en caja)
    @Test
    fun test41_criticalPayment() = runBlocking {
        cashRepo.openCashRegister(1L, "Cobrador 1", 5000.0, "Central", "Fondo", "Apertura", "admin")
        
        val clientId = db.clientDao().insertClient(ClientEntity(fullName = "Juan Pago", curp = "PAGJ900101HDFRNN08", ine = "8899001122334"))
        val loanId = loanRepo.createLoan(clientId = clientId, capital = 1000.0, planTypeCode = "PLAN_20_DIAS", skipSundays = true, collectorId = 1L, collectorName = "Cobrador 1", currentUser = "admin", currentUserRole = "ADMINISTRADOR")
        loanRepo.disburseLoan(loanId, 1L, "Cobrador 1", "admin")
        
        val preReg = db.cashRegisterDao().getActiveCashRegisterForCollectorSync(1L)!!
        val preCash = preReg.cashInflows
        
        // Register payment
        val r1 = loanRepo.registerSmartPayment(loanId, 500.0, "EFECTIVO", 1L, "Cobrador 1", "admin", "Pago 1", "NORMAL", null, false, emptyList())
        assertTrue(r1)
        
        val postReg = db.cashRegisterDao().getActiveCashRegisterForCollectorSync(1L)!!
        assertEquals(preCash + 500.0, postReg.cashInflows, 0.01)
        
        // Duplicate payment attempt
        val r2 = loanRepo.registerSmartPayment(loanId, 500.0, "EFECTIVO", 1L, "Cobrador 1", "admin", "Pago 1", "NORMAL", null, false, emptyList())
        // Should not duplicate
        val postReg2 = db.cashRegisterDao().getActiveCashRegisterForCollectorSync(1L)!!
        assertEquals(preCash + 500.0, postReg2.cashInflows, 0.01) // Inflow shouldn't increase
    }

    // 42. PRUEBA CRÍTICA DE DESEMBOLSO (Caja no negativa y egreso)
    @Test
    fun test42_criticalDisbursement() = runBlocking {
        cashRepo.openCashRegister(1L, "Cobrador 1", 10000.0, "Central", "Fondo", "Apertura", "admin")
        
        val clientId = db.clientDao().insertClient(ClientEntity(fullName = "Juan Desembolso", curp = "DESJ900101HDFRNN08", ine = "8899001122334"))
        val loanId = loanRepo.createLoan(clientId = clientId, capital = 2000.0, planTypeCode = "PLAN_20_DIAS", skipSundays = true, collectorId = 1L, collectorName = "Cobrador 1", currentUser = "admin", currentUserRole = "ADMINISTRADOR")
        
        val preReg = db.cashRegisterDao().getActiveCashRegisterForCollectorSync(1L)!!
        val preOut = preReg.outflows
        
        loanRepo.disburseLoan(loanId, 1L, "Cobrador 1", "admin")
        
        val postReg = db.cashRegisterDao().getActiveCashRegisterForCollectorSync(1L)!!
        assertEquals(preOut + 2000.0, postReg.outflows, 0.01)
        assertEquals(8000.0, postReg.expectedCash, 0.01)
    }

    // 43. PRUEBA CRÍTICA DE TRANSFERENCIA
    @Test
    fun test43_criticalTransfer() = runBlocking {
        cashRepo.openCashRegister(1L, "Cobrador 1", 5000.0, "Central", "Fondo", "Apertura", "admin")
        
        val clientId = db.clientDao().insertClient(ClientEntity(fullName = "Juan Transfer", curp = "TRAJ900101HDFRNN08", ine = "8899001122334"))
        val loanId = loanRepo.createLoan(clientId = clientId, capital = 1000.0, planTypeCode = "PLAN_20_DIAS", skipSundays = true, collectorId = 1L, collectorName = "Cobrador 1", currentUser = "admin", currentUserRole = "ADMINISTRADOR")
        loanRepo.disburseLoan(loanId, 1L, "Cobrador 1", "admin")
        
        val preReg = db.cashRegisterDao().getActiveCashRegisterForCollectorSync(1L)!!
        val preCash = preReg.cashInflows
        val preTransfer = preReg.transferInflows
        
        // Register transfer payment
        val r1 = loanRepo.registerSmartPayment(loanId, 2000.0, "TRANSFERENCIA", 1L, "Cobrador 1", "admin", "Pago 1", "NORMAL", null, false, emptyList())
        assertTrue(r1)
        
        val postReg = db.cashRegisterDao().getActiveCashRegisterForCollectorSync(1L)!!
        assertEquals(preTransfer + 2000.0, postReg.transferInflows, 0.01)
        assertEquals(preCash, postReg.cashInflows, 0.01) // Physical cash doesn't increase
    }

    // 44. PRUEBA CRÍTICA DE ARQUEO
    @Test
    fun test44_criticalArqueo() = runBlocking {
        val regId = cashRepo.openCashRegister(1L, "Cobrador 1", 5000.0, "Central", "Fondo", "Apertura", "admin")
        
        cashRepo.closeCashRegister(regId, 4800.0, "Faltante en caja", "admin", null)
        
        val reg = db.cashRegisterDao().getCashRegisterById(regId)!!
        assertEquals(-200.0, reg.discrepancy ?: 0.0, 0.01)
        assertEquals("FALTANTE", reg.auditStatus)
    }
}
"""

content = content.replace('}\n}', '}\n' + new_tests)

with open('app/src/test/java/com/example/domain/ComprehensiveBusinessRulesTest.kt', 'w') as f:
    f.write(content)

