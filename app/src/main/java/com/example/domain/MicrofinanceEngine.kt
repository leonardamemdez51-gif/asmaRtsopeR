package com.example.domain

import com.example.data.local.InstallmentEntity
import com.example.data.local.LoanEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

enum class PlanType(val code: String, val title: String, val interestRate: Double, val totalDays: Int) {
    PLAN_20_DIAS("PLAN_20_DIAS", "20% en 20 días", 0.20, 20),
    PLAN_30_DIAS("PLAN_30_DIAS", "30% en 30 días", 0.30, 30);

    companion object {
        fun fromCode(code: String): PlanType {
            return values().find { it.code == code } ?: PLAN_20_DIAS
        }
    }
}

data class DynamicLoanPlan(
    val code: String,
    val name: String,
    val days: Int,
    val rate: Double
) {
    val interestRate: Double get() = rate
    val durationDays: Int get() = days
    val interestPercentageText: String get() = "${(rate * 100).toInt()}% en $days días"
}

data class LoanCalculationResult(
    val capital: Double,
    val interestRate: Double,
    val interestAmount: Double,
    val totalAmount: Double,
    val dailyPayment: Double,
    val planCode: String,
    val planTitle: String,
    val totalDays: Int
)

data class LoanRecoveredMetrics(
    val recoveredCapital: Double,
    val recoveredInterest: Double,
    val remainingCapital: Double,
    val remainingInterest: Double
)

object LoanCalculator {

