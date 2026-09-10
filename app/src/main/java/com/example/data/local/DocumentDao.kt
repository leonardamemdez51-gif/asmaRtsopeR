package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // --- Document Type Configs ---
    @Query("SELECT * FROM document_type_configs WHERE active = 1 ORDER BY sortOrder ASC, name ASC")
    fun getActiveDocumentTypes(): Flow<List<DocumentTypeConfigEntity>>

    @Query("SELECT * FROM document_type_configs WHERE active = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getActiveDocumentTypesSync(): List<DocumentTypeConfigEntity>

    @Query("SELECT * FROM document_type_configs ORDER BY sortOrder ASC, name ASC")
    fun getAllDocumentTypes(): Flow<List<DocumentTypeConfigEntity>>

    @Query("SELECT * FROM document_type_configs WHERE code = :code LIMIT 1")
    suspend fun getDocumentTypeByCode(code: String): DocumentTypeConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentType(type: DocumentTypeConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentTypes(types: List<DocumentTypeConfigEntity>)

    @Update
    suspend fun updateDocumentType(type: DocumentTypeConfigEntity)

    @Delete
    suspend fun deleteDocumentType(type: DocumentTypeConfigEntity)


    // --- Client Documents ---
    @Query("SELECT * FROM client_documents WHERE clientId = :clientId ORDER BY createdAtMs DESC")
    fun getDocumentsByClient(clientId: Long): Flow<List<ClientDocumentEntity>>

    @Query("SELECT * FROM client_documents WHERE clientId = :clientId AND status = 'ACTIVO' ORDER BY createdAtMs DESC")
    fun getActiveDocumentsByClient(clientId: Long): Flow<List<ClientDocumentEntity>>

    @Query("SELECT * FROM client_documents WHERE clientId = :clientId AND status = 'ACTIVO'")
    suspend fun getActiveDocumentsByClientSync(clientId: Long): List<ClientDocumentEntity>

    @Query("SELECT * FROM client_documents WHERE loanId = :loanId AND status = 'ACTIVO' ORDER BY createdAtMs DESC")
    fun getDocumentsByLoan(loanId: Long): Flow<List<ClientDocumentEntity>>

    @Query("SELECT * FROM client_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): ClientDocumentEntity?

    @Query("SELECT * FROM client_documents WHERE clientId = :clientId AND typeCode = :typeCode AND status = 'ACTIVO' ORDER BY createdAtMs DESC LIMIT 1")
    suspend fun getLatestDocumentByType(clientId: Long, typeCode: String): ClientDocumentEntity?

    @Query("SELECT * FROM client_documents WHERE replacedDocumentId = :originalDocId OR id = :originalDocId ORDER BY versionNumber ASC")
    suspend fun getDocumentHistory(originalDocId: Long): List<ClientDocumentEntity>

    @Query("SELECT * FROM client_documents WHERE syncStatus = 'PENDIENTE_SYNC'")
    suspend fun getPendingSyncDocuments(): List<ClientDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ClientDocumentEntity): Long

    @Update
    suspend fun updateDocument(document: ClientDocumentEntity)

    @Delete
    suspend fun deleteDocument(document: ClientDocumentEntity)

    @Query("UPDATE client_documents SET status = 'REMPLAZADO' WHERE clientId = :clientId AND typeCode = :typeCode AND id != :newDocId AND status = 'ACTIVO'")
    suspend fun markPreviousDocumentsAsReplaced(clientId: Long, typeCode: String, newDocId: Long)


    // --- Document Audit Logs ---
    @Query("SELECT * FROM document_audit_logs WHERE clientId = :clientId ORDER BY timestampMs DESC")
    fun getAuditLogsByClient(clientId: Long): Flow<List<DocumentAuditLogEntity>>

    @Query("SELECT * FROM document_audit_logs WHERE documentId = :documentId ORDER BY timestampMs DESC")
    fun getAuditLogsByDocument(documentId: Long): Flow<List<DocumentAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: DocumentAuditLogEntity): Long


    // --- Document Sync Queue ---
    @Query("SELECT * FROM document_sync_queue WHERE status = 'PENDIENTE' ORDER BY createdAtMs ASC")
    suspend fun getPendingSyncItems(): List<DocumentSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: DocumentSyncQueueEntity): Long

    @Update
    suspend fun updateSyncItem(item: DocumentSyncQueueEntity)

    @Query("DELETE FROM document_sync_queue WHERE id = :id")
    suspend fun deleteSyncItem(id: Long)
}
