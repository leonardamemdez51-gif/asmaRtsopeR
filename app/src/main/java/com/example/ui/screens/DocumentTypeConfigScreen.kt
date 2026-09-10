package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentTypeConfigEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentTypeConfigScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTypes by viewModel.docRepo.allDocumentTypes.collectAsState(initial = emptyList())

    var showNewTypeDialog by remember { mutableStateOf(false) }
    var newTypeCode by remember { mutableStateOf("") }
    var newTypeName by remember { mutableStateOf("") }
    var newTypeDesc by remember { mutableStateOf("") }
    var newReqClient by remember { mutableStateOf(false) }
    var newReqLoan by remember { mutableStateOf(false) }
    var newReqRenewal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            RamaTopBar(
                title = "Configuración de Documentos",
                subtitle = "Gestión de tipos y obligatoriedad de expedientes",
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTypeDialog = true },
                modifier = Modifier.testTag("fab_add_doc_type")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Tipo de Documento")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Documentación Obligatoria & Políticas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Configura qué documentos se solicitan al cliente, en préstamos nuevos o renovaciones.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            items(allTypes) { docType ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(docType.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Código: ${docType.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Switch(
                                checked = docType.active,
                                onCheckedChange = { active ->
                                    coroutineScope.launch {
                                        viewModel.docRepo.saveDocumentTypeConfig(docType.copy(active = active))
                                    }
                                }
                            )
                        }

                        if (docType.description.isNotBlank()) {
                            Text(docType.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        HorizontalDivider()

                        // Checkbox Toggles for Mandatory Rules
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.isRequiredForClient,
                                    onCheckedChange = { isReq ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(isRequiredForClient = isReq))
                                        }
                                    }
                                )
                                Text("Obligatorio en registro de Cliente", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.isRequiredForLoan,
                                    onCheckedChange = { isReq ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(isRequiredForLoan = isReq))
                                        }
                                    }
                                )
                                Text("Obligatorio antes de Nuevo Préstamo", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.isRequiredForRenewal,
                                    onCheckedChange = { isReq ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(isRequiredForRenewal = isReq))
                                        }
                                    }
                                )
                                Text("Obligatorio antes de Renovación", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.isRequiredForCollection,
                                    onCheckedChange = { isReq ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(isRequiredForCollection = isReq))
                                        }
                                    }
                                )
                                Text("Obligatorio en Cobranza (Evidencia)", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.requiresValidation,
                                    onCheckedChange = { reqVal ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(requiresValidation = reqVal))
                                        }
                                    }
                                )
                                Text("Requiere Validación / Aprobación", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.allowsPhoto,
                                    onCheckedChange = { allow ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(allowsPhoto = allow))
                                        }
                                    }
                                )
                                Text("Permite Captura de Cámara", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.allowsFile,
                                    onCheckedChange = { allow ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(allowsFile = allow))
                                        }
                                    }
                                )
                                Text("Permite Selección de Archivo", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.hasValidityLimit,
                                    onCheckedChange = { hasLimit ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(hasValidityLimit = hasLimit))
                                        }
                                    }
                                )
                                Text("Tiene Límite de Vigencia", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = docType.blocksOperationsIfMissing,
                                    onCheckedChange = { blocks ->
                                        coroutineScope.launch {
                                            viewModel.docRepo.saveDocumentTypeConfig(docType.copy(blocksOperationsIfMissing = blocks))
                                        }
                                    }
                                )
                                Text("Bloquea Operaciones si Falta", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewTypeDialog) {
        AlertDialog(
            onDismissRequest = { showNewTypeDialog = false },
            title = { Text("Crear Nuevo Tipo de Documento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTypeName,
                        onValueChange = {
                            newTypeName = it
                            if (newTypeCode.isBlank()) {
                                newTypeCode = it.uppercase().replace(" ", "_")
                            }
                        },
                        label = { Text("Nombre del Documento (ej. Acta de Nacimiento)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTypeCode,
                        onValueChange = { newTypeCode = it.uppercase().replace(" ", "_") },
                        label = { Text("Código Único (ej. ACTA_NACIMIENTO)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTypeDesc,
                        onValueChange = { newTypeDesc = it },
                        label = { Text("Descripción o instrucciones") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newReqClient, onCheckedChange = { newReqClient = it })
                        Text("Requerido para Cliente", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newReqLoan, onCheckedChange = { newReqLoan = it })
                        Text("Requerido para Préstamo", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newReqRenewal, onCheckedChange = { newReqRenewal = it })
                        Text("Requerido para Renovación", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTypeCode.isNotBlank() && newTypeName.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.docRepo.saveDocumentTypeConfig(
                                    DocumentTypeConfigEntity(
                                        code = newTypeCode,
                                        name = newTypeName,
                                        description = newTypeDesc,
                                        isRequiredForClient = newReqClient,
                                        isRequiredForLoan = newReqLoan,
                                        isRequiredForRenewal = newReqRenewal,
                                        isRequiredForCollection = false,
                                        isSystemDefault = false,
                                        sortOrder = allTypes.size + 1,
                                        requiresValidation = false,
                                        allowsPhoto = true,
                                        allowsFile = true,
                                        hasValidityLimit = false,
                                        blocksOperationsIfMissing = false
                                    )
                                )
                                Toast.makeText(context, "Tipo de documento creado", Toast.LENGTH_SHORT).show()
                                showNewTypeDialog = false
                                newTypeCode = ""
                                newTypeName = ""
                                newTypeDesc = ""
                            }
                        }
                    },
                    enabled = newTypeCode.isNotBlank() && newTypeName.isNotBlank()
                ) {
                    Text("Crear Documento")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTypeDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
