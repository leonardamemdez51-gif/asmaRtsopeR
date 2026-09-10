package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.*
import com.example.util.DocumentStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class MandatoryDocumentsStatus(
    val isComplete: Boolean,
    val missingTypes: List<DocumentTypeConfigEntity>
)

class DocumentRepository(
    private val db: AppDatabase,
    private val documentDao: DocumentDao = db.documentDao(),
    private val clientDao: ClientDao = db.clientDao()
) {

    val activeDocumentTypes: Flow<List<DocumentTypeConfigEntity>> = documentDao.getActiveDocumentTypes()
    val allDocumentTypes: Flow<List<DocumentTypeConfigEntity>> = documentDao.getAllDocumentTypes()

    fun getDocumentsByClient(clientId: Long): Flow<List<ClientDocumentEntity>> =
        documentDao.getDocumentsByClient(clientId)

    fun getDocumentsByLoan(loanId: Long): Flow<List<ClientDocumentEntity>> =
        documentDao.getDocumentsByLoan(loanId)

    fun getAuditLogsByClient(clientId: Long): Flow<List<DocumentAuditLogEntity>> =
        documentDao.getAuditLogsByClient(clientId)

    suspend fun getDocumentById(id: Long): ClientDocumentEntity? =
        documentDao.getDocumentById(id)

    suspend fun getDocumentHistory(originalDocId: Long): List<ClientDocumentEntity> = withContext(Dispatchers.IO) {
        documentDao.getDocumentHistory(originalDocId)
    }

    suspend fun uploadOrCaptureDocument(
        context: Context,
        sourceUri: Uri,
        typeCode: String,
        clientId: Long,
        clientName: String,
        loanId: Long? = null,
        visitId: Long? = null,
        paymentId: Long? = null,
        capturedByUserId: Long = 1L,
        capturedByUsername: String = "admin",
        userRole: String = "ADMINISTRADOR",
        notes: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        isOfflineCapture: Boolean = false,
        issueDateMs: Long? = null,
        expiryDateMs: Long? = null
    ): Result<ClientDocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val docType = documentDao.getDocumentTypeByCode(typeCode)
                ?: DocumentTypeConfigEntity(code = typeCode, name = typeCode)

            val isOfficialId = typeCode in listOf("INE_FRENTE", "INE_REVERSO", "CURP")

            val stored = DocumentStorageManager.saveDocumentImage(
                context = context,
                sourceUri = sourceUri,
                typeCode = typeCode,
                clientId = clientId,
                isOfficialId = isOfficialId
            ) ?: return@withContext Result.failure(Exception("Error al procesar y comprimir la imagen"))

            val previousDoc = documentDao.getLatestDocumentByType(clientId, typeCode)
            val versionNum = if (previousDoc != null) previousDoc.versionNumber + 1 else 1

            val syncStatusStr = if (isOfflineCapture) "PENDIENTE_SYNC" else "SINCRONIZADO"

            // Compute verificationStatus based on dynamic requiresValidation configuration
            val needsValidation = docType.requiresValidation
            val initialStatus = if (needsValidation) {
                "PENDIENTE"
            } else {
                "CARGADO"
            }

            val isAutoVerified = !needsValidation || (userRole in listOf("ADMINISTRADOR", "SUPERVISOR"))
            val finalVerificationStatus = if (isAutoVerified) "VERIFICADO" else initialStatus
            val autoNotes = if (isAutoVerified) "Verificado automáticamente al cargar o no requiere validación" else ""

            val newDoc = ClientDocumentEntity(
                clientId = clientId,
                clientName = clientName,
                loanId = loanId,
                visitId = visitId,
                paymentId = paymentId,
                typeCode = typeCode,
                typeName = docType.name,
                title = "${docType.name}_${clientName.replace(" ", "_")}_v$versionNum.jpg",
                filePath = stored.filePath,
                thumbnailPath = stored.thumbnailPath,
                fileSizeByte = stored.fileSizeByte,
                fileChecksumSha256 = stored.checksumSha256,
                mimeType = "image/jpeg",
                imageWidth = stored.width,
                imageHeight = stored.height,
                latitude = latitude,
                longitude = longitude,
                capturedByUserId = capturedByUserId,
                capturedByUsername = capturedByUsername,
                createdAtMs = System.currentTimeMillis(),
                updatedAtMs = System.currentTimeMillis(),
                status = "ACTIVO",
                verificationStatus = finalVerificationStatus,
                verificationNotes = autoNotes,
                verifiedByUsername = if (isAutoVerified) capturedByUsername else "",
                verifiedAtMs = if (isAutoVerified) System.currentTimeMillis() else null,
                syncStatus = syncStatusStr,
                notes = notes,
                versionNumber = versionNum,
                replacedDocumentId = previousDoc?.id,
                issueDateMs = issueDateMs,
                expiryDateMs = expiryDateMs
            )

            val newDocId = documentDao.insertDocument(newDoc)

            // If not allowing multiple, mark previous active documents of this type as REMPLAZADO
            if (!docType.allowMultiple && previousDoc != null) {
                documentDao.markPreviousDocumentsAsReplaced(clientId, typeCode, newDocId)
            }

            // Insert audit log
            val actionLabel = if (previousDoc != null) "REMPLAZO" else "CARGA"
            documentDao.insertAuditLog(
                DocumentAuditLogEntity(
                    documentId = newDocId,
                    clientId = clientId,
                    action = actionLabel,
                    userId = capturedByUserId,
                    username = capturedByUsername,
                    userRole = userRole,
                    details = "Documento '${docType.name}' $actionLabel v$versionNum (${stored.fileSizeByte / 1024} KB, Hash: ${stored.checksumSha256.take(8)}...)"
                )
            )

            // Insert into sync queue
            documentDao.insertSyncItem(
                DocumentSyncQueueEntity(
                    documentId = newDocId,
                    clientId = clientId,
                    localFilePath = stored.filePath,
                    actionType = "UPLOAD",
                    status = if (isOfflineCapture) "PENDIENTE" else "COMPLETADO"
                )
            )

            // Keep ClientEntity legacy URI fields updated if matching standard fields
            val client = clientDao.getClientById(clientId)
            if (client != null) {
                val updatedClient = when (typeCode) {
                    "FOTO_PERSONAL" -> client.copy(photoUri = stored.filePath)
                    "INE_FRENTE" -> client.copy(ineFrontUri = stored.filePath)
                    "INE_REVERSO" -> client.copy(ineBackUri = stored.filePath)
                    "COMPROBANTE_DOMICILIO" -> client.copy(proofOfAddressUri = stored.filePath)
                    "EVIDENCIA_DOMICILIO" -> client.copy(housePhotoUri = stored.filePath)
                    "FIRMA" -> client.copy(signatureUri = stored.filePath)
                    else -> client
                }
                if (updatedClient != client) {
                    clientDao.updateClient(updatedClient)
                }
            }

            Result.success(newDoc.copy(id = newDocId))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun verifyDocument(
        documentId: Long,
        verifiedByUserId: Long,
        verifiedByUsername: String,
        userRole: String,
        isApproved: Boolean,
        notes: String
    ): Boolean = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext false
        val newStatus = if (isApproved) "VERIFICADO" else "RECHAZADO"

        val updated = doc.copy(
            verificationStatus = newStatus,
            verificationNotes = notes,
            verifiedByUsername = verifiedByUsername,
            verifiedAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis()
        )
        documentDao.updateDocument(updated)

        documentDao.insertAuditLog(
            DocumentAuditLogEntity(
                documentId = documentId,
                clientId = doc.clientId,
                action = "VERIFICACION",
                userId = verifiedByUserId,
                username = verifiedByUsername,
                userRole = userRole,
                details = "Estado de verificación cambiado a $newStatus. Notas: $notes"
            )
        )
        true
    }

    suspend fun deleteDocument(
        documentId: Long,
        userId: Long,
        username: String,
        userRole: String,
        reason: String
    ): Boolean = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext false
        if (userRole !in listOf("ADMINISTRADOR", "SUPERVISOR")) {
            return@withContext false // Permission control
        }

        val updated = doc.copy(
            status = "ELIMINADO",
            updatedAtMs = System.currentTimeMillis(),
            notes = "Eliminado por $username. Razón: $reason"
        )
        documentDao.updateDocument(updated)

        // Delete secure local files
        DocumentStorageManager.deleteFile(doc.filePath)
        doc.thumbnailPath?.let { DocumentStorageManager.deleteFile(it) }

        documentDao.insertAuditLog(
            DocumentAuditLogEntity(
                documentId = documentId,
                clientId = doc.clientId,
                action = "ELIMINACION",
                userId = userId,
                username = username,
                userRole = userRole,
                details = "Documento '${doc.typeName}' eliminado. Motivo: $reason"
            )
        )
        true
    }

    suspend fun auditDocumentAccess(
        documentId: Long,
        clientId: Long,
        userId: Long,
        username: String,
        userRole: String,
        actionType: String = "CONSULTA"
    ) = withContext(Dispatchers.IO) {
        documentDao.insertAuditLog(
            DocumentAuditLogEntity(
                documentId = documentId,
                clientId = clientId,
                action = actionType,
                userId = userId,
                username = username,
                userRole = userRole,
                details = "Acceso/Consulta de documento sensible"
            )
        )
    }

    suspend fun checkMandatoryDocumentsCompliance(
        clientId: Long,
        isLoanRequest: Boolean = false,
        isRenewal: Boolean = false
    ): MandatoryDocumentsStatus = withContext(Dispatchers.IO) {
        val activeTypes = documentDao.getActiveDocumentTypesSync()
        val clientDocs = documentDao.getActiveDocumentsByClientSync(clientId)
        val uploadedTypeCodes = clientDocs.map { it.typeCode }.toSet()

        val requiredTypes = activeTypes.filter { type ->
            when {
                isRenewal -> type.isRequiredForRenewal
                isLoanRequest -> type.isRequiredForLoan
                else -> type.isRequiredForClient
            }
        }

        val missing = requiredTypes.filter { it.code !in uploadedTypeCodes }

        MandatoryDocumentsStatus(
            isComplete = missing.isEmpty(),
            missingTypes = missing
        )
    }

    suspend fun saveDocumentTypeConfig(config: DocumentTypeConfigEntity) = withContext(Dispatchers.IO) {
        documentDao.insertDocumentType(config)
    }

    suspend fun syncPendingDocuments(): Int = withContext(Dispatchers.IO) {
        val pendingItems = documentDao.getPendingSyncItems()
        var syncedCount = 0
        for (item in pendingItems) {
            val doc = documentDao.getDocumentById(item.documentId)
            if (doc != null) {
                val updatedDoc = doc.copy(syncStatus = "SINCRONIZADO")
                documentDao.updateDocument(updatedDoc)
                documentDao.updateSyncItem(item.copy(status = "COMPLETADO"))
                syncedCount++
            }
        }
        syncedCount
    }
}
