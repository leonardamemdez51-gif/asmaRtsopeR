package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "client_references",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"])]
)
data class ClientReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val name: String,
    val relation: String,
    val phone: String,
    val address: String = "",
    val occupation: String = "",
    val type: String = "PERSONAL", // PERSONAL, FAMILIAR, COMERCIAL, LABORAL
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

@Entity(
    tableName = "client_observations",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"])]
)
data class ClientObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val text: String,
    val type: String = "GENERAL", // GENERAL, COBRANZA, VERIFICACION
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

@Entity(
    tableName = "client_alerts",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"])]
)
data class ClientAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val title: String,
    val description: String,
    val priority: String = "MEDIA", // ALTA, MEDIA, BAJA
    val status: String = "ACTIVA", // ACTIVA, RESUELTA, DESCARTADA
    val origin: String = "SISTEMA", // SISTEMA, USUARIO
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
