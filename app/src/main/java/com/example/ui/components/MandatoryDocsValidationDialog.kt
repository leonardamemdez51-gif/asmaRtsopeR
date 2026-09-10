package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentTypeConfigEntity
import com.example.ui.MainViewModel

@Composable
fun MandatoryDocsValidationDialog(
    clientId: Long,
    clientName: String,
    missingDocs: List<DocumentTypeConfigEntity>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onCapturedSuccess: () -> Unit
) {
    var showCaptureForType by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Documentación Obligatoria Incompleta", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Antes de procesar la solicitud de préstamo para $clientName, el sistema requiere capturar los siguientes documentos obligatorios:",
                    style = MaterialTheme.typography.bodyMedium
                )

                missingDocs.forEach { missingType ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(missingType.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB45309))
                                if (missingType.description.isNotBlank()) {
                                    Text(missingType.description, fontSize = 11.sp, color = Color(0xFF78350F))
                                }
                            }

                            Button(
                                onClick = { showCaptureForType = missingType.code },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Capturar", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido / Volver")
            }
        }
    )

    val typeToCapture = showCaptureForType
    if (typeToCapture != null) {
        DocumentCaptureModal(
            clientId = clientId,
            clientName = clientName,
            preselectedTypeCode = typeToCapture,
            viewModel = viewModel,
            onDismiss = { showCaptureForType = null },
            onDocumentCaptured = {
                showCaptureForType = null
                onCapturedSuccess()
            }
        )
    }
}
