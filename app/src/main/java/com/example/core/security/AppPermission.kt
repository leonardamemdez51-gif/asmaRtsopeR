package com.example.core.security

enum class AppPermission(
    val code: String,
    val title: String,
    val description: String,
    val category: String
) {
    // Users
    USER_VIEW("USER_VIEW", "Ver usuarios", "Permite ver la lista y detalles de usuarios", "Usuarios"),
    USER_CREATE("USER_CREATE", "Crear usuarios", "Permite registrar nuevos usuarios", "Usuarios"),
    USER_EDIT("USER_EDIT", "Editar usuarios", "Permite modificar datos de usuarios", "Usuarios"),
    USER_DISABLE("USER_DISABLE", "Desactivar usuarios", "Permite cambiar el estado de usuarios", "Usuarios"),
    USER_RESET_PASSWORD("USER_RESET_PASSWORD", "Restablecer contraseña", "Permite forzar cambio de contraseña a otros usuarios", "Usuarios"),
    USER_ASSIGN_ROLE("USER_ASSIGN_ROLE", "Asignar rol", "Permite cambiar el rol de un usuario", "Usuarios"),
    USER_ASSIGN_ROUTE("USER_ASSIGN_ROUTE", "Asignar ruta", "Permite asignar rutas a usuarios", "Usuarios"),
    USER_ASSIGN_FUND("USER_ASSIGN_FUND", "Asignar fondo", "Permite asignar fondos a usuarios", "Usuarios"),
    USER_MANAGE_PERMISSIONS("USER_MANAGE_PERMISSIONS", "Administrar permisos", "Permite otorgar o denegar permisos individuales", "Usuarios"),

    // Clients
    CLIENT_VIEW("CLIENT_VIEW", "Ver clientes", "Permite consultar el catálogo y detalle de clientes", "Clientes"),
    CLIENT_CREATE("CLIENT_CREATE", "Crear clientes", "Permite registrar nuevos clientes en la plataforma", "Clientes"),
    CLIENT_EDIT("CLIENT_EDIT", "Editar clientes", "Permite modificar datos personales y referencias de clientes", "Clientes"),
    CLIENT_DELETE("CLIENT_DELETE", "Eliminar clientes", "Permite dar de baja o eliminar expedientes de clientes", "Clientes"),

    // Loans
    LOAN_VIEW("LOAN_VIEW", "Ver préstamos", "Permite ver la lista y amortización de préstamos", "Préstamos"),
    LOAN_CREATE("LOAN_CREATE", "Crear préstamos", "Permite simular y desembolsar préstamos", "Préstamos"),
    LOAN_EDIT("LOAN_EDIT", "Editar préstamos", "Permite modificar datos de préstamos", "Préstamos"),
    LOAN_APPROVE("LOAN_APPROVE", "Autorizar préstamos", "Permite aprobar créditos de alto monto o especiales", "Préstamos"),
    LOAN_AUTHORIZE("LOAN_AUTHORIZE", "Autorizar préstamos (Legacy)", "Alias de LOAN_APPROVE", "Préstamos"),
    LOAN_RENEW("LOAN_RENEW", "Renovar préstamos", "Permite procesar reestructuras y renovaciones de crédito", "Préstamos"),

    // Payments & Cash
    PAYMENT_VIEW("PAYMENT_VIEW", "Ver pagos", "Permite consultar los pagos registrados", "Caja y Pagos"),
    PAYMENT_CREATE("PAYMENT_CREATE", "Crear pagos", "Permite registrar nuevos pagos", "Caja y Pagos"),
    PAYMENT_EDIT("PAYMENT_EDIT", "Editar pagos", "Permite modificar datos de pagos registrados", "Caja y Pagos"),
    PAYMENT_REGISTER("PAYMENT_REGISTER", "Registrar pagos (Legacy)", "Alias de PAYMENT_CREATE", "Caja y Pagos"),
    PAYMENT_CANCEL("PAYMENT_CANCEL", "Cancelar pagos", "Permite anular o revertir transacciones de pago", "Caja y Pagos"),
    
    CASH_VIEW("CASH_VIEW", "Ver caja", "Permite consultar el estado de la caja", "Caja y Pagos"),
    CASH_OPEN("CASH_OPEN", "Abrir caja", "Permite iniciar operaciones de caja", "Caja y Pagos"),
    CASH_CLOSE("CASH_CLOSE", "Cerrar caja", "Permite realizar cortes y cierres de caja", "Caja y Pagos"),
    CASH_MANAGE("CASH_MANAGE", "Administración de caja (Legacy)", "Permite aperturar, arquear y cerrar la caja diaria", "Caja y Pagos"),

    // Administration & Systems
    USER_MANAGE("USER_MANAGE", "Administrar usuarios (Legacy)", "Alias general", "Administración"),
    CONFIG_MANAGE("CONFIG_MANAGE", "Configuración del sistema", "Permite modificar tasas, moras y parámetros del ERP", "Administración"),
    SETTINGS_VIEW("SETTINGS_VIEW", "Ver configuración", "Permite ver la configuración global", "Administración"),
    SETTINGS_EDIT("SETTINGS_EDIT", "Editar configuración", "Permite modificar la configuración global", "Administración"),
    
    // Reports
    REPORTS_VIEW("REPORTS_VIEW", "Generación de reportes", "Permite consultar reportes gerenciales", "Reportes"),
    REPORT_VIEW("REPORT_VIEW", "Ver reportes", "Alias de REPORTS_VIEW", "Reportes"),
    REPORT_EXPORT("REPORT_EXPORT", "Exportar reportes", "Permite descargar reportes", "Reportes"),

    // Dashboard
    DASHBOARD_VIEW("DASHBOARD_VIEW", "Ver dashboard", "Permite consultar el resumen financiero y KPIs", "Dashboard"),
    DASHBOARD_SUPERVISOR_VIEW("DASHBOARD_SUPERVISOR_VIEW", "Ver dashboard de supervisor", "Permite al supervisor consultar los KPIs de su equipo", "Dashboard"),
    
    // Audit
    AUDIT_VIEW("AUDIT_VIEW", "Ver auditoría", "Permite consultar la bitácora de eventos y auditoría de seguridad", "Auditoría"),
    AUDIT_VIEW_FINANCIAL("AUDIT_VIEW_FINANCIAL", "Ver auditoría financiera", "Permite consultar auditorías de pagos, préstamos y cajas", "Auditoría"),
    AUDIT_VIEW_USERS("AUDIT_VIEW_USERS", "Ver auditoría de usuarios", "Permite consultar auditorías de creación, modificación y estado de usuarios", "Auditoría"),
    AUDIT_VIEW_SECURITY("AUDIT_VIEW_SECURITY", "Ver auditoría de seguridad", "Permite consultar auditorías de bloqueos, contraseñas y accesos", "Auditoría"),
    AUDIT_EXPORT("AUDIT_EXPORT", "Exportar auditoría", "Permite exportar registros de auditoría a PDF o Excel/CSV", "Auditoría"),
    AUDIT_CONFIG("AUDIT_CONFIG", "Ver auditoría de configuración", "Permite consultar auditorías de cambios en parámetros globales del ERP", "Auditoría"),

    // Authorizations
    AUTHORIZATION_VIEW("AUTHORIZATION_VIEW", "Ver autorizaciones", "Permite ver solicitudes de autorización", "Autorizaciones"),
    AUTHORIZATION_APPROVE("AUTHORIZATION_APPROVE", "Aprobar autorizaciones", "Permite autorizar o rechazar solicitudes", "Autorizaciones"),

    // Customer Evaluation & Scoring
    EVALUATE_CLIENT("EVALUATE_CLIENT", "Evaluar cliente", "Permite consultar scoring, comportamiento y recomendación de crédito", "Evaluación"),
    SCORE_MANUAL_ADJUST("SCORE_MANUAL_ADJUST", "Ajuste manual de score", "Permite ingresar observaciones y ajustes manuales al score del cliente", "Evaluación"),

    // WhatsApp
    WHATSAPP_VIEW("WHATSAPP_VIEW", "Ver WhatsApp", "Permite consultar el historial de comunicaciones por WhatsApp", "WhatsApp"),
    WHATSAPP_SEND("WHATSAPP_SEND", "Enviar WhatsApp", "Permite preparar y abrir mensajes de WhatsApp", "WhatsApp"),
    WHATSAPP_REMINDER("WHATSAPP_REMINDER", "Enviar recordatorio", "Permite enviar recordatorios de pago", "WhatsApp"),
    WHATSAPP_PAYMENT("WHATSAPP_PAYMENT", "Enviar confirmación de pago", "Permite enviar recibos y confirmaciones de cobro", "WhatsApp"),
    WHATSAPP_LATE_FEE("WHATSAPP_LATE_FEE", "Enviar alerta de mora", "Permite enviar notificaciones de cuota vencida y mora", "WhatsApp"),
    WHATSAPP_PROMISE("WHATSAPP_PROMISE", "Enviar promesa de pago", "Permite enviar confirmaciones de promesas de pago agendadas", "WhatsApp"),
    WHATSAPP_RENEWAL("WHATSAPP_RENEWAL", "Enviar oferta de renovación", "Permite enviar invitaciones de renovación de crédito", "WhatsApp"),

    // Maps & Location
    MAPA_VER("MAPA_VER", "Ver mapas", "Permite visualizar el mapa interactivo", "Mapas"),
    UBICACION_CLIENTE_VER("UBICACION_CLIENTE_VER", "Ver ubicación de cliente", "Permite ver coordenadas del cliente", "Mapas"),
    UBICACION_CLIENTE_EDITAR("UBICACION_CLIENTE_EDITAR", "Editar ubicación de cliente", "Permite registrar o modificar coordenadas de clientes", "Mapas"),
    UBICACION_VISITA_REGISTRAR("UBICACION_VISITA_REGISTRAR", "Registrar ubicación de visita", "Permite capturar coordenadas GPS al registrar visitas", "Mapas"),
    RUTA_VER("RUTA_VER", "Ver rutas", "Permite ver la ruta del cobrador", "Mapas"),
    NAVEGACION_USAR("NAVEGACION_USAR", "Usar navegación", "Permite disparar navegación externa de Google Maps", "Mapas"),
    UBICACION_HISTORIAL_VER("UBICACION_HISTORIAL_VER", "Ver historial de ubicaciones", "Permite ver el historial o logs GPS", "Mapas"),

    // Documentos & Expediente Digital
    DOCUMENTOS_VER("DOCUMENTOS_VER", "Ver documentos", "Permite visualizar los documentos del cliente", "Documentos"),
    DOCUMENTOS_CARGAR("DOCUMENTOS_CARGAR", "Cargar documentos", "Permite subir o tomar fotografías de documentos", "Documentos"),
    DOCUMENTOS_EDITAR("DOCUMENTOS_EDITAR", "Editar documentos", "Permite editar metadatos o reemplazar documentos", "Documentos"),
    DOCUMENTOS_VALIDAR("DOCUMENTOS_VALIDAR", "Validar documentos", "Permite aprobar o verificar documentos", "Documentos"),
    DOCUMENTOS_RECHAZAR("DOCUMENTOS_RECHAZAR", "Rechazar documentos", "Permite rechazar documentos no válidos con motivo", "Documentos"),
    DOCUMENTOS_ELIMINAR("DOCUMENTOS_ELIMINAR", "Eliminar documentos", "Permite la eliminación lógica de documentos", "Documentos"),
    CONFIG_DOCUMENTOS("CONFIG_DOCUMENTOS", "Configurar tipos documentales", "Permite configurar obligatoriedad, validaciones y bloqueos de documentos", "Documentos");

    companion object {
        fun fromCode(code: String): AppPermission? = values().find { it.code.equals(code, ignoreCase = true) }
    }
}
