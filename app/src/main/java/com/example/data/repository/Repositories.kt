package com.example.data.repository
import androidx.room.withTransaction
import androidx.room.withTransaction

import com.example.data.local.*
import com.example.domain.LoanCalculator
import com.example.domain.PlanType
import com.example.domain.ScoreCalculator
import com.example.domain.repositories.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class AuthRepository(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao
) : IAuthRepository {
    override val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    override suspend fun login(username: String, password: String): UserEntity? {
        val user = userDao.getUserByIdentifier(username) ?: return null
        if (!user.active) return null
        if (com.example.core.security.BruteForceManager.isUserLocked(user.failedAttempts, user.lockedUntilMs)) {
            return null
        }
        val isMatch = com.example.core.security.SecurityUtils.verifyPassword(password, user.passwordHash)
        if (isMatch) {
            val updatedUser = user.copy(
                failedAttempts = 0,
                lockedUntilMs = 0L,
                lastLoginMs = System.currentTimeMillis()
            )
            userDao.updateUser(updatedUser)
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    userId = user.id,
                    username = user.username,
                    action = "INICIO_SESION",
                    entityType = "USUARIO",
                    entityId = user.id.toString(),
                    newValues = "Sesión iniciada exitosamente para rol ${user.role}"
                )
            )
            return updatedUser
        } else {
            val newFailed = user.failedAttempts + 1
            val lockUntil = if (newFailed >= com.example.core.security.BruteForceManager.MAX_FAILED_ATTEMPTS) {
                com.example.core.security.BruteForceManager.calculateNewLockoutTime()
            } else 0L
            userDao.updateUser(user.copy(failedAttempts = newFailed, lockedUntilMs = lockUntil))
            return null
        }
    }

    override suspend fun loginWithDetails(
        identifier: String,
        password: String,
        rememberMe: Boolean
    ): com.example.core.error.Result<com.example.core.security.UserSessionState> {
        val user = userDao.getUserByIdentifier(identifier.trim())
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.AuthenticationException("Usuario o contraseña incorrectos")
            )

        if (!user.active) {
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.AuthenticationException("La cuenta de usuario se encuentra inactiva")
            )
        }

        if (com.example.core.security.BruteForceManager.isUserLocked(user.failedAttempts, user.lockedUntilMs)) {
            val seconds = com.example.core.security.BruteForceManager.getRemainingLockoutSeconds(user.lockedUntilMs)
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.AuthenticationException(
                    "Bloqueo temporal por intentos fallidos. Intente de nuevo en $seconds segundos."
                )
            )
        }

        val isValid = com.example.core.security.SecurityUtils.verifyPassword(password, user.passwordHash)
        if (!isValid) {
            val newFailed = user.failedAttempts + 1
            val lockUntil = if (newFailed >= com.example.core.security.BruteForceManager.MAX_FAILED_ATTEMPTS) {
                com.example.core.security.BruteForceManager.calculateNewLockoutTime()
            } else 0L
            userDao.updateUser(user.copy(failedAttempts = newFailed, lockedUntilMs = lockUntil))
            
            val remainingAttempts = com.example.core.security.BruteForceManager.MAX_FAILED_ATTEMPTS - newFailed
            val msg = if (remainingAttempts > 0) {
                "Usuario o contraseña incorrectos. Quedan $remainingAttempts intentos."
            } else {
                "Cuenta bloqueada temporalmente por 60 segundos debido a múltiples intentos fallidos."
            }
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.AuthenticationException(msg)
            )
        }

        // Calculate final permissions
        val roleDef = com.example.core.security.RoleRegistry.getRole(user.role)
        val permissions = mutableSetOf<com.example.core.security.AppPermission>()
        permissions.addAll(roleDef.defaultPermissions)

        if (user.customPermissions.isNotBlank()) {
            user.customPermissions.split(",").forEach { pCodeRaw ->
                val pCode = pCodeRaw.trim()
                if (pCode.startsWith("-")) {
                    val perm = com.example.core.security.AppPermission.fromCode(pCode.drop(1))
                    if (perm != null) permissions.remove(perm)
                } else if (pCode.startsWith("+")) {
                    val perm = com.example.core.security.AppPermission.fromCode(pCode.drop(1))
                    if (perm != null) permissions.add(perm)
                } else {
                    val perm = com.example.core.security.AppPermission.fromCode(pCode)
                    if (perm != null) permissions.add(perm)
                }
            }
        }

        // Automatic hash upgrade if legacy plain text
        val newHash = if (!user.passwordHash.startsWith("pbkdf2:")) {
            com.example.core.security.SecurityUtils.hashPassword(password)
        } else user.passwordHash

        val updatedUser = user.copy(
            passwordHash = newHash,
            failedAttempts = 0,
            lockedUntilMs = 0L,
            lastLoginMs = System.currentTimeMillis()
        )
        userDao.updateUser(updatedUser)

        val accessToken = com.example.core.security.JwtTokenManager.generateAccessToken(
            userId = user.id,
            username = user.username,
            email = user.email,
            role = user.role,
            permissions = permissions
        )
        val refreshToken = com.example.core.security.JwtTokenManager.generateRefreshToken(user.id)

        com.example.core.security.SessionManager.startSession(
            userId = user.id,
            username = user.username,
            fullName = user.fullName,
            email = user.email,
            role = user.role,
            zone = user.assignedZone,
            permissions = permissions,
            accessToken = accessToken,
            refreshToken = refreshToken,
            rememberMe = rememberMe
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = user.username,
                action = "INICIO_SESION",
                entityType = "USUARIO",
                entityId = user.id.toString(),
                newValues = "Sesión iniciada exitosamente con JWT Token"
            )
        )

        return com.example.core.error.Result.Success(com.example.core.security.SessionManager.sessionState.value)
    }

    override suspend fun changePassword(
        userId: Long,
        oldPass: String,
        newPass: String
    ): com.example.core.error.Result<Boolean> {
        val user = userDao.getUserById(userId)
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Usuario no encontrado")
            )

        if (!com.example.core.security.SecurityUtils.verifyPassword(oldPass, user.passwordHash)) {
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("La contraseña actual no es correcta")
            )
        }

        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=_!*]).{8,}\$".toRegex()
        if (!passwordRegex.matches(newPass)) {
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("La nueva contraseña debe tener al menos 8 caracteres, mayúsculas, minúsculas, un número y un carácter especial")
            )
        }

        val newHash = com.example.core.security.SecurityUtils.hashPassword(newPass)
        userDao.updateUser(user.copy(passwordHash = newHash))

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = user.username,
                action = "CAMBIO_CONTRASENA",
                entityType = "USUARIO",
                entityId = user.id.toString(),
                newValues = "Contraseña de usuario actualizada exitosamente"
            )
        )
        return com.example.core.error.Result.Success(true)
    }

    override suspend fun requestPasswordReset(identifier: String): com.example.core.error.Result<String> {
        val user = userDao.getUserByIdentifier(identifier.trim())
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Si la cuenta existe, se enviarán las instrucciones correspondientes.")
            )

        val resetCode = (100000..999999).random().toString()
        val expiry = System.currentTimeMillis() + 15 * 60 * 1000L // 15 mins

        userDao.updateUser(user.copy(passwordResetToken = resetCode, passwordResetExpiryMs = expiry))

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = user.username,
                action = "RECUPERACION_CONTRASENA",
                entityType = "USUARIO",
                entityId = user.id.toString(),
                newValues = "Código de recuperación generado: $resetCode"
            )
        )
        return com.example.core.error.Result.Success(resetCode)
    }

    override suspend fun resetPasswordWithToken(
        token: String,
        newPass: String
    ): com.example.core.error.Result<Boolean> {
        val user = userDao.getUserByResetToken(token.trim())
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Código de verificación inválido")
            )

        if (System.currentTimeMillis() > user.passwordResetExpiryMs) {
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("El código de verificación ha expirado")
            )
        }

        if (newPass.length < 6) {
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("La nueva contraseña debe tener al menos 6 caracteres")
            )
        }

        val newHash = com.example.core.security.SecurityUtils.hashPassword(newPass)
        userDao.updateUser(user.copy(passwordHash = newHash, passwordResetToken = null, passwordResetExpiryMs = 0L))

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = user.username,
                action = "RESTABLECER_CONTRASENA",
                entityType = "USUARIO",
                entityId = user.id.toString(),
                newValues = "Contraseña restablecida exitosamente mediante código de recuperación"
            )
        )
        return com.example.core.error.Result.Success(true)
    }

    override suspend fun updateUserPermissions(
        userId: Long,
        permissions: Set<com.example.core.security.AppPermission>
    ): com.example.core.error.Result<Boolean> {
        val user = userDao.getUserById(userId)
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Usuario no encontrado")
            )

        val oldPermissions = user.customPermissions
        
        val roleDef = com.example.core.security.RoleRegistry.getRole(user.role)
        val defaultPerms = roleDef.defaultPermissions
        
        val customStrings = mutableListOf<String>()
        
        // Find denied
        defaultPerms.forEach { dp ->
            if (!permissions.contains(dp)) {
                customStrings.add("-${dp.code}")
            }
        }
        
        // Find explicitly allowed
        permissions.forEach { p ->
            if (!defaultPerms.contains(p)) {
                customStrings.add("+${p.code}")
            }
        }
        
        val permStr = customStrings.joinToString(",")
        
        userDao.updateUser(user.copy(customPermissions = permStr))
        
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = user.username,
                action = "ASIGNAR_PERMISOS",
                entityType = "USUARIO",
                entityId = user.id.toString(),
                previousValues = "Permisos: $oldPermissions",
                newValues = "Permisos asignados: $permStr"
            )
        )
        return com.example.core.error.Result.Success(true)
    }

    override suspend fun createUser(user: UserEntity): Long {
        val hashedUser = if (!user.passwordHash.startsWith("pbkdf2:")) {
            user.copy(passwordHash = com.example.core.security.SecurityUtils.hashPassword(user.passwordHash))
        } else user
        return userDao.insertUser(hashedUser)
    }

    override suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    override suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)

    override suspend fun adminCreateUser(
        user: UserEntity,
        adminUsername: String
    ): com.example.core.error.Result<Long> {
        val existing = userDao.getUserByUsername(user.username.trim())
        if (existing != null) {
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("El nombre de usuario '${user.username}' ya se encuentra registrado")
            )
        }

        val hashedPass = com.example.core.security.SecurityUtils.hashPassword(user.passwordHash.ifBlank { "123456" })
        val userToInsert = user.copy(
            username = user.username.trim(),
            passwordHash = hashedPass,
            createdAtMs = System.currentTimeMillis()
        )

        val newId = userDao.insertUser(userToInsert)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = newId,
                username = adminUsername,
                action = "CREAR_USUARIO",
                entityType = "USUARIO",
                entityId = newId.toString(),
                previousValues = "N/A",
                newValues = "Usuario creado: ${userToInsert.username}, Rol: ${userToInsert.role}, Sucursal: ${userToInsert.branch}, Ruta: ${userToInsert.route}, Fondo: ${userToInsert.fund}"
            )
        )

        return com.example.core.error.Result.Success(newId)
    }

    override suspend fun adminUpdateUser(
        user: UserEntity,
        adminUsername: String
    ): com.example.core.error.Result<Boolean> {
        val oldUser = userDao.getUserById(user.id)
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Usuario no encontrado")
            )

        if (oldUser.role == "ADMINISTRADOR" && oldUser.active) {
            if (user.role != "ADMINISTRADOR" || !user.active) {
                val adminCount = userDao.getActiveAdminCount()
                if (adminCount <= 1) {
                    return com.example.core.error.Result.Error(
                        com.example.core.error.AppException.ValidationException("No se puede modificar o desactivar el último administrador activo.")
                    )
                }
            }
        }

        userDao.updateUser(user)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = adminUsername,
                action = "EDITAR_USUARIO",
                entityType = "USUARIO",
                entityId = user.id.toString(),
                previousValues = "Rol: ${oldUser.role}, Nombre: ${oldUser.fullName}, Tel: ${oldUser.phone}, Email: ${oldUser.email}, Sucursal: ${oldUser.branch}, Ruta: ${oldUser.route}, Fondo: ${oldUser.fund}, Sup: ${oldUser.supervisorName}",
                newValues = "Rol: ${user.role}, Nombre: ${user.fullName}, Tel: ${user.phone}, Email: ${user.email}, Sucursal: ${user.branch}, Ruta: ${user.route}, Fondo: ${user.fund}, Sup: ${user.supervisorName}"
            )
        )

        return com.example.core.error.Result.Success(true)
    }

    override suspend fun adminSetUserStatus(
        userId: Long,
        active: Boolean,
        adminUsername: String
    ): com.example.core.error.Result<Boolean> {
        val user = userDao.getUserById(userId)
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Usuario no encontrado")
            )

        if (!active && user.role == "ADMINISTRADOR" && user.active) {
            val adminCount = userDao.getActiveAdminCount()
            if (adminCount <= 1) {
                return com.example.core.error.Result.Error(
                    com.example.core.error.AppException.ValidationException("No se puede desactivar el último administrador activo.")
                )
            }
        }

        val updated = user.copy(active = active)
        userDao.updateUser(updated)

        val actionName = if (active) "REACTIVAR_USUARIO" else "DESACTIVAR_USUARIO"
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = adminUsername,
                action = actionName,
                entityType = "USUARIO",
                entityId = user.id.toString(),
                previousValues = "Estado activo: ${user.active}",
                newValues = "Estado activo: $active"
            )
        )

        return com.example.core.error.Result.Success(true)
    }

    override suspend fun adminSetUserLock(
        userId: Long,
        locked: Boolean,
        adminUsername: String
    ): com.example.core.error.Result<Boolean> {
        val user = userDao.getUserById(userId)
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Usuario no encontrado")
            )

        val newLockTime = if (locked) System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000) else 0L
        val newFailed = if (locked) com.example.core.security.BruteForceManager.MAX_FAILED_ATTEMPTS else 0
        val updated = user.copy(failedAttempts = newFailed, lockedUntilMs = newLockTime)
        userDao.updateUser(updated)

        val actionName = if (locked) "BLOQUEAR_USUARIO" else "DESBLOQUEAR_USUARIO"
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = adminUsername,
                action = actionName,
                entityType = "USUARIO",
                entityId = user.id.toString(),
                previousValues = "Bloqueado: ${com.example.core.security.BruteForceManager.isUserLocked(user.failedAttempts, user.lockedUntilMs)}",
                newValues = "Bloqueado: $locked"
            )
        )

        return com.example.core.error.Result.Success(true)
    }

    override suspend fun adminResetUserPassword(
        userId: Long,
        newPassword: String,
        adminUsername: String
    ): com.example.core.error.Result<Boolean> {
        val user = userDao.getUserById(userId)
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Usuario no encontrado")
            )

        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=_!*]).{8,}\$".toRegex()
        if (!passwordRegex.matches(newPassword)) {
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("La nueva contraseña debe tener al menos 8 caracteres, mayúsculas, minúsculas, un número y un carácter especial")
            )
        }

        val hashedPass = com.example.core.security.SecurityUtils.hashPassword(newPassword)
        val updated = user.copy(passwordHash = hashedPass, failedAttempts = 0, lockedUntilMs = 0L, passwordResetToken = null, passwordResetExpiryMs = 0L)
        userDao.updateUser(updated)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = adminUsername,
                action = "RESTABLECER_CONTRASENA_ADMIN",
                entityType = "USUARIO",
                entityId = user.id.toString(),
                previousValues = "Contraseña anterior hash",
                newValues = "Contraseña restablecida por Administrador ($adminUsername)"
            )
        )

        return com.example.core.error.Result.Success(true)
    }

    override suspend fun adminDeleteUser(
        userId: Long,
        adminUsername: String
    ): com.example.core.error.Result<Boolean> {
        val user = userDao.getUserById(userId)
            ?: return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("Usuario no encontrado")
            )

        if (user.role.equals("ADMINISTRADOR", ignoreCase = true) && user.username.equals("admin", ignoreCase = true)) {
            return com.example.core.error.Result.Error(
                com.example.core.error.AppException.ValidationException("No se puede eliminar la cuenta de Administrador Principal del sistema")
            )
        }

        if (user.role == "ADMINISTRADOR" && user.active) {
            val adminCount = userDao.getActiveAdminCount()
            if (adminCount <= 1) {
                return com.example.core.error.Result.Error(
                    com.example.core.error.AppException.ValidationException("No se puede eliminar el último administrador activo.")
                )
            }
        }

        userDao.deleteUser(user)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = adminUsername,
                action = "ELIMINAR_USUARIO",
                entityType = "USUARIO",
                entityId = user.id.toString(),
                previousValues = "Usuario: ${user.username}, Rol: ${user.role}",
                newValues = "Usuario eliminado permanentemente por $adminUsername"
            )
        )

        return com.example.core.error.Result.Success(true)
    }
}

