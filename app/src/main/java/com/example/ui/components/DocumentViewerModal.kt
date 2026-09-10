package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.ClientDocumentEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerModal(
    document: ClientDocumentEntity,
    viewModel: MainViewModel,
    userRole: String,
    currentUsername: String,
    currentUserId: Long,
    onDismiss: () -> Unit,
    onReplaceRequest: (ClientDocumentEntity) -> Unit,
    onDocumentUpdated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }

    var showVerifyDialog by remember { mutableStateOf(false) }
    var verifyApproved by remember { mutableStateOf(true) }
    var verifyNotes by remember { mutableStateOf("") }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyList by remember { mutableStateOf<List<ClientDocumentEntity>>(emptyList()) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    // Audit log on view
    LaunchedEffect(document.id) {
        viewModel.docRepo.auditDocumentAccess(
            documentId = document.id,
            clientId = document.clientId,
            userId = currentUserId,
            username = currentUsername,
            userRole = userRole,
            actionType = "CONSULTA"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = document.typeName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${document.clientName} • v${document.versionNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_doc_viewer")) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        // Status badge
                        Surface(
                            color = when (document.verificationStatus) {
                                "VERIFICADO" -> Color(0xFF10B981)
                                "RECHAZADO" -> Color(0xFFEF4444)
                                else -> Color(0xFFF59E0B)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = document.verificationStatus,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                )

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Document Preview Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                val imageFile = File(document.filePath)
                                if (imageFile.exists()) {
                                    AsyncImage(
                                        model = imageFile,
                                        contentDescription = document.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Archivo no encontrado en almacenamiento local", color = Color.LightGray, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Security & Metadata Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "🔒 Información de Seguridad y Metadatos",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider()

                                MetaRow("ID Documento", "#${document.id}")
                                MetaRow("Título Archivo", document.title)
                                MetaRow("Tipo de Documento", document.typeName)
                                MetaRow("Tamaño", "${document.fileSizeByte / 1024} KB (${document.fileSizeByte} B)")
                                MetaRow("Dimensiones", "${document.imageWidth} x ${document.imageHeight} px")
                                MetaRow("MIME Type", document.mimeType)
                                MetaRow("Hash SHA-256 (Integridad)", document.fileChecksumSha256.take(24) + "...", isMono = true)
                                MetaRow("Cargado por", "${document.capturedByUsername} (#${document.capturedByUserId})")
                                MetaRow("Fecha Carga", dateFormat.format(Date(document.createdAtMs)))
                                 document.issueDateMs?.let {
                                     MetaRow("Fecha Emisión", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)))
                                 }
                                 document.expiryDateMs?.let {
                                     MetaRow("Fecha Vencimiento", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)))
                                 }
                                MetaRow("Estado Registro", document.status)
                                MetaRow("Sincronización Cloud", document.syncStatus)

                                if (document.notes.isNotBlank()) {
                                    MetaRow("Notas / Comentarios", document.notes)
                                }

                                if (document.latitude != null && document.longitude != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val uri = "geo:${document.latitude},${document.longitude}?q=${document.latitude},${document.longitude}(${Uri.encode(document.title)})"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ver Ubicación GPS de Captura (Lat: ${document.latitude}, Lng: ${document.longitude})", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Verification Info Card
                    if (document.verifiedByUsername.isNotBlank()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (document.verificationStatus == "VERIFICADO") Color(0xFFECFDF5) else Color(0xFFFEF2F2))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Auditoría de Verificación",
                                        fontWeight = FontWeight.Bold,
                                        color = if (document.verificationStatus == "VERIFICADO") Color(0xFF065F46) else Color(0xFF991B1B)
                                    )
                                    Text("Verificado por: ${document.verifiedByUsername}", fontSize = 12.sp, color = Color.Black)
                                    if (document.verifiedAtMs != null) {
                                        Text("Fecha: ${dateFormat.format(Date(document.verifiedAtMs))}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    if (document.verificationNotes.isNotBlank()) {
                                        Text("Observaciones: ${document.verificationNotes}", fontSize = 12.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }

                    // Actions Bar
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onReplaceRequest(document) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_replace_doc"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reemplazar")
                                }

                                if (userRole in listOf("ADMINISTRADOR", "SUPERVISOR")) {
                                    Button(
                                        onClick = { showVerifyDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_verify_doc"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verificar")
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val history = viewModel.docRepo.getDocumentHistory(document.replacedDocumentId ?: document.id)
                                            historyList = history
                                            showHistoryDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Historial Versiones")
                                }

                                if (userRole in listOf("ADMINISTRADOR", "SUPERVISOR")) {
                                    OutlinedButton(
                                        onClick = { showDeleteDialog = true },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_delete_doc"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Eliminar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Modal
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación de Documento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Esta acción eliminará el documento sensible del almacenamiento privado y registrará un evento de auditoría de seguridad.")
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Motivo de eliminación (Obligatorio)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteReason.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.docRepo.deleteDocument(
                                    documentId = document.id,
                                    userId = currentUserId,
                                    username = currentUsername,
                                    userRole = userRole,
                                    reason = deleteReason
                                )
                                Toast.makeText(context, "Documento eliminado", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                                onDocumentUpdated()
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = deleteReason.isNotBlank()
                ) {
                    Text("Eliminar Definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Verify Modal
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Verificación de Documento Oficial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Evalúa la validez y legibilidad del documento:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = verifyApproved,
                            onClick = { verifyApproved = true },
                            label = { Text("Aprobar") },
                            leadingIcon = { if (verifyApproved) Icon(Icons.Default.Check, contentDescription = null) }
                        )
                        FilterChip(
                            selected = !verifyApproved,
                            onClick = { verifyApproved = false },
                            label = { Text("Rechazar") },
                            leadingIcon = { if (!verifyApproved) Icon(Icons.Default.Close, contentDescription = null) }
                        )
                    }
                    OutlinedTextField(
                        value = verifyNotes,
                        onValueChange = { verifyNotes = it },
                        label = { Text(if (verifyApproved) "Notas de dictamen / observaciones (Opcional)" else "Motivo de rechazo (Obligatorio)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.verifyDocument(
                                documentId = document.id,
                                verifiedByUserId = currentUserId,
                                verifiedByUsername = currentUsername,
                                userRole = userRole,
                                isApproved = verifyApproved,
                                notes = verifyNotes,
                                onDone = {
                                    Toast.makeText(context, "Verificación guardada", Toast.LENGTH_SHORT).show()
                                    showVerifyDialog = false
                                    onDocumentUpdated()
                                    onDismiss()
                                }
                            )
                        }
                    },
                    enabled = verifyApproved || verifyNotes.isNotBlank()
                ) {
                    Text("Guardar Dictamen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Version History Dialog
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Historial de Versiones (${historyList.size})") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (item.status == "ACTIVO") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Versión v${item.versionNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(item.status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (item.status == "ACTIVO") Color(0xFF10B981) else Color.Gray)
                                }
                                Text("Fecha: ${dateFormat.format(Date(item.createdAtMs))}", fontSize = 11.sp, color = Color.Gray)
                                Text("Cargado por: ${item.capturedByUsername}", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default
            )
        )
    }
}