    fun parseCustomPlansJson(jsonString: String?): List<DynamicLoanPlan> {
        val defaultPlans = listOf(
            DynamicLoanPlan("PLAN_20_DIAS", "Plan P20 (20% en 20 días)", 20, 0.20),
            DynamicLoanPlan("PLAN_30_DIAS", "Plan P30 (30% en 30 días)", 30, 0.30)
        )
        if (jsonString.isNullOrBlank()) return defaultPlans

        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<DynamicLoanPlan>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    DynamicLoanPlan(
                        code = obj.optString("code", "PLAN_CUSTOM_$i"),
                        name = obj.optString("name", "Plan Personalizado"),
                        days = obj.optInt("days", 20),
                        rate = obj.optDouble("rate", 0.20)
                    )
                )
            }
            if (list.isEmpty()) defaultPlans else list
        } catch (_: Exception) {
            defaultPlans
        }
    }

    fun calculateLoan(
        capital: Double,
        planCode: String,
        customPlansJson: String? = null
    ): LoanCalculationResult {
        val availablePlans = parseCustomPlansJson(customPlansJson)
        val selectedPlan = availablePlans.find { it.code == planCode }
            ?: availablePlans.firstOrNull()
            ?: DynamicLoanPlan("PLAN_20_DIAS", "Plan P20", 20, 0.20)

        val interestRate = selectedPlan.rate
        val interestAmount = capital * interestRate
        val totalAmount = capital + interestAmount
        val rawDaily = if (selectedPlan.days > 0) totalAmount / selectedPlan.days else totalAmount
        val dailyPayment = Math.ceil(rawDaily * 100.0) / 100.0

        return LoanCalculationResult(
            capital = capital,
            interestRate = interestRate,
            interestAmount = interestAmount,
            totalAmount = totalAmount,
            dailyPayment = dailyPayment,
            planCode = selectedPlan.code,
            planTitle = selectedPlan.name,
            totalDays = selectedPlan.days
        )
    }

    fun calculateLoan(
        capital: Double,
        rate: Double,
        days: Int
    ): LoanCalculationResult {
        val interestAmount = capital * rate
        val totalAmount = capital + interestAmount
        val rawDaily = if (days > 0) totalAmount / days else totalAmount
        val dailyPayment = Math.ceil(rawDaily * 100.0) / 100.0

        return LoanCalculationResult(
            capital = capital,
            interestRate = rate,
            interestAmount = interestAmount,
            totalAmount = totalAmount,
            dailyPayment = dailyPayment,
            planCode = "PLAN_CUSTOM",
            planTitle = "Plan $days días (${(rate * 100).toInt()}%)",
            totalDays = days
        )
    }

    fun calculateLoan(capital: Double, planType: PlanType): LoanCalculationResult {
        return calculateLoan(capital, planType.code, null)
    }

    fun calculateFirstPaymentDate(disbursementDateMs: Long, skipSundays: Boolean): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = disbursementDateMs }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        if (skipSundays && cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun calculateExpectedMaturityDate(disbursementDateMs: Long, totalDays: Int, skipSundays: Boolean): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = disbursementDateMs }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        var added = 0
        while (added < totalDays) {
            if (skipSundays && cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            added++
            if (added < totalDays) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    fun generateInstallments(
        loanId: Long,
        clientId: Long,
        capital: Double,
        interestAmount: Double,
        totalAmount: Double,
        dailyPayment: Double,
        totalDays: Int,
        startDateMs: Long,
        skipSundays: Boolean
    ): List<InstallmentEntity> {
        val installments = mutableListOf<InstallmentEntity>()
        val capitalPerQuota = capital / totalDays
        val interestPerQuota = interestAmount / totalDays

        val calendar = Calendar.getInstance().apply {
            timeInMillis = startDateMs
        }

        // First payment is on the day AFTER disbursement
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var count = 0
        while (count < totalDays) {
            // Check Sunday skip
            if (skipSundays && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            count++
            val dueDateMs = calendar.timeInMillis
            val dateStr = dateFormat.format(calendar.time)

            installments.add(
                InstallmentEntity(
                    loanId = loanId,
                    clientId = clientId,
                    installmentNumber = count,
                    dueDate = dueDateMs,
                    dueDateFormatted = dateStr,
                    capitalComponent = capitalPerQuota,
                    interestComponent = interestPerQuota,
                    targetAmount = dailyPayment,
                    paidAmount = 0.0,
                    remainingAmount = dailyPayment,
                    lateFee = 0.0,
                    status = "PENDIENTE"
                )
            )

            // Move to next day
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return installments
    }

    fun generateInstallments(
        loanId: Long,
        clientId: Long,
        capital: Double,
        interestAmount: Double,
        totalAmount: Double,
        dailyPayment: Double,
        planType: PlanType,
        startDateMs: Long,
        skipSundays: Boolean
    ): List<InstallmentEntity> {
        return generateInstallments(
            loanId = loanId,
            clientId = clientId,
            capital = capital,
            interestAmount = interestAmount,
            totalAmount = totalAmount,
            dailyPayment = dailyPayment,
            totalDays = planType.totalDays,
            startDateMs = startDateMs,
            skipSundays = skipSundays
        )
    }

    fun calculateRecoveredMetrics(loan: LoanEntity, installments: List<InstallmentEntity>): LoanRecoveredMetrics {
        if (installments.isEmpty()) {
            val ratio = if (loan.totalAmount > 0) (loan.paidAmount / loan.totalAmount).coerceIn(0.0, 1.0) else 0.0
            val recCap = loan.capital * ratio
            val recInt = loan.interestAmount * ratio
            return LoanRecoveredMetrics(
                recoveredCapital = recCap,
                recoveredInterest = recInt,
                remainingCapital = (loan.capital - recCap).coerceAtLeast(0.0),
                remainingInterest = (loan.interestAmount - recInt).coerceAtLeast(0.0)
            )
        }

        var totalRecCap = 0.0
        var totalRecInt = 0.0

        for (inst in installments) {
            if (inst.paidAmount > 0) {
                val ratio = if (inst.targetAmount > 0) (inst.paidAmount / inst.targetAmount).coerceIn(0.0, 1.0) else 0.0
                totalRecCap += inst.capitalComponent * ratio
                totalRecInt += inst.interestComponent * ratio
            }
        }

        return LoanRecoveredMetrics(
            recoveredCapital = totalRecCap,
            recoveredInterest = totalRecInt,
            remainingCapital = (loan.capital - totalRecCap).coerceAtLeast(0.0),
            remainingInterest = (loan.interestAmount - totalRecInt).coerceAtLeast(0.0)
        )
    }

    /**
     * Evaluates overdue installments and applies configured late fee (Mora).
     * Also automatically clears late fees for fully paid installments.
     * Default rule: MORA = 5% sobre la cuota/pago atrasado (fixed percentage per overdue quota, NOT multiplied daily unless PORCENTAJE_DIARIO is explicitly configured).
     */
    fun evaluateLateFees(
        installments: List<InstallmentEntity>,
        lateFeeType: String = "PORCENTAJE_CUOTA",
        lateFeeValue: Double = 0.05, // 5% default
        nowMs: Long = System.currentTimeMillis()
    ): Pair<List<InstallmentEntity>, List<com.example.data.local.LateFeeHistoryEntity>> {
        val updatedList = mutableListOf<InstallmentEntity>()
        val newLateFeeLogs = mutableListOf<com.example.data.local.LateFeeHistoryEntity>()

        for (inst in installments) {
            val isOverdue = nowMs > inst.dueDate && inst.paidAmount < inst.targetAmount
            val daysOver = if (isOverdue) {
                val diffDays = ((nowMs - inst.dueDate) / (1000 * 60 * 60 * 24)).toInt()
                diffDays.coerceAtLeast(1)
            } else 0

            var calculatedFee = inst.lateFee
            var newStatus = inst.status

            if (inst.status == "PAGADO" || inst.remainingAmount <= 0.0) {
                // Eliminate late fee automatically when installment is fully cleared
                calculatedFee = 0.0
                newStatus = "PAGADO"
            } else if (isOverdue) {
                val fee = when (lateFeeType.uppercase()) {
                    "PORCENTAJE_DIARIO" -> {
                        val dailyPct = if (lateFeeValue > 1.0) lateFeeValue / 100.0 else lateFeeValue
                        inst.remainingAmount * dailyPct * daysOver
                    }
                    "FIJO", "MONTO_FIJO" -> lateFeeValue
                    else -> {
                        // "PORCENTAJE_CUOTA", "PORCENTAJE_FIJO_CUOTA", "PORCENTAJE" or default
                        val pct = if (lateFeeValue > 1.0) lateFeeValue / 100.0 else lateFeeValue
                        inst.remainingAmount * pct
                    }
                }
                calculatedFee = Math.ceil(fee * 100.0) / 100.0
                newStatus = "MORA"

                if (inst.lateFee != calculatedFee && calculatedFee > 0.0) {
                    newLateFeeLogs.add(
                        com.example.data.local.LateFeeHistoryEntity(
                            loanId = inst.loanId,
                            installmentId = inst.id,
                            installmentNumber = inst.installmentNumber,
                            amountApplied = calculatedFee,
                            appliedDate = nowMs,
                            status = "ACTIVA",
                            daysOverdueAtApplication = daysOver,
                            notes = "Mora automática ($lateFeeType - ${calculatedFee} MXN)"
                        )
                    )
                }
            } else if (inst.paidAmount > 0.0 && inst.remainingAmount > 0.0) {
                newStatus = "PARCIAL"
            } else {
                newStatus = "PENDIENTE"
            }

            updatedList.add(
                inst.copy(
                    lateFee = calculatedFee,
                    daysOverdue = daysOver,
                    status = newStatus
                )
            )
        }

        return Pair(updatedList, newLateFeeLogs)
    }

    /**
     * Smart Redistribution Engine:
     * When an advance payment or overpayment occurs, redistributes remaining balance
     * exclusively across the remaining future unpaid quotas, without altering the loan's final maturity date.
     */
    fun redistributeScheduleOnAdvance(
        loan: LoanEntity,
        installments: List<InstallmentEntity>,
        paymentAmount: Double,
        redistributeRemaining: Boolean = true,
        nowMs: Long = System.currentTimeMillis()
    ): List<InstallmentEntity> {
        if (installments.isEmpty() || paymentAmount <= 0.0) return installments

        val result = installments.map { it.copy() }.toMutableList()
        var remainingPay = paymentAmount

        // Step 1: Apply payment sequentially to pay late fees and current/past due quotas
        for (i in result.indices) {
            if (remainingPay <= 0.0) break
            val inst = result[i]
            if (inst.status == "PAGADO") continue

            val totalNeeded = inst.remainingAmount + inst.lateFee
            if (remainingPay >= totalNeeded) {
                remainingPay -= totalNeeded
                result[i] = inst.copy(
                    paidAmount = inst.targetAmount,
                    remainingAmount = 0.0,
                    lateFee = 0.0,
                    status = "PAGADO",
                    lastPaymentDate = nowMs
                )
            } else {
                val newPaid = inst.paidAmount + remainingPay
                val newRemaining = (inst.targetAmount - newPaid).coerceAtLeast(0.0)
                result[i] = inst.copy(
                    paidAmount = newPaid,
                    remainingAmount = newRemaining,
                    status = if (newRemaining <= 0.0) "PAGADO" else "PARCIAL",
                    lastPaymentDate = nowMs
                )
                remainingPay = 0.0
            }
        }

        // Step 2: If there's excess payment and user requested smart redistribution across remaining future quotas
        if (redistributeRemaining && remainingPay > 0.0) {
            val futureUnpaidIndices = result.indices.filter { result[it].status != "PAGADO" }
            if (futureUnpaidIndices.isNotEmpty()) {
                val quotaReduction = remainingPay / futureUnpaidIndices.size
                for (idx in futureUnpaidIndices) {
                    val inst = result[idx]
                    val newTarget = (inst.targetAmount - quotaReduction).coerceAtLeast(0.0)
                    val newCap = (inst.capitalComponent * (newTarget / inst.targetAmount)).coerceAtLeast(0.0)
                    val newInt = (inst.interestComponent * (newTarget / inst.targetAmount)).coerceAtLeast(0.0)
                    val newRem = (newTarget - inst.paidAmount).coerceAtLeast(0.0)

                    result[idx] = inst.copy(
                        targetAmount = Math.ceil(newTarget * 100.0) / 100.0,
                        capitalComponent = Math.ceil(newCap * 100.0) / 100.0,
                        interestComponent = Math.ceil(newInt * 100.0) / 100.0,
                        remainingAmount = Math.ceil(newRem * 100.0) / 100.0,
                        status = if (newRem <= 0.0) "PAGADO" else "PENDIENTE"
                    )
                }
            }
        }

        return result
    }
}

object ScoreCalculator {
    fun calculateScore(paidInstallments: Int, overdueInstallments: Int): Int {
        val total = paidInstallments + overdueInstallments
        if (total == 0) return 100
        val ratio = paidInstallments.toDouble() / total.toDouble()
        val score = (ratio * 100).toInt()
        return score.coerceIn(0, 100)
    }
}
