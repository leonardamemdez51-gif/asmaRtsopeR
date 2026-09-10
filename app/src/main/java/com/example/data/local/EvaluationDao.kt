package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientEvaluationDao {
    @Query("SELECT * FROM client_evaluations WHERE clientId = :clientId ORDER BY evaluatedAt DESC")
    fun getEvaluationsForClient(clientId: Long): Flow<List<ClientEvaluationEntity>>

    @Query("SELECT * FROM client_evaluations WHERE clientId = :clientId ORDER BY evaluatedAt DESC LIMIT 1")
    fun getLatestEvaluationForClient(clientId: Long): Flow<ClientEvaluationEntity?>

    @Query("SELECT * FROM client_evaluations WHERE clientId = :clientId ORDER BY evaluatedAt DESC LIMIT 1")
    suspend fun getLatestEvaluationForClientSync(clientId: Long): ClientEvaluationEntity?

    @Query("SELECT * FROM client_evaluations ORDER BY evaluatedAt DESC")
    fun getAllEvaluations(): Flow<List<ClientEvaluationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: ClientEvaluationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualAdjustment(adjustment: ManualScoreAdjustmentEntity): Long

    @Query("SELECT * FROM manual_score_adjustments WHERE clientId = :clientId ORDER BY adjustedAt DESC")
    fun getManualAdjustmentsForClient(clientId: Long): Flow<List<ManualScoreAdjustmentEntity>>

    @Query("SELECT * FROM client_evaluation_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<ClientEvaluationConfigEntity?>

    @Query("SELECT * FROM client_evaluation_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): ClientEvaluationConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: ClientEvaluationConfigEntity)
}