class ClientRepository(
    private val clientDao: ClientDao,
    private val auditLogDao: AuditLogDao
) : IClientRepository {
    override val allClients: Flow<List<ClientEntity>> = clientDao.getAllClients()
    override val activeClientsCount: Flow<Int> = clientDao.getActiveClientsCount()

    override fun searchClients(query: String): Flow<List<ClientEntity>> {
        return if (query.isBlank()) clientDao.getAllClients() else clientDao.searchClients(query)
    }

    override suspend fun getClientById(id: Long): ClientEntity? = clientDao.getClientById(id)

    override suspend fun insertClient(client: ClientEntity, currentUser: String): Long {
        val id = clientDao.insertClient(client)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = "CREAR_CLIENTE",
                entityType = "CLIENTE",
                entityId = id.toString(),
                newValues = "Cliente registrado: ${client.fullName}, CURP: ${client.curp}"
            )
        )
        return id
    }

    override suspend fun updateClient(client: ClientEntity, currentUser: String) {
        clientDao.updateClient(client)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = "ACTUALIZAR_CLIENTE",
                entityType = "CLIENTE",
                entityId = client.id.toString(),
                newValues = "Cliente actualizado: ${client.fullName}"
            )
        )
    }

    override suspend fun deleteClient(client: ClientEntity, currentUser: String) {
        clientDao.deleteClient(client)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = "ELIMINAR_CLIENTE",
                entityType = "CLIENTE",
                entityId = client.id.toString(),
                previousValues = "Cliente eliminado: ${client.fullName}"
            )
        )
    }

    override fun getClientReferences(clientId: Long): Flow<List<ClientReferenceEntity>> =
        clientDao.getClientReferences(clientId)

    override suspend fun insertClientReference(reference: ClientReferenceEntity): Long {
        val id = clientDao.insertClientReference(reference)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = reference.createdBy,
                action = "AGREGAR_REFERENCIA",
                entityType = "CLIENTE",
                entityId = reference.clientId.toString(),
                newValues = "Referencia agregada: ${reference.name} (${reference.type})"
            )
        )
        return id
    }

    override suspend fun deleteClientReference(reference: ClientReferenceEntity) {
        clientDao.deleteClientReference(reference)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = "SISTEMA", // Or pass user
                action = "ELIMINAR_REFERENCIA",
                entityType = "CLIENTE",
                entityId = reference.clientId.toString(),
                previousValues = "Referencia eliminada: ${reference.name}"
            )
        )
    }

    override fun getClientObservations(clientId: Long): Flow<List<ClientObservationEntity>> =
        clientDao.getClientObservations(clientId)

    override suspend fun insertClientObservation(observation: ClientObservationEntity): Long {
        val id = clientDao.insertClientObservation(observation)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = observation.createdBy,
                action = "AGREGAR_OBSERVACION",
                entityType = "CLIENTE",
                entityId = observation.clientId.toString(),
                newValues = "Observación agregada: ${observation.text}"
            )
        )
        return id
    }

    override fun getClientAlerts(clientId: Long): Flow<List<ClientAlertEntity>> =
        clientDao.getClientAlerts(clientId)

    override suspend fun insertClientAlert(alert: ClientAlertEntity): Long {
        val id = clientDao.insertClientAlert(alert)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = alert.createdBy,
                action = "AGREGAR_ALERTA",
                entityType = "CLIENTE",
                entityId = alert.clientId.toString(),
                newValues = "Alerta agregada: ${alert.title}"
            )
        )
        return id
    }

    override suspend fun updateClientAlert(alert: ClientAlertEntity) {
        clientDao.updateClientAlert(alert)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = alert.createdBy,
                action = "ACTUALIZAR_ALERTA",
                entityType = "CLIENTE",
                entityId = alert.clientId.toString(),
                newValues = "Alerta actualizada: ${alert.title} a estado ${alert.status}"
            )
        )
    }
}

