package com.example.shared.constants

object AppConstants {

    // Roles
    const val ROLE_ADMINISTRADOR = "ADMINISTRADOR"
    const val ROLE_SUPERVISOR = "SUPERVISOR"
    const val ROLE_COBRADOR = "COBRADOR"
    const val ROLE_CONSULTA = "CONSULTA"

    // Loan Status
    const val STATUS_ACTIVO = "ACTIVO"
    const val STATUS_LIQUIDADO = "LIQUIDADO"
    const val STATUS_RENOVADO = "RENOVADO"
    const val STATUS_VENCIDO = "VENCIDO"
    const val STATUS_MOROSO = "MOROSO"

    // Installment Status
    const val INSTALLMENT_PENDIENTE = "PENDIENTE"
    const val INSTALLMENT_PAGADO = "PAGADO"
    const val INSTALLMENT_PARCIAL = "PARCIAL"
    const val INSTALLMENT_VENCIDO = "VENCIDO"

    // Cash Register Status
    const val CASH_STATUS_ABIERTA = "ABIERTA"
    const val CASH_STATUS_CERRADA = "CERRADA"

    // Payment Methods
    const val METHOD_EFECTIVO = "EFECTIVO"
    const val METHOD_TRANSFERENCIA = "TRANSFERENCIA"

    // Plan Codes
    const val PLAN_20_DIAS = "PLAN_20_DIAS"
    const val PLAN_30_DIAS = "PLAN_30_DIAS"

    // Default Zones
    val AVAILABLE_ZONES = listOf(
        "Zona Centro",
        "Zona Norte",
        "Zona Sur",
        "Zona Oriente",
        "Zona Poniente"
    )

    // Formats
    const val DATE_FORMAT_ISO = "yyyy-MM-dd"
    const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy"
    const val DATE_TIME_FORMAT_DISPLAY = "dd/MM/yyyy HH:mm"
}
