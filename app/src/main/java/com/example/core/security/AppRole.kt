package com.example.core.security

data class RoleDefinition(
    val name: String,
    val description: String,
    val defaultPermissions: Set<AppPermission>,
    val isSystemRole: Boolean = true
)

object RoleRegistry {
    val ADMINISTRADOR = RoleDefinition(
        name = "ADMINISTRADOR",
        description = "Acceso total y administración de la plataforma",
        defaultPermissions = AppPermission.values().toSet(),
        isSystemRole = true
    )

    val SUPERVISOR = RoleDefinition(
        name = "SUPERVISOR",
        description = "Supervisión de operaciones, créditos, cobranza y reportes",
        defaultPermissions = setOf(
            AppPermission.CLIENT_VIEW, AppPermission.CLIENT_CREATE, AppPermission.CLIENT_EDIT,
            AppPermission.LOAN_VIEW, AppPermission.LOAN_CREATE, AppPermission.LOAN_AUTHORIZE, AppPermission.LOAN_RENEW, AppPermission.LOAN_APPROVE,
            AppPermission.PAYMENT_REGISTER, AppPermission.CASH_MANAGE,
            AppPermission.REPORTS_VIEW, AppPermission.DASHBOARD_SUPERVISOR_VIEW, AppPermission.AUDIT_VIEW,
            AppPermission.AUDIT_VIEW_FINANCIAL, AppPermission.AUDIT_VIEW_USERS, AppPermission.AUDIT_EXPORT,
            AppPermission.AUTHORIZATION_VIEW, AppPermission.AUTHORIZATION_APPROVE,
            AppPermission.EVALUATE_CLIENT, AppPermission.SCORE_MANUAL_ADJUST,
            AppPermission.WHATSAPP_VIEW, AppPermission.WHATSAPP_SEND, AppPermission.WHATSAPP_REMINDER, AppPermission.WHATSAPP_PAYMENT, AppPermission.WHATSAPP_LATE_FEE, AppPermission.WHATSAPP_PROMISE, AppPermission.WHATSAPP_RENEWAL,
            AppPermission.DOCUMENTOS_VER, AppPermission.DOCUMENTOS_CARGAR, AppPermission.DOCUMENTOS_EDITAR, AppPermission.DOCUMENTOS_VALIDAR, AppPermission.DOCUMENTOS_RECHAZAR, AppPermission.DOCUMENTOS_ELIMINAR, AppPermission.CONFIG_DOCUMENTOS
        ),
        isSystemRole = true
    )

    val COBRADOR = RoleDefinition(
        name = "COBRADOR",
        description = "Cobranza en ruta, registro de pagos y simulaciones de préstamos",
        defaultPermissions = setOf(
            AppPermission.CLIENT_VIEW, AppPermission.CLIENT_CREATE,
            AppPermission.LOAN_VIEW, AppPermission.LOAN_CREATE,
            AppPermission.PAYMENT_REGISTER, AppPermission.CASH_MANAGE,
            AppPermission.DASHBOARD_VIEW,
            AppPermission.EVALUATE_CLIENT,
            AppPermission.WHATSAPP_VIEW, AppPermission.WHATSAPP_SEND, AppPermission.WHATSAPP_REMINDER, AppPermission.WHATSAPP_PAYMENT, AppPermission.WHATSAPP_LATE_FEE, AppPermission.WHATSAPP_PROMISE,
            AppPermission.DOCUMENTOS_VER, AppPermission.DOCUMENTOS_CARGAR
        ),
        isSystemRole = true
    )

    val CONSULTA = RoleDefinition(
        name = "CONSULTA",
        description = "Acceso de solo lectura a catálogos y reportes",
        defaultPermissions = setOf(
            AppPermission.CLIENT_VIEW,
            AppPermission.LOAN_VIEW,
            AppPermission.DASHBOARD_VIEW,
            AppPermission.REPORTS_VIEW,
            AppPermission.WHATSAPP_VIEW,
            AppPermission.DOCUMENTOS_VER
        ),
        isSystemRole = true
    )

    private val dynamicRoles = mutableMapOf<String, RoleDefinition>(
        ADMINISTRADOR.name to ADMINISTRADOR,
        SUPERVISOR.name to SUPERVISOR,
        COBRADOR.name to COBRADOR,
        CONSULTA.name to CONSULTA
    )

    fun registerRole(role: RoleDefinition) {
        dynamicRoles[role.name.uppercase()] = role
    }

    fun getRole(name: String): RoleDefinition {
        return dynamicRoles[name.uppercase()] ?: RoleDefinition(
            name = name.uppercase(),
            description = "Rol personalizado: $name",
            defaultPermissions = setOf(AppPermission.DASHBOARD_VIEW, AppPermission.CLIENT_VIEW),
            isSystemRole = false
        )
    }

    fun getAllRoles(): List<RoleDefinition> = dynamicRoles.values.toList()
}