class LoanRepository(
    private val database: AppDatabase,
    private val loanDao: LoanDao,
    private val installmentDao: InstallmentDao,
    private val lateFeeHistoryDao: LateFeeHistoryDao,
    private val skippedPaymentLogDao: SkippedPaymentLogDao,
    private val paymentDao: PaymentDao,
    private val cashRegisterDao: CashRegisterDao,
    private val cashMovementDao: CashMovementDao,
    private val clientDao: ClientDao,
    private val auditLogDao: AuditLogDao,
    private val configDao: SystemConfigDao
) : ILoanRepository {
    override val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()
    override val activeLoans: Flow<List<LoanEntity>> = loanDao.getActiveLoans()
    override val activeLoansCount: Flow<Int> = loanDao.getActiveLoansCount()
    override val totalActiveCapital: Flow<Double?> = loanDao.getTotalActiveCapital()
    override val totalRecoveredCapital: Flow<Double?> = loanDao.getTotalRecoveredCapital()
    override val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    override fun getLoansByClient(clientId: Long): Flow<List<LoanEntity>> = loanDao.getLoansByClient(clientId)

    override suspend fun getLoansByClientSync(clientId: Long): List<LoanEntity> = loanDao.getLoansByClientSync(clientId)
    override suspend fun getLoanById(id: Long): LoanEntity? = loanDao.getLoanById(id)

    override fun getInstallmentsByLoan(loanId: Long): Flow<List<InstallmentEntity>> =
        installmentDao.getInstallmentsByLoan(loanId)

    override suspend fun getInstallmentsByLoanSync(loanId: Long): List<InstallmentEntity> =
        installmentDao.getInstallmentsByLoanSync(loanId)

    override suspend fun createLoan(
        clientId: Long,
        capital: Double,
        planTypeCode: String,
        skipSundays: Boolean,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        currentUserRole: String,
        isRenewal: Boolean,
        previousLoanId: Long?
    ): Long {
        val client = clientDao.getClientById(clientId)
            ?: throw IllegalStateException("Cliente no encontrado.")

        if (client.status == "BLOQUEADO" || client.status == "LISTA_NEGRA" || client.status == "INACTIVO") {
            throw IllegalStateException("No se puede crear préstamo: El cliente se encuentra en estado ${client.status}.")
        }

        val config = configDao.getConfigSync()
        if (config != null && !config.allowMultipleLoans && !isRenewal) {
            val activeCount = loanDao.getActiveLoansCountByClientSync(clientId)
            if (activeCount > 0) {
                throw IllegalStateException("El cliente ya cuenta con un préstamo activo y la configuración del sistema no permite múltiples préstamos.")
            }
        }

        val maxStandard = config?.maxLoanAmountStandard ?: 10000.0
        val maxSupervisor = config?.maxLoanAmountSupervisor ?: 25000.0

        val initialStatus = when {
            capital <= maxStandard -> "AUTORIZADO"
            capital <= maxSupervisor -> {
                if (currentUserRole == "SUPERVISOR" || currentUserRole == "ADMINISTRADOR") "AUTORIZADO"
                else "PENDIENTE"
            }
            else -> {
                if (currentUserRole == "ADMINISTRADOR") "AUTORIZADO"
                else "PENDIENTE"
            }
        }

        val calc = LoanCalculator.calculateLoan(capital, planTypeCode, config?.customLoanPlansJson)
        val now = System.currentTimeMillis()
        val maturityDate = LoanCalculator.calculateExpectedMaturityDate(now, calc.totalDays, skipSundays)

        val loan = LoanEntity(
            clientId = clientId,
            clientName = client.fullName,
            clientCurp = client.curp,
            collectorId = collectorId,
            collectorName = collectorName,
            planType = planTypeCode,
            capital = capital,
            interestRate = calc.interestRate,
            interestAmount = calc.interestAmount,
            totalAmount = calc.totalAmount,
            dailyPayment = calc.dailyPayment,
            paidAmount = 0.0,
            remainingBalance = calc.totalAmount,
            skipSundays = skipSundays,
            disbursementDate = if (initialStatus == "ACTIVO") now else 0L,
            startDate = if (initialStatus == "ACTIVO") now else 0L,
            expectedEndDate = maturityDate,
            status = initialStatus,
            isRenewal = isRenewal,
            previousLoanId = previousLoanId,
            lateFeePercentage = config?.lateFeePercentage ?: 0.05,
            lateFeeType = config?.lateFeeType ?: "PORCENTAJE_CUOTA"
        )

        val loanId = loanDao.insertLoan(loan)

        if (initialStatus == "ACTIVO") {
            val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
            if (activeCash != null && (activeCash.expectedCash - capital) < 0.0) {
                // Should not happen as UI should prevent it, but enforce business rule
                // Assuming "permitir desembolsos" has no explicit config yet, we strictly protect against negative cash
                throw IllegalStateException("El desembolso de $$capital excede el saldo de caja disponible ($${activeCash.expectedCash}).")
            }

            val installments = LoanCalculator.generateInstallments(
                loanId = loanId,
                clientId = clientId,
                capital = capital,
                interestAmount = calc.interestAmount,
                totalAmount = calc.totalAmount,
                dailyPayment = calc.dailyPayment,
                totalDays = calc.totalDays,
                startDateMs = now,
                skipSundays = skipSundays
            )
            installmentDao.insertInstallments(installments)

            if (activeCash != null) {
                val updatedOutflows = activeCash.outflows + capital
                val updatedExpected = activeCash.initialCash + activeCash.cashInflows - updatedOutflows
                cashRegisterDao.updateCashRegister(
                    activeCash.copy(
                        outflows = updatedOutflows,
                        expectedCash = updatedExpected
                    )
                )
                cashMovementDao.insertCashMovement(
                    CashMovementEntity(
                        cashRegisterId = activeCash.id,
                        type = "EGRESO_PRESTAMO",
                        amount = capital,
                        concept = "Desembolso Préstamo #$loanId para ${client.fullName}",
                        registeredBy = currentUser
                    )
                )
            }
        }

        if (isRenewal && previousLoanId != null) {
            val prev = loanDao.getLoanById(previousLoanId)
            if (prev != null) {
                loanDao.updateLoan(prev.copy(status = "RENOVADO", remainingBalance = 0.0))
            }
        }

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = if (isRenewal) "RENOVACION_PRESTAMO" else "CREAR_PRESTAMO",
                entityType = "PRESTAMO",
                entityId = loanId.toString(),
                newValues = "Préstamo registrado: $capital MXN en $planTypeCode para ${client.fullName} [Estado: $initialStatus]"
            )
        )

        return loanId
    }

    override suspend fun editLoanBeforeDisbursement(
        loanId: Long,
        newCapital: Double,
        newPlanTypeCode: String,
        newSkipSundays: Boolean,
        currentUser: String,
        currentUserRole: String
    ): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        if (loan.status != "PENDIENTE" && loan.status != "AUTORIZADO") {
            throw IllegalStateException("No se puede editar un préstamo que ya fue desembolsado o procesado.")
        }

        val config = configDao.getConfigSync()
        val maxStandard = config?.maxLoanAmountStandard ?: 10000.0
        val maxSupervisor = config?.maxLoanAmountSupervisor ?: 25000.0

        val newStatus = when {
            newCapital <= maxStandard -> "AUTORIZADO"
            newCapital <= maxSupervisor -> {
                if (currentUserRole == "SUPERVISOR" || currentUserRole == "ADMINISTRADOR") "AUTORIZADO"
                else "PENDIENTE"
            }
            else -> {
                if (currentUserRole == "ADMINISTRADOR") "AUTORIZADO"
                else "PENDIENTE"
            }
        }

        val calc = LoanCalculator.calculateLoan(newCapital, newPlanTypeCode, config?.customLoanPlansJson)
        val now = System.currentTimeMillis()
        val maturityDate = LoanCalculator.calculateExpectedMaturityDate(now, calc.totalDays, newSkipSundays)

        val updated = loan.copy(
            capital = newCapital,
            planType = newPlanTypeCode,
            interestRate = calc.interestRate,
            interestAmount = calc.interestAmount,
            totalAmount = calc.totalAmount,
            dailyPayment = calc.dailyPayment,
            remainingBalance = calc.totalAmount,
            skipSundays = newSkipSundays,
            expectedEndDate = maturityDate,
            status = newStatus
        )

        loanDao.updateLoan(updated)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = "EDITAR_PRESTAMO_PRE_DESEMBOLSO",
                entityType = "PRESTAMO",
                entityId = loanId.toString(),
                previousValues = "Capital previo: ${loan.capital}, Plan: ${loan.planType}",
                newValues = "Nuevo capital: $newCapital, Plan: $newPlanTypeCode, Estado: $newStatus"
            )
        )

        return true
    }

    override suspend fun cancelLoanBeforeDisbursement(
        loanId: Long,
        currentUser: String,
        reason: String
    ): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        if (loan.status != "PENDIENTE" && loan.status != "AUTORIZADO") {
            throw IllegalStateException("No se puede cancelar un préstamo que ya ha sido desembolsado.")
        }

        val updated = loan.copy(status = "CANCELADO")
        loanDao.updateLoan(updated)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = "CANCELAR_PRESTAMO",
                entityType = "PRESTAMO",
                entityId = loanId.toString(),
                previousValues = "Estado previo: ${loan.status}",
                newValues = "Estado: CANCELADO. Motivo: $reason"
            )
        )

        return true
    }

    override suspend fun authorizeLoan(
        loanId: Long,
        currentUser: String,
        currentUserRole: String
    ): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        if (loan.status != "PENDIENTE") {
            throw IllegalStateException("El préstamo no se encuentra en estado Pendiente.")
        }

        val config = configDao.getConfigSync()
        val maxSupervisor = config?.maxLoanAmountSupervisor ?: 25000.0

        if (loan.capital > maxSupervisor && currentUserRole != "ADMINISTRADOR") {
            throw IllegalStateException("Préstamos mayores a $$maxSupervisor MXN requieren autorización exclusiva de Administrador.")
        }

        val updated = loan.copy(status = "AUTORIZADO")
        loanDao.updateLoan(updated)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = "AUTORIZAR_PRESTAMO",
                entityType = "PRESTAMO",
                entityId = loanId.toString(),
                newValues = "Préstamo #$loanId autorizado por $currentUser ($currentUserRole)"
            )
        )

        return true
    }

    override suspend fun disburseLoan(
        loanId: Long,
        collectorId: Long,
        collectorName: String,
        currentUser: String
    ): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        if (loan.status != "AUTORIZADO" && loan.status != "PENDIENTE") {
            throw IllegalStateException("Sólo préstamos Autorizados o Pendientes pueden ser desembolsados.")
        }

        val config = configDao.getConfigSync()
        val calc = LoanCalculator.calculateLoan(loan.capital, loan.planType, config?.customLoanPlansJson)
        val now = System.currentTimeMillis()
        val maturityDate = LoanCalculator.calculateExpectedMaturityDate(now, calc.totalDays, loan.skipSundays)

        val updated = loan.copy(
            status = "ACTIVO",
            disbursementDate = now,
            startDate = now,
            expectedEndDate = maturityDate,
            collectorId = collectorId,
            collectorName = collectorName
        )
        loanDao.updateLoan(updated)

        // Clear existing installments if any and generate fresh ones
        installmentDao.deleteInstallmentsByLoan(loanId)

        val installments = LoanCalculator.generateInstallments(
            loanId = loanId,
            clientId = loan.clientId,
            capital = loan.capital,
            interestAmount = loan.interestAmount,
            totalAmount = loan.totalAmount,
            dailyPayment = loan.dailyPayment,
            totalDays = calc.totalDays,
            startDateMs = now,
            skipSundays = loan.skipSundays
        )
        installmentDao.insertInstallments(installments)

        // Active cash register outflow
        val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
        if (activeCash != null) {
            if ((activeCash.expectedCash - loan.capital) < 0.0) {
                throw IllegalStateException("El desembolso de $${loan.capital} excede el saldo de caja disponible ($${activeCash.expectedCash}).")
            }
            val updatedOutflows = activeCash.outflows + loan.capital
            val updatedExpected = activeCash.initialCash + activeCash.cashInflows - updatedOutflows
            cashRegisterDao.updateCashRegister(
                activeCash.copy(
                    outflows = updatedOutflows,
                    expectedCash = updatedExpected
                )
            )
            cashMovementDao.insertCashMovement(
                CashMovementEntity(
                    cashRegisterId = activeCash.id,
                    type = "EGRESO_PRESTAMO",
                    amount = loan.capital,
                    concept = "Desembolso Préstamo #$loanId para ${loan.clientName}",
                    registeredBy = currentUser
                )
            )
        }

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = "DESEMBOLSAR_PRESTAMO",
                entityType = "PRESTAMO",
                entityId = loanId.toString(),
                newValues = "Préstamo #$loanId desembolsado por ${loan.capital} MXN"
            )
        )

        return true
    }

    override suspend fun renewLoan(
        clientId: Long,
        previousLoanId: Long,
        capital: Double,
        planTypeCode: String,
        skipSundays: Boolean,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        currentUserRole: String
    ): Long {
        return renewPlusLoan(
            clientId = clientId,
            previousLoanId = previousLoanId,
            newCapital = capital,
            extraCapital = 0.0,
            planTypeCode = planTypeCode,
            skipSundays = skipSundays,
            collectorId = collectorId,
            collectorName = collectorName,
            currentUser = currentUser,
            currentUserRole = currentUserRole
        )
    }

    override suspend fun renewPlusLoan(
        clientId: Long,
        previousLoanId: Long,
        newCapital: Double,
        extraCapital: Double,
        planTypeCode: String,
        skipSundays: Boolean,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        currentUserRole: String
    ): Long {
        val prevLoan = loanDao.getLoanById(previousLoanId)
            ?: throw IllegalStateException("Préstamo anterior no encontrado.")

        val client = clientDao.getClientById(clientId)
            ?: throw IllegalStateException("Cliente no encontrado.")

        if (client.status == "BLOQUEADO" || client.status == "LISTA_NEGRA" || client.status == "INACTIVO") {
            throw IllegalStateException("No se puede renovar: El cliente está en estado ${client.status}.")
        }

        val totalCapitalToFinance = newCapital + extraCapital
        val prevRemainingBalance = prevLoan.remainingBalance

        // Net cash out to customer = totalCapitalToFinance - prevRemainingBalance
        val netCashOutflow = (totalCapitalToFinance - prevRemainingBalance).coerceAtLeast(0.0)

        // Mark previous loan as RENOVADO
        loanDao.updateLoan(prevLoan.copy(status = "RENOVADO", remainingBalance = 0.0))

        val config = configDao.getConfigSync()
        val calc = LoanCalculator.calculateLoan(totalCapitalToFinance, planTypeCode, config?.customLoanPlansJson)
        val now = System.currentTimeMillis()
        val maturityDate = LoanCalculator.calculateExpectedMaturityDate(now, calc.totalDays, skipSundays)

        val newLoan = LoanEntity(
            clientId = clientId,
            clientName = client.fullName,
            clientCurp = client.curp,
            collectorId = collectorId,
            collectorName = collectorName,
            planType = planTypeCode,
            capital = totalCapitalToFinance,
            interestRate = calc.interestRate,
            interestAmount = calc.interestAmount,
            totalAmount = calc.totalAmount,
            dailyPayment = calc.dailyPayment,
            paidAmount = 0.0,
            remainingBalance = calc.totalAmount,
            skipSundays = skipSundays,
            disbursementDate = now,
            startDate = now,
            expectedEndDate = maturityDate,
            status = "ACTIVO",
            isRenewal = true,
            previousLoanId = previousLoanId
        )

        val newLoanId = loanDao.insertLoan(newLoan)

        val installments = LoanCalculator.generateInstallments(
            loanId = newLoanId,
            clientId = clientId,
            capital = totalCapitalToFinance,
            interestAmount = calc.interestAmount,
            totalAmount = calc.totalAmount,
            dailyPayment = calc.dailyPayment,
            totalDays = calc.totalDays,
            startDateMs = now,
            skipSundays = skipSundays
        )
        installmentDao.insertInstallments(installments)

        // Outflow net cash disbursed
        if (netCashOutflow > 0.0) {
            val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
            if (activeCash != null) {
                if ((activeCash.expectedCash - netCashOutflow) < 0.0) {
                    throw IllegalStateException("El efectivo neto a entregar ($$netCashOutflow) excede el saldo de caja disponible ($${activeCash.expectedCash}).")
                }
                val updatedOutflows = activeCash.outflows + netCashOutflow
                val updatedExpected = activeCash.initialCash + activeCash.cashInflows - updatedOutflows
                cashRegisterDao.updateCashRegister(
                    activeCash.copy(
                        outflows = updatedOutflows,
                        expectedCash = updatedExpected
                    )
                )
                cashMovementDao.insertCashMovement(
                    CashMovementEntity(
                        cashRegisterId = activeCash.id,
                        type = "EGRESO_PRESTAMO",
                        amount = netCashOutflow,
                        concept = "Renovación Préstamo #$previousLoanId a #$newLoanId (Efectivo neto entregado: $$netCashOutflow)",
                        registeredBy = currentUser
                    )
                )
            }
        }

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = currentUser,
                action = "RENOVACION_PRESTAMO",
                entityType = "PRESTAMO",
                entityId = newLoanId.toString(),
                previousValues = "Préstamo previo #$previousLoanId saldo abonado",
                newValues = "Renovación exitosa. Nuevo Préstamo #$newLoanId por $totalCapitalToFinance MXN"
            )
        )

        return newLoanId
    }

    override suspend fun registerPayment(
        loanId: Long,
        amount: Double,
        method: String,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        notes: String
    ): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        val now = System.currentTimeMillis()
        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(8)}"

        // Record payment entity
        val payment = PaymentEntity(
            loanId = loanId,
            clientId = loan.clientId,
            clientName = loan.clientName,
            collectorId = collectorId,
            collectorName = collectorName,
            amount = amount,
            method = method,
            paymentDate = now,
            receiptNumber = receiptNo,
            notes = notes
        )
        paymentDao.insertPayment(payment)

        // Apply payment across unpaid installments sequentially
        val unpaidInstallments = installmentDao.getUnpaidInstallmentsForLoan(loanId)
        var remainingPayment = amount

        for (inst in unpaidInstallments) {
            if (remainingPayment <= 0.0) break

            val currentRemainingInst = inst.remainingAmount
            val lateFee = if (inst.status == "VENCIDO") inst.targetAmount * 0.05 else 0.0
            val totalNeeded = currentRemainingInst + lateFee

            if (remainingPayment >= totalNeeded) {
                remainingPayment -= totalNeeded
                val updatedInst = inst.copy(
                    paidAmount = inst.targetAmount,
                    remainingAmount = 0.0,
                    lateFee = 0.0,
                    status = "PAGADO",
                    lastPaymentDate = now
                )
                installmentDao.updateInstallment(updatedInst)
            } else {
                val newPaid = inst.paidAmount + remainingPayment
                val newRemaining = (inst.targetAmount - newPaid).coerceAtLeast(0.0)
                val updatedInst = inst.copy(
                    paidAmount = newPaid,
                    remainingAmount = newRemaining,
                    status = if (newRemaining <= 0.0) "PAGADO" else "PARCIAL",
                    lastPaymentDate = now
                )
                installmentDao.updateInstallment(updatedInst)
                remainingPayment = 0.0
            }
        }

        // Update Loan totals
        val newLoanPaid = loan.paidAmount + amount
        val newRemainingBalance = (loan.totalAmount - newLoanPaid).coerceAtLeast(0.0)
        val newStatus = if (newRemainingBalance <= 0.0) "LIQUIDADO" else "ACTIVO"

        loanDao.updateLoan(
            loan.copy(
                paidAmount = newLoanPaid,
                remainingBalance = newRemainingBalance,
                status = newStatus
            )
        )

        // Update active cash register
        val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
        if (activeCash != null) {
            val newCashInflows = if (method == "EFECTIVO") activeCash.cashInflows + amount else activeCash.cashInflows
            val newTransferInflows = if (method == "TRANSFERENCIA") activeCash.transferInflows + amount else activeCash.transferInflows
            val newExpectedCash = activeCash.initialCash + newCashInflows - activeCash.outflows

            cashRegisterDao.updateCashRegister(
                activeCash.copy(
                    cashInflows = newCashInflows,
                    transferInflows = newTransferInflows,
                    expectedCash = newExpectedCash
                )
            )

            cashMovementDao.insertCashMovement(
                CashMovementEntity(
                    cashRegisterId = activeCash.id,
                    type = if (method == "EFECTIVO") "INGRESO_COBRO_EFECTIVO" else "INGRESO_COBRO_TRANSFERENCIA",
                    amount = amount,
                    concept = "Cobro $method Préstamo #$loanId - Recibo $receiptNo",
                    registeredBy = currentUser
                )
            )
        }

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = collectorId,
                username = currentUser,
                action = "REGISTRO_PAGO",
                entityType = "PAGO",
                entityId = receiptNo,
                newValues = "Pago de $amount MXN ($method) para Préstamo #$loanId. Recibo: $receiptNo"
            )
        )

        return true
    }

    override fun getLateFeeHistoryForLoan(loanId: Long): Flow<List<LateFeeHistoryEntity>> =
        lateFeeHistoryDao.getLateFeeHistoryForLoan(loanId)

    override fun getSkippedPaymentsForLoan(loanId: Long): Flow<List<SkippedPaymentLogEntity>> =
        skippedPaymentLogDao.getSkippedPaymentsForLoan(loanId)

    override suspend fun recordSkippedPayment(
        loanId: Long,
        installmentId: Long,
        reason: String,
        collectorName: String
    ): Boolean {
        val inst = installmentDao.getInstallmentById(installmentId) ?: return false
        val updatedInst = inst.copy(
            isSkipped = true,
            skippedReason = reason,
            status = if (inst.status == "PENDIENTE") "VENCIDO" else inst.status
        )
        installmentDao.updateInstallment(updatedInst)

        skippedPaymentLogDao.insertSkippedPaymentLog(
            SkippedPaymentLogEntity(
                loanId = loanId,
                installmentId = installmentId,
                installmentNumber = inst.installmentNumber,
                collectorName = collectorName,
                reason = reason
            )
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 0,
                username = collectorName,
                action = "PAGO_OMITIDO",
                entityType = "CUOTA",
                entityId = installmentId.toString(),
                newValues = "Cuota #${inst.installmentNumber} omitida. Razón: $reason"
            )
        )
        return true
    }

    override suspend fun evaluateAndApplyLateFees(loanId: Long): Boolean {
        val loan = loanDao.getLoanById(loanId) ?: return false
        val installments = installmentDao.getInstallmentsByLoanSync(loanId)
        if (installments.isEmpty()) return false

        val (updatedInsts, newLateFeeLogs) = LoanCalculator.evaluateLateFees(
            installments = installments,
            lateFeeType = loan.lateFeeType,
            lateFeeValue = loan.lateFeePercentage
        )

        installmentDao.insertInstallments(updatedInsts)
        for (log in newLateFeeLogs) {
            lateFeeHistoryDao.insertLateFeeHistory(log)
        }
        return true
    }

    override suspend fun registerSmartPayment(
        loanId: Long,
        amount: Double,
        method: String,
        collectorId: Long,
        collectorName: String,
        currentUser: String,
        notes: String,
        paymentType: String,
        proofPhotoUri: String?,
        redistributeAdvance: Boolean,
        targetInstallmentIds: List<Long>
    ): Boolean {
        return database.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: return@withTransaction false
        if (loan.status == "LIQUIDADO" || loan.status == "CANCELADO") return@withTransaction false
        if (amount <= 0.0) return@withTransaction false

        val now = System.currentTimeMillis()
        
        // Anti-double-payment guard
        val recentPayments = paymentDao.getRecentPaymentsByLoanSync(loanId)
        val duplicate = recentPayments.firstOrNull { it.amount == amount && (now - it.paymentDate) < 15000 }
        if (duplicate != null) {
            // Already registered recently to avoid double tap
            return@withTransaction true
        }

        val receiptNo = "REC-${System.currentTimeMillis().toString().takeLast(8)}"
        val installments = installmentDao.getInstallmentsByLoanSync(loanId)

        // Calculate updated installments schedule
        val updatedInstallments = LoanCalculator.redistributeScheduleOnAdvance(
            loan = loan,
            installments = installments,
            paymentAmount = amount,
            redistributeRemaining = redistributeAdvance,
            nowMs = now
        )

        // Count how many installments changed state to PAGADO or PARCIAL
        val newlyPaidCount = updatedInstallments.count { newInst ->
            val oldInst = installments.find { it.id == newInst.id }
            oldInst?.status != "PAGADO" && newInst.status == "PAGADO"
        }.coerceAtLeast(1)

        // Record payment entity
        val payment = PaymentEntity(
            loanId = loanId,
            clientId = loan.clientId,
            clientName = loan.clientName,
            collectorId = collectorId,
            collectorName = collectorName,
            amount = amount,
            method = method,
            paymentDate = now,
            receiptNumber = receiptNo,
            notes = notes,
            isAdvance = redistributeAdvance,
            paymentType = paymentType,
            proofPhotoUri = proofPhotoUri,
            installmentsCoveredCount = newlyPaidCount
        )
        paymentDao.insertPayment(payment)

        installmentDao.insertInstallments(updatedInstallments)

        // Clear active late fees for cleared installments
        for (inst in updatedInstallments) {
            if (inst.remainingAmount <= 0.0) {
                lateFeeHistoryDao.markLateFeeLiquidated(inst.id)
            }
        }

        // Recalculate loan total remaining balance
        val newLoanPaid = loan.paidAmount + amount
        val newRemainingBalance = (loan.totalAmount - newLoanPaid).coerceAtLeast(0.0)
        val newStatus = if (newRemainingBalance <= 0.0) "LIQUIDADO" else "ACTIVO"

        loanDao.updateLoan(
            loan.copy(
                paidAmount = newLoanPaid,
                remainingBalance = newRemainingBalance,
                status = newStatus
            )
        )

        // Active cash register inflow
        val activeCash = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
        if (activeCash != null) {
            val isCash = method.uppercase() == "EFECTIVO"
            val isTransfer = method.uppercase() == "TRANSFERENCIA"
            val newCashInflows = if (isCash) activeCash.cashInflows + amount else activeCash.cashInflows
            val newTransferInflows = if (isTransfer) activeCash.transferInflows + amount else activeCash.transferInflows
            val newExpectedCash = activeCash.initialCash + newCashInflows - activeCash.outflows

            cashRegisterDao.updateCashRegister(
                activeCash.copy(
                    cashInflows = newCashInflows,
                    transferInflows = newTransferInflows,
                    expectedCash = newExpectedCash
                )
            )

            cashMovementDao.insertCashMovement(
                CashMovementEntity(
                    cashRegisterId = activeCash.id,
                    type = if (isCash) "INGRESO_COBRO_EFECTIVO" else if (isTransfer) "INGRESO_COBRO_TRANSFERENCIA" else "INGRESO_COBRO_OTROS",
                    amount = amount,
                    concept = "Cobro $paymentType ($method) Préstamo #$loanId - Recibo $receiptNo",
                    registeredBy = currentUser
                )
            )
        }

        // Recalculate and update Client Score and Status
        val client = clientDao.getClientById(loan.clientId)
        if (client != null) {
            val allClientLoans = loanDao.getLoansByClientSync(loan.clientId)
            var paidQuotas = 0
            var overdueQuotas = 0
            for (l in allClientLoans) {
                val insts = installmentDao.getInstallmentsByLoanSync(l.id)
                paidQuotas += insts.count { it.status == "PAGADO" }
                overdueQuotas += insts.count { it.status == "MORA" || it.status == "VENCIDO" }
            }
            val newScore = ScoreCalculator.calculateScore(paidQuotas, overdueQuotas)
            val newClientStatus = when {
                overdueQuotas > 3 -> "MOROSO"
                else -> "ACTIVO"
            }
            clientDao.updateClient(
                client.copy(
                    punctualityScore = newScore,
                    status = newClientStatus
                )
            )
        }

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = collectorId,
                username = currentUser,
                action = "REGISTRO_PAGO_INTELIGENTE",
                entityType = "PAGO",
                entityId = receiptNo,
                newValues = "Pago $paymentType de $amount MXN ($method) para Préstamo #$loanId. Redistribución: $redistributeAdvance. Recibo: $receiptNo. Comprobante: ${proofPhotoUri ?: "Ninguno"}"
            )
        )

        true
        }
    }
}

