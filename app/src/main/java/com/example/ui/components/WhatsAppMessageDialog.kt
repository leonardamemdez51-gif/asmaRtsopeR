package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.MessageTemplateEntity
import com.example.data.service.WhatsAppIntegrationService
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppMessageDialog(
    viewModel: MainViewModel,
    clientId: Long,
    clientName: String,
    clientPhone: String,
    monto: String = "$0.00",
    cuota: String = "$0.00",
    fechaPago: String = "Hoy",
    diasAtraso: Int = 0,
    saldo: String = "$0.00",
    numeroPrestamo: String = "PRST-0000",
    defaultTemplateCode: String = "PAGO_PROXIMO",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val templates by viewModel.allMessageTemplates.collectAsState()
    val sysConfig by viewModel.systemConfig.collectAsState()
    val currentUserSession by viewModel.userSession.collectAsState()
    val currentUsername = currentUserSession.user?.fullName ?: "Cobrador RAMA"

    // 1. Permissions Check
    val canSend = viewModel.hasPermission(com.example.core.security.AppPermission.WHATSAPP_SEND)
    if (!canSend) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Acceso Restringido")
                }
            },
            text = { Text("No cuenta con el permiso requerido (WHATSAPP_ENVIAR) para enviar mensajes por WhatsApp.") },
            confirmButton = {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Entendido")
                }
            }
        )
        return
    }

    // 2. Client Phone Validation
    val cleanPhone = clientPhone.replace("[^0-9]".toRegex(), "")
    val expectedLength = if (sysConfig?.whatsappDefaultCountryCode == "52") 10 else 8
    val isPhoneValid = cleanPhone.length >= expectedLength && clientPhone.isNotBlank()

    // 3. Filter templates according to specific permissions
    val filteredTemplates = remember(templates) {
        templates.filter { template ->
            when (template.code) {
                "PAGO_PROXIMO", "PAGO_HOY" -> viewModel.hasPermission(com.example.core.security.AppPermission.WHATSAPP_REMINDER)
                "CONFIRMACION_PAGO", "PAGO_PARCIAL" -> viewModel.hasPermission(com.example.core.security.AppPermission.WHATSAPP_PAYMENT)
                "MORA", "PAGO_VENCIDO" -> viewModel.hasPermission(com.example.core.security.AppPermission.WHATSAPP_LATE_FEE)
                "PROMESA_PAGO" -> viewModel.hasPermission(com.example.core.security.AppPermission.WHATSAPP_PROMISE)
                "RENOVACION_DISPONIBLE" -> viewModel.hasPermission(com.example.core.security.AppPermission.WHATSAPP_RENEWAL)
                else -> true
            }
        }
    }

    var selectedTemplateCode by remember { 
        mutableStateOf(
            if (filteredTemplates.any { it.code == defaultTemplateCode }) defaultTemplateCode 
            else filteredTemplates.firstOrNull()?.code ?: "MANUAL"
        ) 
    }
    var customNotes by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val activeTemplate = filteredTemplates.find { it.code == selectedTemplateCode }
        ?: MessageTemplateEntity(
            code = "MANUAL",
            name = "Mensaje Personalizado",
            templateText = "Hola {nombre}, le contactamos de RAMA Microfinanzas referente a su préstamo #{folio}."
        )

    val signature = sysConfig?.whatsappMessageSignature ?: "Atentamente,\nRAMA Microfinanzas."
    val companyName = sysConfig?.companyName ?: "RAMA Microfinanzas"
    val companyPhone = sysConfig?.phone ?: "55-8000-7262"

    val variableMap = remember(clientName, monto, cuota, fechaPago, diasAtraso, saldo, numeroPrestamo, currentUsername, signature, companyName, companyPhone) {
        WhatsAppIntegrationService.buildVariableMap(
            clientName = clientName,
            monto = monto,
            cuota = cuota,
            fechaPago = fechaPago,
            diasAtraso = diasAtraso,
            saldo = saldo,
            numeroPrestamo = numeroPrestamo,
            nombreCobrador = currentUsername,
            fechaVencimiento = "N/A",
            mora = "$0.00",
            totalPendiente = cuota,
            nombreEmpresa = companyName,
            telefonoEmpresa = companyPhone
        )
    }

    val previewText = remember(activeTemplate.templateText, variableMap, signature) {
        WhatsAppIntegrationService.formatTemplateMessage(activeTemplate.templateText, variableMap, signature)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "WhatsApp",
                    tint = Color(0xFF25D366)
                )
                Text(
                    text = "Enviar por WhatsApp",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Warning if global WhatsApp alerts are disabled
                if (sysConfig?.enableWhatsAppAlerts == false) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                "Advertencia: El Administrador desactivó la integración de WhatsApp globalmente. El envío podría no procesarse.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Client Info Header
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = clientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = clientPhone.ifBlank { "Sin teléfono registrado" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Phone Validation Error
                if (!isPhoneValid) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("phone_validation_error_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "El cliente no tiene un número de WhatsApp válido registrado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Template Selector
                if (filteredTemplates.isNotEmpty()) {
                    Text(
                        text = "Seleccionar Plantilla:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredTemplates.forEach { template ->
                            FilterChip(
                                selected = template.code == selectedTemplateCode,
                                onClick = { selectedTemplateCode = template.code },
                                label = { Text(template.name) },
                                leadingIcon = if (template.code == selectedTemplateCode) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.fillMaxWidth().testTag("chip_tpl_${template.code}")
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No tiene permisos para usar ninguna plantilla de WhatsApp preestablecida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Divider()

                // Live Preview
                Text(
                    text = "Vista Previa del Mensaje:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFDCF8C6).copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF25D366).copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }

                OutlinedTextField(
                    value = customNotes,
                    onValueChange = { customNotes = it },
                    label = { Text("Notas internas de auditoría (Opcional)") },
                    placeholder = { Text("Ej: Cliente promete pagar a las 4 PM") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_notes_input"),
                    singleLine = true
                )

                // Audit Notice
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3CD)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Aviso de Transparencia: El envío se abrirá en WhatsApp para validación y confirmación. Todo intento queda registrado en la bitácora histórica.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF856404)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSending = true
                    viewModel.sendWhatsAppMessageWithLog(
                        context = context,
                        clientId = clientId,
                        clientName = clientName,
                        clientPhone = clientPhone,
                        templateCode = selectedTemplateCode,
                        messageContent = previewText,
                        notes = customNotes
                    ) {
                        isSending = false
                        onDismiss()
                    }
                },
                enabled = !isSending && isPhoneValid && filteredTemplates.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_open_whatsapp_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSending) "Cargando..." else "Abrir en WhatsApp")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
