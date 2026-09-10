package com.example.domain

import com.example.data.local.*
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class EvaluationResult(
    val score: Int,
    val classification: String,
    val trend: String, // "ASCENDENTE", "DESCENDENTE", "ESTABLE"
    val recommendation: String, // "RECOMENDADO", "RECOMENDADO_CONDICIONADO", "REQUIERE_REVISION", "NO_RECOMENDADO"
    val minRecommendedAmount: Double,
    val maxRecommendedAmount: Double,
    val suggestedAmount: Double,
    val recommendedPlanCode: String,
    val recommendedPlanMessage: String,
    val positiveFactors: List<String>,
    val negativeFactors: List<String>,
    val explanationReasons: List<String>,
    val alerts: List<String>,
    val compliancePercentage: Double,
    val totalLoansCount: Int,
    val activeLoansCount: Int,
    val activeBalanceTotal: Double,
    val activeMoraTotal: Double,
    val renewalRecommendation: String, // "Renovación favorable", "Renovación condicionada", "Revisión requerida"
    val renewalReason: String
)

object CustomerEvaluationEngine {

    fun evaluateClient(
        client: ClientEntity,
        loans: List<LoanEntity>,
        installments: List<InstallmentEntity>,
        payments: List<PaymentEntity>,
        visits: List<CollectionVisitEntity>,
        previousEvaluation: ClientEvaluationEntity? = null,
        config: ClientEvaluationConfigEntity = ClientEvaluationConfigEntity(),
        systemConfig: SystemConfigEntity = SystemConfigEntity()
    ): EvaluationResult {

        if (!config.enableScore) {
            return EvaluationResult(
                score = 100,
                classification = "SISTEMA DESACTIVADO",
                trend = "ESTABLE",
                recommendation = "REQUIERE REVISIÓN",
                minRecommendedAmount = 1000.0,
                maxRecommendedAmount = systemConfig.maxLoanAmountStandard,
                suggestedAmount = 5000.0,
                recommendedPlanCode = "PLAN_20_DIAS",
                recommendedPlanMessage = "Módulo de score desactivado en configuración.",
                positiveFactors = listOf("Sistema de score desactivado por el administrador"),
                negativeFactors = emptyList(),
                explanationReasons = listOf("El motor de score se encuentra inactivo."),
                alerts = emptyList(),
                compliancePercentage = 100.0,
                totalLoansCount = loans.size,
                activeLoansCount = loans.count { it.status == "ACTIVO" || it.status == "VENCIDO" },
                activeBalanceTotal = loans.filter { it.status == "ACTIVO" || it.status == "VENCIDO" }.sumOf { it.remainingBalance },
                activeMoraTotal = installments.filter { it.status == "MORA" || it.status == "VENCIDO" }.sumOf { it.lateFee },
                renewalRecommendation = "Revisión requerida",
                renewalReason = "Score de cliente desactivado."
            )
        }

        val nowMs = System.currentTimeMillis()

        // Filter by historic period if configured (e.g. last 180 days)
        val cutoffMs = if (config.historicPeriodDays > 0) {
            nowMs - TimeUnit.DAYS.toMillis(config.historicPeriodDays.toLong())
        } else 0L

        val filteredInstallments = if (cutoffMs > 0) installments.filter { it.dueDate >= cutoffMs } else installments
        val filteredPayments = if (cutoffMs > 0) payments.filter { it.paymentDate >= cutoffMs } else payments
        val filteredVisits = if (cutoffMs > 0) visits.filter { it.visitDate >= cutoffMs } else visits

        // A. HISTORIAL DE PAGOS
        val totalDueInstallments = filteredInstallments.count { it.dueDate <= nowMs }
        val punctualPayments = filteredInstallments.count { it.status == "PAGADO" && it.daysOverdue <= 0 }
        val advancePayments = filteredPayments.count { it.isAdvance || it.paymentType == "ADELANTADO" }
        val latePayments = filteredInstallments.count { it.daysOverdue > 0 && it.paidAmount > 0 }
        val partialPayments = filteredInstallments.count { it.status == "PARCIAL" }
        val unpaidInstallments = filteredInstallments.count { (it.status == "VENCIDO" || it.status == "MORA") && it.paidAmount == 0.0 }

        // B. MORA
        val overdueInstallmentsCount = filteredInstallments.count { it.status == "MORA" || it.daysOverdue > 0 }
        val activeMoraAmount = installments.filter { it.status == "MORA" || it.status == "VENCIDO" }.sumOf { it.lateFee }
        val liquidatedMoraAmount = 0.0 // Could track liquidated late fee logs
        val brokenPromisesCount = filteredVisits.count { it.result == "PROMESA_INCUMPLIDA" || (it.result == "PROMESA_PAGO" && it.promisedPaymentDate != null && it.promisedPaymentDate < nowMs) }
        val fulfilledPromisesCount = filteredVisits.count { it.result == "PAGO_REALIZADO" || it.result == "PROMESA_CUMPLIDA" }

        // C. PRÉSTAMOS
        val totalLoansCount = loans.size
        val liquidatedLoansCount = loans.count { it.status == "LIQUIDADO" }
        val renewedLoansCount = loans.count { it.isRenewal || it.status == "RENOVADO" }
        val activeLoans = loans.filter { it.status == "ACTIVO" || it.status == "VENCIDO" }
        val activeLoansCount = activeLoans.size
        val activeBalanceTotal = activeLoans.sumOf { it.remainingBalance }

        // D. CUMPLIMIENTO
        val compliancePct = if (totalDueInstallments > 0) {
            ((punctualPayments.toDouble() + (advancePayments.toDouble() * 0.5)) / totalDueInstallments.toDouble() * 100.0).coerceIn(0.0, 100.0)
        } else {
            100.0 // Client without history starts at 100% compliance
        }

        // E. ANTIGÜEDAD
        val daysTenure = ((nowMs - client.createdAt) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)

        // F. COMPORTAMIENTO DE COBRANZA
        val successfulVisitsCount = filteredVisits.count { it.result == "PAGO_REALIZADO" }

        // SCORE CALCULATION WEIGHTED BY FACTORS
        var rawScore = 100.0

        if (totalLoansCount == 0 && totalDueInstallments == 0) {
            // New Client without loan history -> Initial score base 80 (MUY BUENO)
            rawScore = 80.0
        } else {
            // 1. Payment History Factor (Weight 30%)
            val paymentHistoryScore = if (totalDueInstallments > 0) {
                val ratioPunctual = punctualPayments.toDouble() / totalDueInstallments.toDouble()
                val ratioLate = latePayments.toDouble() / totalDueInstallments.toDouble()
                val ratioUnpaid = unpaidInstallments.toDouble() / totalDueInstallments.toDouble()
                (ratioPunctual * 100.0 + advancePayments * 5.0 - ratioLate * 30.0 - ratioUnpaid * 50.0).coerceIn(0.0, 100.0)
            } else 100.0

            // 2. Mora Factor (Weight 25%)
            val moraScore = run {
                var score = 100.0
                if (activeMoraAmount > 0) score -= 30.0
                if (overdueInstallmentsCount > 0) score -= (overdueInstallmentsCount * 15.0)
                if (brokenPromisesCount > 0) score -= (brokenPromisesCount * 20.0)
                score.coerceIn(0.0, 100.0)
            }

            // 3. Loans History Factor (Weight 15%)
            val loansHistoryScore = run {
                var score = 70.0
                score += (liquidatedLoansCount * 10.0)
                score += (renewedLoansCount * 5.0)
                if (activeLoansCount > config.maxActiveLoansPerClient) score -= 30.0
                score.coerceIn(0.0, 100.0)
            }

            // 4. Compliance Percentage Factor (Weight 15%)
            val complianceScore = compliancePct

            // 5. Seniority/Tenure Factor (Weight 5%)
            val tenureScore = when {
                daysTenure >= 365 -> 100.0
                daysTenure >= 180 -> 90.0
                daysTenure >= 90 -> 80.0
                daysTenure >= 30 -> 70.0
                else -> 60.0
            }

            // 6. Collection Behavior Factor (Weight 10%)
            val collectionScore = run {
                var score = 80.0
                score += (successfulVisitsCount * 5.0)
                score += (fulfilledPromisesCount * 10.0)
                score -= (brokenPromisesCount * 15.0)
                score.coerceIn(0.0, 100.0)
            }

            val totalWeight = config.weightPaymentHistory + config.weightMora +
                    config.weightLoansHistory + config.weightCompliancePct +
                    config.weightTenure + config.weightCollectionBehavior

            if (totalWeight > 0) {
                rawScore = (
                        (paymentHistoryScore * config.weightPaymentHistory) +
                                (moraScore * config.weightMora) +
                                (loansHistoryScore * config.weightLoansHistory) +
                                (complianceScore * config.weightCompliancePct) +
                                (tenureScore * config.weightTenure) +
                                (collectionScore * config.weightCollectionBehavior)
                        ) / totalWeight
            }
        }

        val finalScore = rawScore.toInt().coerceIn(0, 100)

        // CLASSIFICATION
        val classification = when {
            finalScore >= config.rangeExcelenteMin -> "EXCELENTE"
            finalScore >= config.rangeMuyBuenoMin -> "MUY BUENO"
            finalScore >= config.rangeBuenoMin -> "BUENO"
            finalScore >= config.rangeRegularMin -> "REGULAR"
            finalScore >= config.rangeRiesgoAltoMin -> "RIESGO ALTO"
            else -> "RIESGO CRÍTICO"
        }

        // TREND
        val trend = if (previousEvaluation != null) {
            val diff = finalScore - previousEvaluation.score
            when {
                diff >= 3 -> "ASCENDENTE"
                diff <= -3 -> "DESCENDENTE"
                else -> "ESTABLE"
            }
        } else "ESTABLE"

        // POSITIVE AND NEGATIVE FACTORS
        val positiveFactors = mutableListOf<String>()
        val negativeFactors = mutableListOf<String>()

        if (punctualPayments > 0) positiveFactors.add("+ $punctualPayments pagos puntuales")
        if (advancePayments > 0) positiveFactors.add("+ $advancePayments pagos adelantados")
        if (liquidatedLoansCount > 0) positiveFactors.add("+ $liquidatedLoansCount préstamos liquidados")
        if (compliancePct >= 90.0) positiveFactors.add("+ ${compliancePct.toInt()}% de cumplimiento histórico")
        if (daysTenure >= 180) positiveFactors.add("+ Cliente con más de ${daysTenure / 30} meses de antigüedad")
        if (fulfilledPromisesCount > 0) positiveFactors.add("+ $fulfilledPromisesCount promesas de pago cumplidas")

        if (unpaidInstallments > 0) negativeFactors.add("- $unpaidInstallments cuota(s) incumplidas")
        if (latePayments > 0) negativeFactors.add("- $latePayments abono(s) con atraso")
        if (activeMoraAmount > 0) negativeFactors.add("- Mora activa acumulada de $${String.format("%.2f", activeMoraAmount)}")
        if (brokenPromisesCount > 0) negativeFactors.add("- $brokenPromisesCount promesa(s) de pago incumplida(s)")
        if (activeLoansCount >= config.maxActiveLoansPerClient) negativeFactors.add("- $activeLoansCount préstamo(s) activo(s) simultáneo(s)")

        if (positiveFactors.isEmpty()) {
            positiveFactors.add("+ Registro de cliente activo")
        }

        // ALERTS GENERATION
        val alerts = mutableListOf<String>()
        if (trend == "DESCENDENTE") alerts.add("Score disminuyendo en evaluaciones recientes")
        if (activeMoraAmount > 0) alerts.add("Aumento o existencia de mora activa")
        if (brokenPromisesCount > 0) alerts.add("Se registraron promesas de pago incumplidas")
        if (latePayments >= 3) alerts.add("Múltiples atrasos en pagos recientes")
        if (activeLoansCount > config.maxActiveLoansPerClient) alerts.add("Exceso de préstamos activos simultáneos")
        if (finalScore >= 90) alerts.add("Cliente con comportamiento excepcionalmente bueno")
        if (trend == "ASCENDENTE") alerts.add("Cliente que mejora su score progresivamente")

        // RECOMMENDATION CATEGORY
        val recommendation = when {
            finalScore < 40 || activeMoraAmount > 0.0 || brokenPromisesCount >= 2 -> "NO RECOMENDADO"
            finalScore < 60 || activeLoansCount >= config.maxActiveLoansPerClient -> "REQUIERE REVISIÓN"
            finalScore < 80 || latePayments > 0 -> "RECOMENDADO CONDICIONADO"
            else -> "RECOMENDADO"
        }

        // RECOMMENDED AMOUNTS
        // Max capacity based on monthly income (30% capacity) or default standard limit
        val capacityLimit = if (client.monthlyIncome > 0) client.monthlyIncome * 0.35 * 2.0 else systemConfig.maxLoanAmountStandard

        var suggestedBase = when (classification) {
            "EXCELENTE" -> 10000.0
            "MUY BUENO" -> 8000.0
            "BUENO" -> 6000.0
            "REGULAR" -> 4000.0
            "RIESGO ALTO" -> 2000.0
            else -> 1000.0
        }

        if (liquidatedLoansCount > 0) {
            val maxPreviousCapital = loans.filter { it.status == "LIQUIDADO" }.maxOfOrNull { it.capital } ?: 5000.0
            suggestedBase = (maxPreviousCapital * 1.2).coerceAtLeast(suggestedBase)
        }

        // STRICT SAFETY RULES: NEVER exceed system limits (Product, Collector, Supervisor, Admin, Configured Capacity)
        val adminMaxLimit = systemConfig.maxLoanAmountSupervisor.coerceAtLeast(systemConfig.maxLoanAmountStandard)
        val strictMax = minOf(capacityLimit, adminMaxLimit)

        val suggestedAmount = minOf(suggestedBase, strictMax)
        val minRecommendedAmount = Math.max(1000.0, Math.floor(suggestedAmount * 0.5 / 500.0) * 500.0)
        val maxRecommendedAmount = minOf(Math.ceil(suggestedAmount * 1.3 / 500.0) * 500.0, strictMax)

        // RECOMMENDED PLAN & TERM
        val (recommendedPlanCode, recommendedPlanMsg) = when {
            finalScore >= 80 -> Pair("PLAN_30_DIAS", "Cliente con comportamiento favorable. Puede evaluarse P30 (30 días).")
            finalScore >= 60 -> Pair("PLAN_20_DIAS", "Cliente con buen comportamiento. Se recomienda mantener P20 (20 días).")
            else -> Pair("PLAN_20_DIAS", "Riesgo moderado. Se recomienda mantener plazo corto P20 y monto limitado.")
        }

        // EXPLANATION REASONS (Explicabilidad)
        val reasons = mutableListOf<String>()
        reasons.add("Score obtenido: $finalScore / 100 ($classification)")
        reasons.add("${compliancePct.toInt()}% de cumplimiento en pagos históricos")
        if (activeLoansCount > 0) reasons.add("$activeLoansCount préstamo(s) activo(s) con saldo total de $${String.format("%.2f", activeBalanceTotal)}")
        if (activeMoraAmount > 0) reasons.add("Mora activa de $${String.format("%.2f", activeMoraAmount)}") else reasons.add("Sin mora activa actualmente")
        if (brokenPromisesCount > 0) reasons.add("$brokenPromisesCount promesa(s) de pago incumplida(s)")
        if (liquidatedLoansCount > 0) reasons.add("$liquidatedLoansCount préstamo(s) previo(s) liquidados con éxito")

        // RENEWAL RECOMMENDATION
        val (renewalRec, renewalMsg) = when {
            activeMoraAmount > 0 || brokenPromisesCount >= 2 -> Pair("Revisión requerida", "Presenta mora activa o promesas incumplidas. No se sugiere renovación automática.")
            activeBalanceTotal > maxRecommendedAmount * 0.8 -> Pair("Renovación condicionada", "Saldo pendiente elevado ($${String.format("%.2f", activeBalanceTotal)}). Se sugiere liquidación previa o reestructura.")
            finalScore >= 70 -> Pair("Renovación favorable", "Historial de pagos positivo. Candidato idóneo para renovación de crédito.")
            else -> Pair("Renovación condicionada", "Score regular ($finalScore). Requiere autorización de supervisor.")
        }

        return EvaluationResult(
            score = finalScore,
            classification = classification,
            trend = trend,
            recommendation = recommendation,
            minRecommendedAmount = minRecommendedAmount,
            maxRecommendedAmount = maxRecommendedAmount,
            suggestedAmount = suggestedAmount,
            recommendedPlanCode = recommendedPlanCode,
            recommendedPlanMessage = recommendedPlanMsg,
            positiveFactors = positiveFactors,
            negativeFactors = negativeFactors,
            explanationReasons = reasons,
            alerts = alerts,
            compliancePercentage = compliancePct,
            totalLoansCount = totalLoansCount,
            activeLoansCount = activeLoansCount,
            activeBalanceTotal = activeBalanceTotal,
            activeMoraTotal = activeMoraAmount,
            renewalRecommendation = renewalRec,
            renewalReason = renewalMsg
        )
    }
}