class CashRepository(
    private val cashRegisterDao: CashRegisterDao,
    private val cashMovementDao: CashMovementDao,
    private val auditLogDao: AuditLogDao
) : ICashRepository {
    override val activeCashRegister: Flow<CashRegisterEntity?> = cashRegisterDao.getActiveCashRegister()
    override val allCashRegisters: Flow<List<CashRegisterEntity>> = cashRegisterDao.getAllCashRegisters()

    override fun getActiveCashRegisterForCollectorFlow(collectorId: Long): kotlinx.coroutines.flow.Flow<CashRegisterEntity?> =
        cashRegisterDao.getActiveCashRegisterForCollectorFlow(collectorId)

    override fun getMovementsForRegister(registerId: Long): Flow<List<CashMovementEntity>> =
        cashMovementDao.getMovementsForRegister(registerId)

    override fun getAllMovements(): Flow<List<CashMovementEntity>> =
        cashMovementDao.getAllMovements()

    override suspend fun getActiveCashRegisterForCollector(collectorId: Long): CashRegisterEntity? =
        cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)

    override suspend fun openCashRegister(
        collectorId: Long,
        collectorName: String,
        initialCash: Double,
        notes: String,
        currentUser: String,
        branchName: String,
        fundName: String
    ): Long {
        // Enforce constraint: Only ONE active cash register allowed per user/collector!
        val existingActive = cashRegisterDao.getActiveCashRegisterForCollectorSync(collectorId)
        if (existingActive != null && existingActive.status == "ABIERTA") {
            throw IllegalStateException("Ya existe una caja ABIERTA activa (#${existingActive.id}) para este cobrador. Debe realizar el cierre antes de abrir una nueva.")
        }

        val register = CashRegisterEntity(
            collectorId = collectorId,
            collectorName = collectorName,
            openDate = System.currentTimeMillis(),
            initialCash = initialCash,
            cashInflows = 0.0,
            transferInflows = 0.0,
            outflows = 0.0,
            expectedCash = initialCash,
            status = "ABIERTA",
            notes = notes,
            branchName = branchName,
            fundName = fundName
        )
        val id = cashRegisterDao.insertCashRegister(register)

        // Log initial movement
        cashMovementDao.insertCashMovement(
            CashMovementEntity(
                cashRegisterId = id,
                type = "INGRESO_MANUAL",
                amount = initialCash,
                concept = "Apertura de Caja (Fondo Inicial: $$initialCash MXN - $branchName / $fundName)",
                registeredBy = currentUser,
                paymentMethod = "EFECTIVO",
                category = "APORTACION"
            )
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = collectorId,
                username = currentUser,
                action = "APERTURA_CAJA",
                entityType = "CAJA",
                entityId = id.toString(),
                newValues = "Apertura de caja con $$initialCash MXN. Cobrador: $collectorName | Sucursal: $branchName | Fondo: $fundName"
            )
        )
        return id
    }

    override suspend fun closeCashRegister(
        registerId: Long,
        actualCash: Double,
        notes: String,
        currentUser: String,
        denominationBreakdownJson: String?
    ): Boolean {
        val reg = cashRegisterDao.getCashRegisterById(registerId)
            ?: return false

        val expected = reg.initialCash + reg.cashInflows - reg.outflows
        val discrepancy = actualCash - expected

        val auditStatus = when {
            discrepancy < -0.01 -> "FALTANTE"
            discrepancy > 0.01 -> "SOBRANTE"
            else -> "CORRECTO"
        }

        val effectiveNotes = if (notes.isBlank()) {
            if (auditStatus != "CORRECTO") "Diferencia de arqueo detectada ($auditStatus: $$discrepancy MXN)" else "Cierre regular sin observaciones"
        } else {
            notes
        }

        val updated = reg.copy(
            closeDate = System.currentTimeMillis(),
            actualCash = actualCash,
            discrepancy = discrepancy,
            status = "CERRADA",
            auditStatus = auditStatus,
            denominationBreakdownJson = denominationBreakdownJson,
            notes = if (reg.notes.isBlank()) effectiveNotes else "${reg.notes} | Cierre: $effectiveNotes"
        )
        cashRegisterDao.updateCashRegister(updated)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = reg.collectorId,
                username = currentUser,
                action = "CIERRE_CAJA",
                entityType = "CAJA",
                entityId = registerId.toString(),
                newValues = "Cierre de caja #$registerId. Esperado: $$expected MXN, Contado: $$actualCash MXN, Diferencia: $$discrepancy MXN, Estado Auditoría: $auditStatus"
            )
        )
        return true
    }

    override suspend fun addCashMovement(
        registerId: Long,
        type: String,
        amount: Double,
        concept: String,
        paymentMethod: String,
        category: String,
        currentUser: String
    ): Boolean {
        val reg = cashRegisterDao.getCashRegisterById(registerId)
            ?: throw IllegalStateException("No hay una caja activa para registrar el movimiento.")

        if (amount <= 0.0) {
            throw IllegalArgumentException("El monto del movimiento debe ser estrictamente mayor a 0.")
        }

        val isCash = paymentMethod.uppercase() == "EFECTIVO"
        val isTransfer = paymentMethod.uppercase() == "TRANSFERENCIA"

        var newCashInflows = reg.cashInflows
        var newTransferInflows = reg.transferInflows
        var newOutflows = reg.outflows

        when (type) {
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
        }

        val newExpectedCash = reg.initialCash + newCashInflows - newOutflows

        cashRegisterDao.updateCashRegister(
            reg.copy(
                cashInflows = newCashInflows,
                transferInflows = newTransferInflows,
                outflows = newOutflows,
                expectedCash = newExpectedCash
            )
        )

        val movement = CashMovementEntity(
            cashRegisterId = reg.id,
            type = type,
            amount = amount,
            concept = concept,
            timestamp = System.currentTimeMillis(),
            registeredBy = currentUser,
            paymentMethod = paymentMethod,
            category = category
        )
        cashMovementDao.insertCashMovement(movement)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = reg.collectorId,
                username = currentUser,
                action = "MOVIMIENTO_CAJA",
                entityType = "CAJA_MOVIMIENTO",
                entityId = reg.id.toString(),
                newValues = "Movimiento $type de $$amount MXN ($paymentMethod). Concepto: $concept"
            )
        )
        return true
    }
}

