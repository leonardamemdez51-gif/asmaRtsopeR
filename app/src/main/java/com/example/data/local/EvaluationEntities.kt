package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "client_evaluations",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"]), Index(value = ["evaluatedAt"])]
)
data class ClientEvaluationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val score: Int, // 0 to 100
    val classification: String, // "EXCELENTE", "MUY_BUENO", "BUENO", "REGULAR", "RIESGO_ALTO", "RIESGO_CRITICO"
    val previousScore: Int? = null,
    val trend: String = "ESTABLE", // "ASCENDENTE", "DESCENDENTE", "ESTABLE"
    val recommendation: String = "RECOMENDADO", // "RECOMENDADO", "RECOMENDADO_CONDICIONADO", "REQUIERE_REVISION", "NO_RECOMENDADO"
    val minRecommendedAmount: Double = 1000.0,
    val maxRecommendedAmount: Double = 10000.0,
    val suggestedAmount: Double = 5000.0,
    val recommendedPlanCode: String = "PLAN_20_DIAS",
    val recommendedPlanMessage: String = "",
    val positiveFactorsJson: String = "[]",
    val negativeFactorsJson: String = "[]",
    val reasonsJson: String = "[]",
    val alertsJson: String = "[]",
    val compliancePercentage: Double = 100.0,
    val totalLoansCount: Int = 0,
    val activeLoansCount: Int = 0,
    val activeBalanceTotal: Double = 0.0,
    val activeMoraTotal: Double = 0.0,
    val evaluatedAt: Long = System.currentTimeMillis(),
    val evaluatedByUserId: Long? = null,
    val evaluatedByUsername: String? = "SISTEMA",
    val isManualAdjustment: Boolean = false,
    val manualAdjustmentReason: String? = null
)

@Entity(
    tableName = "manual_score_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"]), Index(value = ["adjustedAt"])]
)
data class ManualScoreAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val previousScore: Int,
    val newScore: Int,
    val reason: String, // Mandatory reason
    val observation: String = "",
    val adjustedByUserId: Long,
    val adjustedByUsername: String,
    val adjustedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "client_evaluation_config")
data class ClientEvaluationConfigEntity(
    @PrimaryKey val id: Int = 1,
    val enableScore: Boolean = true,
    // Score Ranges (0-100)
    val rangeExcelenteMin: Int = 90,
    val rangeExcelenteMax: Int = 100,
    val rangeMuyBuenoMin: Int = 80,
    val rangeMuyBuenoMax: Int = 89,
    val rangeBuenoMin: Int = 70,
    val rangeBuenoMax: Int = 79,
    val rangeRegularMin: Int = 60,
    val rangeRegularMax: Int = 69,
    val rangeRiesgoAltoMin: Int = 40,
    val rangeRiesgoAltoMax: Int = 59,
    val rangeRiesgoCriticoMin: Int = 0,
    val rangeRiesgoCriticoMax: Int = 39,
    // Factor Weights (in percentage, total 100)
    val weightPaymentHistory: Double = 30.0,
    val weightMora: Double = 25.0,
    val weightLoansHistory: Double = 15.0,
    val weightCompliancePct: Double = 15.0,
    val weightTenure: Double = 5.0,
    val weightCollectionBehavior: Double = 10.0,
    // Settings
    val allowManualAdjustments: Boolean = true,
    val historicPeriodDays: Int = 180, // 0 for all time, or 30, 90, 180, 365
    val maxActiveLoansPerClient: Int = 2
)
