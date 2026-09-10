package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import com.example.data.local.ClientEntity
import com.example.data.local.CollectionVisitEntity
import com.example.data.local.LoanEntity
import com.example.data.local.PaymentEntity

enum class ClientRiskLevel(val label: String, val badgeColor: Color, val textColor: Color) {
    BAJO("Riesgo Bajo", Color(0xFFD1FAE5), Color(0xFF065F46)),
    MEDIO("Riesgo Medio", Color(0xFFFEF3C7), Color(0xFF92400E)),
    ALTO("Riesgo Alto", Color(0xFFFFEDD5), Color(0xFFC2410C)),
    CRITICO("Riesgo Crítico", Color(0xFFFEE2E2), Color(0xFF991B1B))
}

data class CreditRecommendation(
    val riskLevel: ClientRiskLevel,
    val paymentProbabilityLabel: String,
    val paymentProbabilityValue: Double, // e.g. 0.95
    val recommendedAmount: Double,
    val recommendedTerm: String,
    val decisionTitle: String,
    val decisionRationale: String
)

data class ClientDigitalExpediente(
    val client: ClientEntity,
    val loans: List<LoanEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val visits: List<CollectionVisitEntity> = emptyList(),
    val activeLoansCount: Int = 0,
    val totalLoansCount: Int = 0,
    val liquidatedLoansCount: Int = 0,
    val totalPaidAmount: Double = 0.0,
    val activeRemainingBalance: Double = 0.0,
    val overdueInstallmentsCount: Int = 0,
    val totalLateFees: Double = 0.0,
    val renewalsCount: Int = 0,
    val recommendation: CreditRecommendation
)

fun calculateClientRecommendation(
    client: ClientEntity,
    loans: List<LoanEntity>,
    payments: List<PaymentEntity>,
    visits: List<CollectionVisitEntity>
): CreditRecommendation {
    val score = client.punctualityScore
    val isBlacklisted = client.status == "LISTA_NEGRA"
    val isMoroso = client.status == "MOROSO"
    val monthlyIncome = if (client.monthlyIncome > 0) client.monthlyIncome else 10000.0

    val liquidatedLoans = loans.count { it.status == "LIQUIDADO" }
    val activeLoans = loans.count { it.status == "ACTIVO" }
    val maxPreviousLoanCapital = loans.maxOfOrNull { it.capital } ?: 3000.0

    // Income-based max capacity rule: daily payment shouldn't exceed ~20% of daily income
    val dailyIncome = monthlyIncome / 30.0
    val maxAffordableDaily = dailyIncome * 0.25
    val maxAffordableCapital20Days = maxAffordableDaily * 16.0 // capital approx for 20 days plan

    return when {
        isBlacklisted -> {
            CreditRecommendation(
                riskLevel = ClientRiskLevel.CRITICO,
                paymentProbabilityLabel = "Muy Baja (5%)",
                paymentProbabilityValue = 0.05,
                recommendedAmount = 0.0,
                recommendedTerm = "No Aplica",
                decisionTitle = "RECHAZO DE CRÉDITO AUTOMÁTICO",
                decisionRationale = "El cliente se encuentra registrado en Lista Negra por antecedentes graves de incumplimiento o fraude. No otorgar nuevos préstamos."
            )
        }
        isMoroso -> {
            CreditRecommendation(
                riskLevel = ClientRiskLevel.ALTO,
                paymentProbabilityLabel = "Baja (35%)",
                paymentProbabilityValue = 0.35,
                recommendedAmount = 1500.0,
                recommendedTerm = "20 Días Diarios",
                decisionTitle = "REVISIÓN MANUAL CON AVAL / RECHAZO",
                decisionRationale = "El cliente cuenta con estatus de Moroso activo o cuotas vencidas. Se requiere liquidación previa de saldo pendiente antes de evaluar renovación."
            )
        }
        score >= 90 && liquidatedLoans >= 1 -> {
            val nextCapital = (maxPreviousLoanCapital * 1.3).coerceAtMost(maxAffordableCapital20Days).coerceAtMost(15000.0)
            CreditRecommendation(
                riskLevel = ClientRiskLevel.BAJO,
                paymentProbabilityLabel = "Excelente (96%)",
                paymentProbabilityValue = 0.96,
                recommendedAmount = (Math.round(nextCapital / 500.0) * 500).toDouble().coerceAtLeast(3000.0),
                recommendedTerm = if (nextCapital >= 6000) "30 Días Diarios" else "20 Días Diarios",
                decisionTitle = "APROBACIÓN AUTOMÁTICA RECOMENDADA",
                decisionRationale = "Cliente con excelente historial ($score/100 pts) y $liquidatedLoans préstamo(s) liquidado(s) puntualmente. Se autoriza incremento de línea de crédito."
            )
        }
        score >= 75 -> {
            val nextCapital = (maxPreviousLoanCapital * 1.1).coerceAtMost(maxAffordableCapital20Days)
            CreditRecommendation(
                riskLevel = ClientRiskLevel.MEDIO,
                paymentProbabilityLabel = "Alta (82%)",
                paymentProbabilityValue = 0.82,
                recommendedAmount = (Math.round(nextCapital / 500.0) * 500).toDouble().coerceAtLeast(2000.0),
                recommendedTerm = "20 Días Diarios",
                decisionTitle = "APROBACIÓN CON CONDICIONES ESTÁNDAR",
                decisionRationale = "Cliente con buen comportamiento ($score/100 pts). Se recomienda mantener monto controlado hasta completar ciclo actual."
            )
        }
        else -> {
            CreditRecommendation(
                riskLevel = ClientRiskLevel.ALTO,
                paymentProbabilityLabel = "Media-Baja (55%)",
                paymentProbabilityValue = 0.55,
                recommendedAmount = 2000.0,
                recommendedTerm = "20 Días Diarios",
                decisionTitle = "REVISIÓN POR SUPERVISOR REQUERIDA",
                decisionRationale = "Score de puntualidad regular ($score/100 pts). Se aconseja solicitar verificación domiciliaria actualizada antes de autorizar."
            )
        }
    }
}