class CollectionRepository(
    private val visitDao: CollectionVisitDao,
    private val gpsTrackLogDao: GpsTrackLogDao,
    private val auditLogDao: AuditLogDao
) : ICollectionRepository {
    override val allVisits: Flow<List<CollectionVisitEntity>> = visitDao.getAllVisits()

    override fun getVisitsByClient(clientId: Long): Flow<List<CollectionVisitEntity>> =
        visitDao.getVisitsByClient(clientId)

    override suspend fun recordVisit(visit: CollectionVisitEntity, currentUser: String): Long {
        val id = visitDao.insertVisit(visit)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = visit.collectorId,
                username = currentUser,
                action = "REGISTRO_VISITA_COBRANZA",
                entityType = "COBRANZA",
                entityId = id.toString(),
                newValues = "Visita a ${visit.clientName}: Resultado: ${visit.result}, Lat: ${visit.latitude}, Lng: ${visit.longitude}"
            )
        )
        return id
    }

    override suspend fun getPendingSyncVisits(): List<CollectionVisitEntity> =
        visitDao.getPendingSyncVisits()

    override suspend fun syncPendingVisits(): Int {
        val pending = visitDao.getPendingSyncVisits()
        for (v in pending) {
            visitDao.markVisitSynced(v.id)
        }
        return pending.size
    }

    override val allGpsLogs: Flow<List<GpsTrackLogEntity>> =
        gpsTrackLogDao.getAllGpsTrackLogs()

    override fun getGpsLogsForCollector(collectorId: Long): Flow<List<GpsTrackLogEntity>> =
        gpsTrackLogDao.getGpsTrackLogsForCollector(collectorId)

    override suspend fun recordGpsLog(log: GpsTrackLogEntity): Long =
        gpsTrackLogDao.insertGpsLog(log)

    override suspend fun syncPendingGpsLogs(): Int {
        val pending = gpsTrackLogDao.getPendingGpsLogs()
        for (g in pending) {
            gpsTrackLogDao.markGpsLogSynced(g.id)
        }
        return pending.size
    }
}

