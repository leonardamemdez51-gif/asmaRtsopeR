package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ClientDocumentEntity
import com.example.data.local.ClientEntity
import com.example.data.local.DocumentTypeConfigEntity
import com.example.data.repository.MandatoryDocumentsStatus
import com.example.ui.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentExpedienteSection(
    client: ClientEntity,
    viewModel: MainViewModel,
    userRole: String,
    currentUsername: String,
    currentUserId: Long,
    onNavigate: (String) -> Unit
) {
    var showCaptureModal by remember { mutableStateOf(false) }
    var selectedTypeCodeForCapture by remember { mutableStateOf<String?>(null) }
    var viewingDocument by remember { mutableStateOf<ClientDocumentEntity?>(null) }

    val documents by viewModel.docRepo.getDocumentsByClient(client.id).collectAsState(initial = emptyList())
    val docTypes by viewModel.docRepo.activeDocumentTypes.collectAsState(initial = emptyList())

    var activeFilter by remember { mutableStateOf("TODOS") } // "TODOS", "OBLIGATORIOS", "IDENTIFICACIONES", "EVIDENCIAS", "VERIFICADOS"
    var isGridView by remember { mutableStateOf(true) }

    var complianceStatus by remember { mutableStateOf<MandatoryDocumentsStatus?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Re-check compliance
    LaunchedEffect(documents) {
        complianceStatus = viewModel.docRepo.checkMandatoryDocumentsCompliance(client.id)
    }

    val activeDocs = remember(documents) {
        documents.filter { it.status == "ACTIVO" }
    }

    val filteredDocs = remember(activeDocs, activeFilter) {
        when (activeFilter) {
            "OBLIGATORIOS" -> activeDocs.filter { doc ->
                val type = docTypes.find { it.code == doc.typeCode }
                type?.isRequiredForClient == true || type?.isRequiredForLoan == true
            }
            "IDENTIFICACIONES" -> activeDocs.filter { it.typeCode in listOf("INE_FRENTE", "INE_REVERSO", "CURP", "FOTO_PERSONAL") }
            "EVIDENCIAS" -> activeDocs.filter { it.typeCode in listOf("EVIDENCIA_DOMICILIO", "EVIDENCIA_NEGOCIO", "EVIDENCIA_VISITA", "COMPROBANTE_TRANSFERENCIA") }
            "VERIFICADOS" -> activeDocs.filter { it.verificationStatus == "VERIFICADO" }
            else -> activeDocs
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // 1. COMPLIANCE BANNER
        val comp = complianceStatus
        if (comp != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (comp.isComplete) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (comp.isComplete) Icons.Default.VerifiedUser else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (comp.isComplete) Color(0xFF047857) else Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (comp.isComplete) "EXPEDIENTE DIGITAL COMPLETO" else "EXPEDIENTE INCOMPLETO",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (comp.isComplete) Color(0xFF047857) else Color(0xFFB45309)
                            )
                        }

                        Button(
                            onClick = {
                                selectedTypeCodeForCapture = comp.missingTypes.firstOrNull()?.code
                                showCaptureModal = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (comp.isComplete) Color(0xFF047857) else Color(0xFFD97706)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (comp.isComplete) "Agregar Extra" else "Capturar Faltante", fontSize = 12.sp)
                        }
                    }

                    if (!comp.isComplete) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Faltan ${comp.missingTypes.size} documento(s) obligatorio(s) para otorgar préstamos:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            comp.missingTypes.forEach { missingType ->
                                SuggestionChip(
                                    onClick = {
                                        selectedTypeCodeForCapture = missingType.code
                                        showCaptureModal = true
                                    },
                                    label = { Text("⚡ ${missingType.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFEF3C7))
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. TOOLBAR & FILTERS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Documentos Registrados (${activeDocs.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.List else Icons.Default.GridView,
                                contentDescription = "Cambiar vista"
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedTypeCodeForCapture = null
                                showCaptureModal = true
                            },
                            modifier = Modifier.testTag("btn_open_doc_capture")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Agregar Documento", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider()

                // Filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("TODOS", "OBLIGATORIOS", "IDENTIFICACIONES", "EVIDENCIAS", "VERIFICADOS")
                    items(filters) { f ->
                        FilterChip(
                            selected = activeFilter == f,
                            onClick = { activeFilter = f },
                            label = { Text(f, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 3. DOCUMENTS CONTENT
        if (filteredDocs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No se encontraron documentos en este filtro.", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Presiona + para tomar una foto o cargar desde la galería.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            if (isGridView) {
                // Miniatures Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredDocs.chunked(2).forEach { rowDocs ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowDocs.forEach { doc ->
                                Box(modifier = Modifier.weight(1f)) {
                                    GridDocumentCard(
                                        doc = doc,
                                        dateFormat = dateFormat,
                                        onClick = { viewingDocument = doc }
                                    )
                                }
                            }
                            if (rowDocs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // List View
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredDocs.forEach { doc ->
                        ListDocumentCard(
                            doc = doc,
                            dateFormat = dateFormat,
                            onClick = { viewingDocument = doc }
                        )
                    }
                }
            }
        }
    }

    // Capture Modal
    if (showCaptureModal) {
        DocumentCaptureModal(
            clientId = client.id,
            clientName = client.fullName,
            preselectedTypeCode = selectedTypeCodeForCapture,
            viewModel = viewModel,
            onDismiss = { showCaptureModal = false },
            onDocumentCaptured = {
                showCaptureModal = false
            }
        )
    }

    // Viewer Modal
    val activeDoc = viewingDocument
    if (activeDoc != null) {
        DocumentViewerModal(
            document = activeDoc,
            viewModel = viewModel,
            userRole = userRole,
            currentUsername = currentUsername,
            currentUserId = currentUserId,
            onDismiss = { viewingDocument = null },
            onReplaceRequest = { docToReplace ->
                viewingDocument = null
                selectedTypeCodeForCapture = docToReplace.typeCode
                showCaptureModal = true
            },
            onDocumentUpdated = {
                viewingDocument = null
            }
        )
    }
}

@Composable
private fun GridDocumentCard(
    doc: ClientDocumentEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("doc_grid_item_${doc.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = doc.thumbnailPath?.let { File(it) } ?: File(doc.filePath)
                if (thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = doc.typeName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(36.dp))
                }

                // Verification Overlay Chip
                Surface(
                    color = when (doc.verificationStatus) {
                        "VERIFICADO" -> Color(0xFF10B981)
                        "RECHAZADO" -> Color(0xFFEF4444)
                        else -> Color(0xFFF59E0B)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = doc.verificationStatus,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = doc.typeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = "${dateFormat.format(Date(doc.createdAtMs))} • v${doc.versionNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ListDocumentCard(
    doc: ClientDocumentEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("doc_list_item_${doc.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = doc.thumbnailPath?.let { File(it) } ?: File(doc.filePath)
                if (thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = doc.typeName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(doc.typeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Cargado por ${doc.capturedByUsername} • ${dateFormat.format(Date(doc.createdAtMs))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("v${doc.versionNumber} | ${doc.fileSizeByte / 1024} KB | Hash: ${doc.fileChecksumSha256.take(8)}...", fontSize = 10.sp, color = Color.Gray)
            }

            Surface(
                color = when (doc.verificationStatus) {
                    "VERIFICADO" -> Color(0xFF10B981)
                    "RECHAZADO" -> Color(0xFFEF4444)
                    else -> Color(0xFFF59E0B)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = doc.verificationStatus,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
