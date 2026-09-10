package com.example.data.repository

import com.example.data.local.*
import com.example.domain.CustomerEvaluationEngine
import com.example.domain.EvaluationResult
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

class EvaluationRepository(
    private val evaluationDao: ClientEvaluationDao,
    private val auditDao: AuditLogDao
) {
    fun getEvaluationsForClient(clientId: Long): Flow<List<ClientEvaluationEntity>> {
        return evaluationDao.getEvaluationsForClient(clientId)
    }

    fun getLatestEvaluationForClient(clientId: Long): Flow<ClientEvaluationEntity?> {
        return evaluationDao.getLatestEvaluationForClient(clientId)
    }

    suspend fun getLatestEvaluationForClientSync(clientId: Long): ClientEvaluationEntity? {
        return evaluationDao.getLatestEvaluationForClientSync(clientId)
    }

    fun getAllEvaluations(): Flow<List<ClientEvaluationEntity>> {
        return evaluationDao.getAllEvaluations()
    }

    fun getConfig(): Flow<ClientEvaluationConfigEntity?> {
        return evaluationDao.getConfig()
    }

    suspend fun getConfigSync(): ClientEvaluationConfigEntity {
        return evaluationDao.getConfigSync() ?: ClientEvaluationConfigEntity()
    }

    suspend fun saveConfig(config: ClientEvaluationConfigEntity, user: UserEntity?) {
        evaluationDao.insertOrUpdateConfig(config)
        if (user != null) {
            auditDao.insertAuditLog(
                AuditLogEntity(
                    userId = user.id,
                    username = user.username,
                    action = "MODIFICAR_CONFIG_EVALUACION",
                    entityType = "CONFIGURACION_SCORE",
                    entityId = "1",
                    newValues = "Configuración de motor de score y evaluación actualizada"
                )
            )
        }
    }

    suspend fun evaluateAndSaveClient(
        client: ClientEntity,
        loans: List<LoanEntity>,
        installments: List<InstallmentEntity>,
        payments: List<PaymentEntity>,
        visits: List<CollectionVisitEntity>,
        user: UserEntity?,
        systemConfig: SystemConfigEntity = SystemConfigEntity()
    ): ClientEvaluationEntity {
        val config = getConfigSync()
        val previous = evaluationDao.getLatestEvaluationForClientSync(client.id)

        val evalResult = CustomerEvaluationEngine.evaluateClient(
            client = client,
            loans = loans,
            installments = installments,
            payments = payments,
            visits = visits,
            previousEvaluation = previous,
            config = config,
            systemConfig = systemConfig
        )

        val entity = ClientEvaluationEntity(
            clientId = client.id,
            score = evalResult.score,
            classification = evalResult.classification,
            previousScore = previous?.score,
            trend = evalResult.trend,
            recommendation = evalResult.recommendation,
            minRecommendedAmount = evalResult.minRecommendedAmount,
            maxRecommendedAmount = evalResult.maxRecommendedAmount,
            suggestedAmount = evalResult.suggestedAmount,
            recommendedPlanCode = evalResult.recommendedPlanCode,
            recommendedPlanMessage = evalResult.recommendedPlanMessage,
            positiveFactorsJson = JSONArray(evalResult.positiveFactors).toString(),
            negativeFactorsJson = JSONArray(evalResult.negativeFactors).toString(),
            reasonsJson = JSONArray(evalResult.explanationReasons).toString(),
            alertsJson = JSONArray(evalResult.alerts).toString(),
            compliancePercentage = evalResult.compliancePercentage,
            totalLoansCount = evalResult.totalLoansCount,
            activeLoansCount = evalResult.activeLoansCount,
            activeBalanceTotal = evalResult.activeBalanceTotal,
            activeMoraTotal = evalResult.activeMoraTotal,
            evaluatedAt = System.currentTimeMillis(),
            evaluatedByUserId = user?.id,
            evaluatedByUsername = user?.username ?: "SISTEMA",
            isManualAdjustment = false
        )

        val newId = evaluationDao.insertEvaluation(entity)
        return entity.copy(id = newId)
    }

    suspend fun adjustScoreManually(
        clientId: Long,
        newScore: Int,
        reason: String,
        observation: String,
        user: UserEntity
    ): ClientEvaluationEntity {
        require(reason.isNotBlank()) { "El motivo del ajuste manual es obligatorio" }

        val previous = evaluationDao.getLatestEvaluationForClientSync(clientId)
        val prevScore = previous?.score ?: 80

        val config = getConfigSync()
        require(config.allowManualAdjustments) { "Los ajustes manuales de score están desactivados en la configuración del sistema." }

        val classification = when {
            newScore >= config.rangeExcelenteMin -> "EXCELENTE"
            newScore >= config.rangeMuyBuenoMin -> "MUY BUENO"
            newScore >= config.rangeBuenoMin -> "BUENO"
            newScore >= config.rangeRegularMin -> "REGULAR"
            newScore >= config.rangeRiesgoAltoMin -> "RIESGO ALTO"
            else -> "RIESGO CRÍTICO"
        }

        val adjustmentLog = ManualScoreAdjustmentEntity(
            clientId = clientId,
            previousScore = prevScore,
            newScore = newScore,
            reason = reason,
            observation = observation,
            adjustedByUserId = user.id,
            adjustedByUsername = user.username,
            adjustedAt = System.currentTimeMillis()
        )
        evaluationDao.insertManualAdjustment(adjustmentLog)

        val evalEntity = ClientEvaluationEntity(
            clientId = clientId,
            score = newScore,
            classification = classification,
            previousScore = prevScore,
            trend = if (newScore > prevScore) "ASCENDENTE" else if (newScore < prevScore) "DESCENDENTE" else "ESTABLE",
            recommendation = if (newScore >= 70) "RECOMENDADO" else if (newScore >= 50) "RECOMENDADO_CONDICIONADO" else "REQUIERE_REVISION",
            minRecommendedAmount = previous?.minRecommendedAmount ?: 1000.0,
            maxRecommendedAmount = previous?.maxRecommendedAmount ?: 10000.0,
            suggestedAmount = previous?.suggestedAmount ?: 5000.0,
            recommendedPlanCode = previous?.recommendedPlanCode ?: "PLAN_20_DIAS",
            recommendedPlanMessage = "Ajuste manual aplicado por supervisor/administrador.",
            positiveFactorsJson = previous?.positiveFactorsJson ?: "[\"+ Ajuste manual de supervisor\"]",
            negativeFactorsJson = previous?.negativeFactorsJson ?: "[]",
            reasonsJson = JSONArray(listOf("Score ajustado manualmente por ${user.fullName}. Motivo: $reason")).toString(),
            alertsJson = JSONArray(listOf("Ajuste manual registrado")).toString(),
            compliancePercentage = previous?.compliancePercentage ?: 100.0,
            totalLoansCount = previous?.totalLoansCount ?: 0,
            activeLoansCount = previous?.activeLoansCount ?: 0,
            activeBalanceTotal = previous?.activeBalanceTotal ?: 0.0,
            activeMoraTotal = previous?.activeMoraTotal ?: 0.0,
            evaluatedAt = System.currentTimeMillis(),
            evaluatedByUserId = user.id,
            evaluatedByUsername = user.username,
            isManualAdjustment = true,
            manualAdjustmentReason = reason
        )

        val newId = evaluationDao.insertEvaluation(evalEntity)

        // Record Audit Log (Mandatory for security and transparency)
        auditDao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                username = user.username,
                action = "AJUSTE_MANUAL_SCORE",
                entityType = "CLIENTE_SCORE",
                entityId = clientId.toString(),
                previousValues = "Score anterior: $prevScore",
                newValues = "Score nuevo: $newScore, Clasificación: $classification, Motivo: $reason, Obs: $observation"
            )
        )

        return evalEntity.copy(id = newId)
    }

    fun getManualAdjustmentsForClient(clientId: Long): Flow<List<ManualScoreAdjustmentEntity>> {
        return evaluationDao.getManualAdjustmentsForClient(clientId)
    }
}