class RouteRepository(
    private val routeDao: CollectionRouteDao,
    private val assignmentDao: RouteClientAssignmentDao,
    private val auditLogDao: AuditLogDao
) : IRouteRepository {
    override val allRoutes: Flow<List<CollectionRouteEntity>> =
        routeDao.getAllRoutes()

    override fun getActiveRoutesForCollector(collectorId: Long): Flow<List<CollectionRouteEntity>> =
        routeDao.getActiveRoutesForCollector(collectorId)

    override fun getRoutesByZone(zone: String): Flow<List<CollectionRouteEntity>> =
        routeDao.getRoutesByZone(zone)

    override suspend fun getRouteById(id: Long): CollectionRouteEntity? =
        routeDao.getRouteById(id)

    override suspend fun saveRoute(route: CollectionRouteEntity, currentUser: String): Long {
        val id = routeDao.insertOrUpdateRoute(route)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 1L,
                username = currentUser,
                action = if (route.id == 0L) "CREAR_RUTA" else "ACTUALIZAR_RUTA",
                entityType = "RUTA_COBRANZA",
                entityId = id.toString(),
                newValues = "Ruta: ${route.name} (${route.code}), Cobrador: ${route.collectorName}, Zona: ${route.zone}"
            )
        )
        return id
    }

    override suspend fun deleteRoute(route: CollectionRouteEntity, currentUser: String) {
        routeDao.deleteRoute(route)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                userId = 1L,
                username = currentUser,
                action = "ELIMINAR_RUTA",
                entityType = "RUTA_COBRANZA",
                entityId = route.id.toString(),
                newValues = "Ruta eliminada: ${route.name}"
            )
        )
    }

    override fun getAssignmentsForRoute(routeId: Long): Flow<List<RouteClientAssignmentEntity>> =
        assignmentDao.getAssignmentsForRoute(routeId)

    override suspend fun getAssignmentsForRouteSync(routeId: Long): List<RouteClientAssignmentEntity> =
        assignmentDao.getAssignmentsForRouteSync(routeId)

    override suspend fun saveAssignmentsForRoute(
        routeId: Long,
        assignments: List<RouteClientAssignmentEntity>
    ): Boolean {
        assignmentDao.clearAssignmentsForRoute(routeId)
        assignmentDao.insertAssignments(assignments)
        // update route client count
        val route = routeDao.getRouteById(routeId)
        if (route != null) {
            routeDao.insertOrUpdateRoute(route.copy(totalClientsCount = assignments.size))
        }
        return true
    }

    override suspend fun assignClientToRoute(
        routeId: Long,
        clientId: Long,
        orderIndex: Int
    ): Long {
        val assignment = RouteClientAssignmentEntity(
            routeId = routeId,
            clientId = clientId,
            orderIndex = orderIndex
        )
        val id = assignmentDao.insertAssignment(assignment)
        val route = routeDao.getRouteById(routeId)
        if (route != null) {
            val assignments = assignmentDao.getAssignmentsForRouteSync(routeId)
            routeDao.insertOrUpdateRoute(route.copy(totalClientsCount = assignments.size))
        }
        return id
    }

    override suspend fun removeClientFromRoute(routeId: Long, clientId: Long) {
        assignmentDao.removeClientFromRoute(routeId, clientId)
        val route = routeDao.getRouteById(routeId)
        if (route != null) {
            val assignments = assignmentDao.getAssignmentsForRouteSync(routeId)
            routeDao.insertOrUpdateRoute(route.copy(totalClientsCount = assignments.size))
        }
    }
}

