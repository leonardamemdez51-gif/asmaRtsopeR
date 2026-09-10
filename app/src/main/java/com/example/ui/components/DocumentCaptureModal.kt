package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.ClientDocumentEntity
import com.example.data.local.DocumentTypeConfigEntity
import com.example.ui.MainViewModel
import com.example.util.DocumentStorageManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentCaptureModal(
    clientId: Long,
    clientName: String,
    preselectedTypeCode: String? = null,
    loanId: Long? = null,
    visitId: Long? = null,
    paymentId: Long? = null,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onDocumentCaptured: (ClientDocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val docTypes by viewModel.docRepo.activeDocumentTypes.collectAsState(initial = emptyList())

    var selectedTypeCode by remember { mutableStateOf(preselectedTypeCode ?: docTypes.firstOrNull()?.code ?: "INE_FRENTE") }
    var notesInput by remember { mutableStateOf("") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var issueDateInput by remember { mutableStateOf<Long?>(null) }
    var expiryDateInput by remember { mutableStateOf<Long?>(null) }
    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val currentSession by viewModel.userSession.collectAsState()
    val userRole = currentSession.user?.role ?: "ADMINISTRADOR"
    val username = currentSession.user?.username ?: "admin"
    val userId = currentSession.user?.id ?: 1L

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            capturedImageUri = tempCameraUri
        } else {
            Toast.makeText(context, "Captura de cámara cancelada o fallida", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission Launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val cameraPair = DocumentStorageManager.getTempImageUriForCamera(context)
            if (cameraPair != null) {
                tempCameraUri = cameraPair.first
                tempCameraFile = cameraPair.second
                cameraLauncher.launch(cameraPair.first)
            } else {
                Toast.makeText(context, "Error al preparar cámara local", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            capturedImageUri = uri
        }
    }

    fun launchCamera() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val cameraPair = DocumentStorageManager.getTempImageUriForCamera(context)
            if (cameraPair != null) {
                tempCameraUri = cameraPair.first
                tempCameraFile = cameraPair.second
                cameraLauncher.launch(cameraPair.first)
            } else {
                Toast.makeText(context, "Error al preparar cámara local", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Capturar / Cargar Documento",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cliente: $clientName (#$clientId)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. SELECT DOCUMENT TYPE
                    Text("1. Selecciona el Tipo de Documento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(docTypes) { type ->
                            FilterChip(
                                selected = selectedTypeCode == type.code,
                                onClick = { selectedTypeCode = type.code },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(type.name)
                                        if (type.isRequiredForClient || type.isRequiredForLoan) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("*", color = Color.Red, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                leadingIcon = {
                                    if (selectedTypeCode == type.code) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }

                    // Selected type detail
                    val selectedTypeObj = docTypes.find { it.code == selectedTypeCode }
                    if (selectedTypeObj != null && selectedTypeObj.description.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "ℹ️ ${selectedTypeObj.description}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // 2. CAPTURE METHOD (Camera or Gallery)
                    val allowsPhoto = selectedTypeObj?.allowsPhoto ?: true
                    val allowsFile = selectedTypeObj?.allowsFile ?: true

                    if (allowsPhoto || allowsFile) {
                        Text("2. Origen del Documento / Evidencia", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (allowsPhoto) {
                                Button(
                                    onClick = { launchCamera() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("btn_capture_camera"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tomar Foto")
                                }
                            }

                            if (allowsFile) {
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("btn_select_gallery"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Desde Galería")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "⚠️ Este tipo de documento no permite cargas ni fotos por configuración.",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // 3. IMAGE PREVIEW
                    if (capturedImageUri != null) {
                        Text("3. Vista Previa Capturada", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = capturedImageUri,
                                    contentDescription = "Vista previa",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ninguna imagen seleccionada", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // VALIDITY DATES (Condicional si hasValidityLimit = true)
                    if (selectedTypeObj?.hasValidityLimit == true) {
                        Text("Vigencia del Documento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showIssueDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                val dateStr = issueDateInput?.let { formatter.format(Date(it)) } ?: "Seleccionar"
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Fecha de Emisión", fontSize = 11.sp, color = Color.Gray)
                                    Text(dateStr, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { showExpiryDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                val dateStr = expiryDateInput?.let { formatter.format(Date(it)) } ?: "Seleccionar"
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Fecha de Vencimiento", fontSize = 11.sp, color = Color.Gray)
                                    Text(dateStr, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Date Pickers dialogs
                        if (showIssueDatePicker) {
                            val calendar = Calendar.getInstance()
                            issueDateInput?.let { calendar.timeInMillis = it }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    issueDateInput = cal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                            showIssueDatePicker = false
                        }

                        if (showExpiryDatePicker) {
                            val calendar = Calendar.getInstance()
                            expiryDateInput?.let { calendar.timeInMillis = it }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    expiryDateInput = cal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                            showExpiryDatePicker = false
                        }
                    }

                    // 4. OPTIONAL NOTES
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notas o Comentarios Adicionales") },
                        placeholder = { Text("Ej. INE legible, comprobante de luz del mes actual...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SAVE BUTTON
                Button(
                    onClick = {
                        val uri = capturedImageUri
                        if (uri == null) {
                            Toast.makeText(context, "Por favor toma una foto o selecciona una imagen de la galería", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isProcessing = true
                        coroutineScope.launch {
                            val result = viewModel.docRepo.uploadOrCaptureDocument(
                                context = context,
                                sourceUri = uri,
                                typeCode = selectedTypeCode,
                                clientId = clientId,
                                clientName = clientName,
                                loanId = loanId,
                                visitId = visitId,
                                paymentId = paymentId,
                                capturedByUserId = userId,
                                capturedByUsername = username,
                                userRole = userRole,
                                notes = notesInput,
                                issueDateMs = issueDateInput,
                                expiryDateMs = expiryDateInput
                            )

                            isProcessing = false
                            result.onSuccess { doc ->
                                Toast.makeText(context, "Documento guardado y comprimido exitosamente", Toast.LENGTH_SHORT).show()
                                onDocumentCaptured(doc)
                                onDismiss()
                            }.onFailure { err ->
                                Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = capturedImageUri != null && !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_captured_doc"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Procesando & Comprimiendo...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Documento en Expediente Seguro", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
