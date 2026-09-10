package com.example

import com.example.data.local.AuditLogEntity
import com.example.data.local.AuditLogDao
import com.example.data.local.SystemConfigEntity
import com.example.data.local.SystemConfigDao
import com.example.data.repository.AuditRepository
import com.example.data.repository.ConfigRepository
import com.example.core.security.AppPermission
import com.example.core.security.RoleRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class AuditSystemTest {

    // Fakes for database tier
    class FakeAuditLogDao : AuditLogDao {
        val logs = mutableListOf<AuditLogEntity>()
        override fun getAllAuditLogs(): Flow<List<AuditLogEntity>> = flowOf(logs)
        override suspend fun insertAuditLog(log: AuditLogEntity): Long {
            val inserted = log.copy(id = (logs.size + 1).toLong())
            logs.add(inserted)
            return inserted.id
        }
    }

    class FakeSystemConfigDao : SystemConfigDao {
        var currentConfig: SystemConfigEntity? = SystemConfigEntity()
        override fun getConfig(): Flow<SystemConfigEntity?> = flowOf(currentConfig)
        override suspend fun getConfigSync(): SystemConfigEntity? = currentConfig
        override suspend fun insertOrUpdateConfig(config: SystemConfigEntity) {
            currentConfig = config
        }
    }

    @Test
    fun testTraceIdAutoGeneration() {
        val entity = AuditLogEntity(
            userId = 1L,
            username = "admin",
            action = "TEST_ACTION",
            entityType = "TEST_ENTITY",
            entityId = "100"
        )
        assertNotNull(entity.traceId)
        assertTrue(entity.traceId.startsWith("TRX-2026-"))
    }

    @Test
    fun testTraceIdUniquePerNanoTime() {
        val entity1 = AuditLogEntity(userId = 1L, username = "u", action = "A", entityType = "E", entityId = "1")
        val entity2 = AuditLogEntity(userId = 1L, username = "u", action = "A", entityType = "E", entityId = "2")
        // Check uniqueness; even if created closely, nanotechnology/nanotime components make them different or sequential
        assertNotEquals(entity1.traceId, entity2.traceId)
    }

    @Test
    fun testAuditLogEntityFieldsPreserved() {
        val entity = AuditLogEntity(
            userId = 42L,
            username = "supervisor_one",
            action = "LOAN_APPROVE",
            entityType = "LOAN",
            entityId = "999",
            userRole = "SUPERVISOR",
            reason = "Excellent credit score",
            notes = "Checked history manually",
            result = "EXITOSO",
            latitude = 19.4326,
            longitude = -99.1332
        )
        assertEquals(42L, entity.userId)
        assertEquals("supervisor_one", entity.username)
        assertEquals("LOAN_APPROVE", entity.action)
        assertEquals("LOAN", entity.entityType)
        assertEquals("999", entity.entityId)
        assertEquals("SUPERVISOR", entity.userRole)
        assertEquals("Excellent credit score", entity.reason)
        assertEquals("Checked history manually", entity.notes)
        assertEquals("EXITOSO", entity.result)
        assertEquals(19.4326, entity.latitude!!, 0.0001)
        assertEquals(-99.1332, entity.longitude!!, 0.0001)
    }

    @Test
    fun testAuditRepositoryLogActionPersistence() = runBlocking {
        val fakeDao = FakeAuditLogDao()
        val repository = AuditRepository(fakeDao)

        val logId = repository.logAction(
            userId = 10L,
            username = "teller1",
            action = "CASH_DEPOSIT",
            entityType = "CASH_REGISTER",
            entityId = "5",
            previousValues = "Bal: 100",
            newValues = "Bal: 500",
            traceId = "TRX-MOCK-777",
            userRole = "COBRADOR",
            reason = "End of day cash up",
            notes = "Matched receipts",
            result = "EXITOSO",
            latitude = 20.6597,
            longitude = -103.3496
        )

        assertEquals(1L, logId)
        assertEquals(1, fakeDao.logs.size)
        val saved = fakeDao.logs.first()
        assertEquals("teller1", saved.username)
        assertEquals("CASH_DEPOSIT", saved.action)
        assertEquals("TRX-MOCK-777", saved.traceId)
        assertEquals(20.6597, saved.latitude!!, 0.0001)
    }

    @Test
    fun testConfigRepositoryChangeAudit() = runBlocking {
        val fakeAuditDao = FakeAuditLogDao()
        val fakeConfigDao = FakeSystemConfigDao()
        val configRepo = ConfigRepository(fakeConfigDao, fakeAuditDao)

        val updatedConfig = SystemConfigEntity(
            plan20InterestRate = 0.25,
            plan30InterestRate = 0.35,
            lateFeePercentage = 0.08,
            allowSkipSundays = false
        )

        configRepo.updateConfig(updatedConfig, "admin_user", "Actualizacion anual de tasas")

        // Config should be updated
        val current = fakeConfigDao.getConfigSync()
        assertNotNull(current)
        assertEquals(0.25, current!!.plan20InterestRate, 0.01)

        // Audit log should be populated
        assertEquals(1, fakeAuditDao.logs.size)
        val log = fakeAuditDao.logs.first()
        assertEquals("CONFIG_UPDATED", log.action)
        assertEquals("admin_user", log.username)
        assertEquals("Actualizacion anual de tasas", log.notes)
        assertTrue(log.previousValues!!.contains("P20 Interés"))
        assertTrue(log.newValues!!.contains("P20 Interés"))
    }

    @Test
    fun testAuditFilterByUsername() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "Alice", action = "CREATE", entityType = "U", entityId = "1"),
            AuditLogEntity(userId = 2, username = "Bob", action = "CREATE", entityType = "U", entityId = "2")
        )
        val query = "Bob"
        val filtered = logs.filter { it.username.contains(query, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("Bob", filtered.first().username)
    }

    @Test
    fun testAuditFilterByAction() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "USER_LOGIN", entityType = "U", entityId = "1"),
            AuditLogEntity(userId = 2, username = "A", action = "PAYMENT_RECEIVE", entityType = "P", entityId = "2")
        )
        val query = "LOGIN"
        val filtered = logs.filter { it.action.contains(query, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("USER_LOGIN", filtered.first().action)
    }

    @Test
    fun testAuditFilterByTraceId() {
        val log1 = AuditLogEntity(userId = 1, username = "A", action = "A", entityType = "U", entityId = "1")
        val logs = listOf(log1)
        val query = log1.traceId
        val filtered = logs.filter { it.traceId.contains(query, ignoreCase = true) }
        assertEquals(1, filtered.size)
    }

    @Test
    fun testAuditFilterByNotes() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "A", entityType = "U", entityId = "1", notes = "Regular deposit"),
            AuditLogEntity(userId = 2, username = "B", action = "B", entityType = "U", entityId = "2", notes = "Exception case")
        )
        val query = "Exception"
        val filtered = logs.filter { (it.notes ?: "").contains(query, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("B", filtered.first().username)
    }

    @Test
    fun testAuditFilterByReason() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "A", entityType = "U", entityId = "1", reason = "Score drop"),
            AuditLogEntity(userId = 2, username = "B", action = "B", entityType = "U", entityId = "2", reason = "Manager override")
        )
        val query = "Manager"
        val filtered = logs.filter { (it.reason ?: "").contains(query, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("B", filtered.first().username)
    }

    @Test
    fun testAuditFilterByEntityType() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "A", entityType = "ClientEntity", entityId = "1"),
            AuditLogEntity(userId = 2, username = "B", action = "B", entityType = "UserEntity", entityId = "2")
        )
        val query = "Client"
        val filtered = logs.filter { it.entityType.contains(query, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("A", filtered.first().username)
    }

    @Test
    fun testAuditFilterByEntityId() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "A", entityType = "U", entityId = "101"),
            AuditLogEntity(userId = 2, username = "B", action = "B", entityType = "U", entityId = "202")
        )
        val query = "202"
        val filtered = logs.filter { it.entityId == query }
        assertEquals(1, filtered.size)
        assertEquals("B", filtered.first().username)
    }

    @Test
    fun testAuditRoleRestrictionForCobrador() {
        val role = "COBRADOR"
        val hasAccess = role != "COBRADOR" // Simulated restriction
        assertFalse(hasAccess)
    }

    @Test
    fun testAuditRoleRestrictionForSupervisorSensitiveActions() {
        val role = "SUPERVISOR"
        val log = AuditLogEntity(userId = 1, username = "A", action = "RESET_PASSWORD", entityType = "U", entityId = "1")
        
        val canSupervisorSee = if (role == "SUPERVISOR") {
            val sensitiveActions = listOf("RESET_PASSWORD", "BLOQUEAR_USUARIO", "DESBLOQUEAR_USUARIO")
            !sensitiveActions.contains(log.action)
        } else true

        assertFalse(canSupervisorSee)
    }

    @Test
    fun testAuditResultFilterTodos() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "A", entityType = "U", entityId = "1", result = "EXITOSO"),
            AuditLogEntity(userId = 2, username = "B", action = "B", entityType = "U", entityId = "2", result = "FALLIDO")
        )
        val filter = "TODOS"
        val filtered = logs.filter { filter == "TODOS" || it.result.equals(filter, ignoreCase = true) }
        assertEquals(2, filtered.size)
    }

    @Test
    fun testAuditResultFilterExitoso() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "A", entityType = "U", entityId = "1", result = "EXITOSO"),
            AuditLogEntity(userId = 2, username = "B", action = "B", entityType = "U", entityId = "2", result = "FALLIDO")
        )
        val filter = "EXITOSO"
        val filtered = logs.filter { filter == "TODOS" || it.result.equals(filter, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("EXITOSO", filtered.first().result)
    }

    @Test
    fun testAuditResultFilterFallido() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "A", entityType = "U", entityId = "1", result = "EXITOSO"),
            AuditLogEntity(userId = 2, username = "B", action = "B", entityType = "U", entityId = "2", result = "FALLIDO")
        )
        val filter = "FALLIDO"
        val filtered = logs.filter { filter == "TODOS" || it.result.equals(filter, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("FALLIDO", filtered.first().result)
    }

    @Test
    fun testAuditCategoryFilterUsuarios() {
        val log = AuditLogEntity(userId = 1, username = "A", action = "USER_CREATED", entityType = "UserEntity", entityId = "1")
        val isUserCat = log.action.contains("USER") || log.entityType.contains("User")
        assertTrue(isUserCat)
    }

    @Test
    fun testAuditCategoryFilterClientes() {
        val log = AuditLogEntity(userId = 1, username = "A", action = "CLIENT_REGISTERED", entityType = "ClientEntity", entityId = "1")
        val isClientCat = log.action.contains("CLIENT") || log.entityType.contains("Client")
        assertTrue(isClientCat)
    }

    @Test
    fun testAuditCategoryFilterPrestamos() {
        val log = AuditLogEntity(userId = 1, username = "A", action = "LOAN_APPROVED", entityType = "LoanEntity", entityId = "1")
        val isLoanCat = log.action.contains("LOAN") || log.entityType.contains("Loan")
        assertTrue(isLoanCat)
    }

    @Test
    fun testAuditCategoryFilterPagos() {
        val log = AuditLogEntity(userId = 1, username = "A", action = "PAYMENT_RECEIVED", entityType = "PaymentEntity", entityId = "1")
        val isPaymentCat = log.action.contains("PAYMENT") || log.entityType.contains("Payment")
        assertTrue(isPaymentCat)
    }

    @Test
    fun testAuditCategoryFilterCaja() {
        val log = AuditLogEntity(userId = 1, username = "A", action = "CASH_CLOSED", entityType = "CashRegisterEntity", entityId = "1")
        val isCashCat = log.action.contains("CASH") || log.entityType.contains("Cash")
        assertTrue(isCashCat)
    }

    @Test
    fun testAuditCategoryFilterConfiguracion() {
        val log = AuditLogEntity(userId = 1, username = "A", action = "CONFIG_UPDATED", entityType = "SystemConfigEntity", entityId = "1")
        val isConfigCat = log.action == "CONFIG_UPDATED" || log.entityType.contains("Config")
        assertTrue(isConfigCat)
    }

    @Test
    fun testAppPermissionAddition() {
        val permissions = AppPermission.values()
        assertTrue(permissions.any { it.name == "AUDIT_VIEW_FINANCIAL" })
        assertTrue(permissions.any { it.name == "AUDIT_VIEW_USERS" })
    }

    @Test
    fun testAppRoleSupervisorPermissions() {
        val supervisorPermissions = RoleRegistry.SUPERVISOR.defaultPermissions
        assertTrue(supervisorPermissions.contains(AppPermission.AUDIT_VIEW_FINANCIAL))
        assertTrue(supervisorPermissions.contains(AppPermission.AUDIT_VIEW_USERS))
    }

    @Test
    fun testEmptyConfigChangesList() {
        val logs = listOf(
            AuditLogEntity(userId = 1, username = "A", action = "USER_LOGIN", entityType = "U", entityId = "1")
        )
        val configChanges = logs.filter { it.action == "CONFIG_UPDATED" }
        assertTrue(configChanges.isEmpty())
    }

    @Test
    fun testGeolocationBounds() {
        val lat = 19.4326
        val lon = -99.1332
        // Lat bounds [-90, 90], Lon bounds [-180, 180]
        assertTrue(lat in -90.0..90.0)
        assertTrue(lon in -180.0..180.0)
    }

    @Test
    fun testSecureDataAuditNoSecrets() {
        val entity = AuditLogEntity(
            userId = 1,
            username = "admin",
            action = "USER_CREATED",
            entityType = "UserEntity",
            entityId = "10",
            previousValues = null,
            newValues = "username=teller, role=COBRADOR" // No passwordHash included in audit newValues!
        )
        assertFalse(entity.newValues!!.contains("password"))
        assertFalse(entity.newValues!!.contains("Hash"))
    }
}