class AuditRepository(private val auditDao: AuditLogDao) : IAuditRepository {
    override val allAuditLogs: Flow<List<AuditLogEntity>> = auditDao.getAllAuditLogs()

    override suspend fun logAction(
        userId: Long,
        username: String,
        action: String,
        entityType: String,
        entityId: String,
        previousValues: String?,
        newValues: String?,
        traceId: String?,
        userRole: String?,
        reason: String?,
        notes: String?,
        result: String,
        latitude: Double?,
        longitude: Double?
    ): Long {
        val finalTraceId = traceId ?: ("TRX-2026-" + String.format(java.util.Locale.US, "%06d", (Math.abs(System.nanoTime()) % 1000000)))
        return auditDao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                username = username,
                action = action,
                entityType = entityType,
                entityId = entityId,
                previousValues = previousValues,
                newValues = newValues,
                traceId = finalTraceId,
                userRole = userRole,
                reason = reason,
                notes = notes,
                result = result,
                latitude = latitude,
                longitude = longitude
            )
        )
    }
}

class ConfigRepository(
    private val configDao: SystemConfigDao,
    private val auditLogDao: AuditLogDao
) : IConfigRepository {
    override val config: Flow<SystemConfigEntity?> = configDao.getConfig()

    override suspend fun getConfigSync(): SystemConfigEntity? {
        return configDao.getConfigSync()
    }

    override suspend fun updateConfig(config: SystemConfigEntity, modifiedBy: String, notes: String) {
        val oldConfig = configDao.getConfigSync()
        configDao.insertOrUpdateConfig(config)
        
        val oldString = oldConfig?.let {
            "P20 Interés: ${it.plan20InterestRate}, P30 Interés: ${it.plan30InterestRate}, Mora: ${it.lateFeePercentage}, Tipo Mora: ${it.lateFeeType}, Máx Supervisor: ${it.maxLoanAmountSupervisor}, Omitir Domingos: ${it.allowSkipSundays}"
        } ?: "Nueva Configuración"
        
        val newString = "P20 Interés: ${config.plan20InterestRate}, P30 Interés: ${config.plan30InterestRate}, Mora: ${config.lateFeePercentage}, Tipo Mora: ${config.lateFeeType}, Máx Supervisor: ${config.maxLoanAmountSupervisor}, Omitir Domingos: ${config.allowSkipSundays}"

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                action = "CONFIG_UPDATED",
                entityType = "SystemConfigEntity",
                entityId = config.id.toString(),
                userId = 1L, // System or current user ID
                username = modifiedBy,
                userRole = "ADMINISTRADOR",
                previousValues = oldString,
                newValues = newString,
                notes = notes,
                reason = "Cambio administrativo de configuración global",
                timestamp = System.currentTimeMillis()
            )
        )
    }
}

