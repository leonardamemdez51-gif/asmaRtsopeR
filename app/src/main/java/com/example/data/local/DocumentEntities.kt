package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "document_type_configs")
data class DocumentTypeConfigEntity(
    @PrimaryKey val code: String, // e.g., "FOTO_PERSONAL", "INE_FRENTE", "INE_REVERSO"
    val name: String,
    val description: String = "",
    val isRequiredForClient: Boolean = false,
    val isRequiredForLoan: Boolean = false,
    val isRequiredForRenewal: Boolean = false,
    val isRequiredForCollection: Boolean = false,
    val allowMultiple: Boolean = false,
    val iconName: String = "Badge",
    val isSystemDefault: Boolean = true,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val requiresValidation: Boolean = false,
    val allowsPhoto: Boolean = true,
    val allowsFile: Boolean = true,
    val hasValidityLimit: Boolean = false,
    val blocksOperationsIfMissing: Boolean = false
)

@Entity(
    tableName = "client_documents",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["clientId"]),
        Index(value = ["typeCode"]),
        Index(value = ["status"])
    ]
)
data class ClientDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val clientName: String,
    val loanId: Long? = null,
    val visitId: Long? = null,
    val paymentId: Long? = null,
    val typeCode: String, // "FOTO_PERSONAL", "INE_FRENTE", "INE_REVERSO", "CURP", etc.
    val typeName: String,
    val title: String,
    val filePath: String, // Secure internal storage path
    val thumbnailPath: String? = null,
    val fileSizeByte: Long = 0L,
    val fileChecksumSha256: String = "",
    val mimeType: String = "image/jpeg",
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val capturedByUserId: Long = 1L,
    val capturedByUsername: String = "admin",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val status: String = "ACTIVO", // "ACTIVO", "REMPLAZADO", "OBSOLETO", "RECHAZADO"
    val verificationStatus: String = "PENDIENTE", // "PENDIENTE", "VERIFICADO", "RECHAZADO"
    val verificationNotes: String = "",
    val verifiedByUsername: String = "",
    val verifiedAtMs: Long? = null,
    val syncStatus: String = "SINCRONIZADO", // "SINCRONIZADO", "PENDIENTE_SYNC", "ERROR"
    val notes: String = "",
    val versionNumber: Int = 1,
    val replacedDocumentId: Long? = null,
    val issueDateMs: Long? = null,
    val expiryDateMs: Long? = null
)

@Entity(tableName = "document_audit_logs")
data class DocumentAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val clientId: Long,
    val action: String, // "CARGA", "CONSULTA", "REMPLAZO", "ELIMINACION", "VERIFICACION", "DESCARGA_SEGURA"
    val userId: Long,
    val username: String,
    val userRole: String = "USUARIO",
    val timestampMs: Long = System.currentTimeMillis(),
    val details: String = "",
    val ipDevice: String = "Android Mobile Terminal"
)

@Entity(tableName = "document_sync_queue")
data class DocumentSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val clientId: Long,
    val localFilePath: String,
    val actionType: String, // "UPLOAD", "UPDATE", "DELETE"
    val attemptCount: Int = 0,
    val lastAttemptMs: Long = 0L,
    val status: String = "PENDIENTE", // "PENDIENTE", "EN_PROCESO", "COMPLETADO", "ERROR"
    val errorMessage: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)