class CommunicationRepository(
    private val notificationDao: NotificationDao,
    private val messageTemplateDao: MessageTemplateDao,
    private val reminderConfigDao: ReminderConfigDao,
    private val communicationLogDao: CommunicationLogDao,
    private val notificationPreferenceDao: NotificationPreferenceDao,
    private val fcmDeviceRegistrationDao: FcmDeviceRegistrationDao
) : ICommunicationRepository {

    override fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsForUser(userId)

    override fun getAllNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getAllNotifications()

    override fun getUnreadCountForUser(userId: Long): Flow<Int> =
        notificationDao.getUnreadCountForUser(userId)

    override fun getTotalUnreadCount(): Flow<Int> =
        notificationDao.getTotalUnreadCount()

    override suspend fun createNotification(notification: NotificationEntity): Long =
        notificationDao.insertNotification(notification)

    override suspend fun markAsRead(id: Long) =
        notificationDao.markAsRead(id)

    override suspend fun markAllAsReadForUser(userId: Long) =
        notificationDao.markAllAsReadForUser(userId)

    override suspend fun deleteNotification(id: Long) =
        notificationDao.deleteNotification(id)

    override suspend fun clearAllNotifications() =
        notificationDao.clearAllNotifications()

    override val allTemplates: Flow<List<MessageTemplateEntity>> =
        messageTemplateDao.getAllTemplates()

    override suspend fun getTemplateByCode(code: String): MessageTemplateEntity? =
        messageTemplateDao.getTemplateByCode(code)

    override suspend fun saveTemplate(template: MessageTemplateEntity): Long =
        messageTemplateDao.insertOrUpdateTemplate(template)

    override val reminderConfig: Flow<ReminderConfigEntity?> =
        reminderConfigDao.getReminderConfig()

    override suspend fun getReminderConfigSync(): ReminderConfigEntity? =
        reminderConfigDao.getReminderConfigSync()

    override suspend fun saveReminderConfig(config: ReminderConfigEntity) =
        reminderConfigDao.insertOrUpdateConfig(config)

    override val allCommunicationLogs: Flow<List<CommunicationLogEntity>> =
        communicationLogDao.getAllLogs()

    override fun getCommunicationLogsByClient(clientId: Long): Flow<List<CommunicationLogEntity>> =
        communicationLogDao.getLogsByClient(clientId)

    override suspend fun logCommunicationAttempt(log: CommunicationLogEntity): Long =
        communicationLogDao.insertLog(log)

    override fun getNotificationPreferencesForUser(userId: Long): Flow<NotificationPreferenceEntity?> =
        notificationPreferenceDao.getPreferencesForUser(userId)

    override suspend fun saveNotificationPreferences(pref: NotificationPreferenceEntity) =
        notificationPreferenceDao.insertOrUpdatePreferences(pref)

    override suspend fun registerFcmDevice(
        userId: Long,
        fcmToken: String,
        deviceId: String,
        deviceModel: String
    ): Long {
        val reg = FcmDeviceRegistrationEntity(
            userId = userId,
            fcmToken = fcmToken,
            deviceId = deviceId,
            deviceModel = deviceModel,
            registeredAtMs = System.currentTimeMillis(),
            isActive = true
        )
        return fcmDeviceRegistrationDao.insertRegistration(reg)
    }
}


