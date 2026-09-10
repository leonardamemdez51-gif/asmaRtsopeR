package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CollectionVisitEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionVisitsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }

    var selectedClientId by remember { mutableStateOf(clients.firstOrNull()?.id ?: 0L) }
    var resultType by remember { mutableStateOf("PAGO_REALIZADO") }
    var promisedAmountInput by remember { mutableStateOf("") }
    var promisedDateInput by remember { mutableStateOf("2026-08-06") }
    var notesInput by remember { mutableStateOf("") }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            RamaTopBar(title = "Gestión de Cobranza en Campo", onBackClick = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_record_visit")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Visita")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (visits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay visitas de cobranza registradas.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(visits) { visit ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(visit.clientName, fontWeight = FontWeight.Bold)
                                    Surface(
                                        color = when (visit.result) {
                                            "PAGO_REALIZADO" -> MaterialTheme.colorScheme.primaryContainer
                                            "PROMESA_PAGO" -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.errorContainer
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = visit.result.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Fecha: ${dateFormat.format(Date(visit.visitDate))}", style = MaterialTheme.typography.bodySmall)
                                Text("Observaciones: ${visit.notes}", style = MaterialTheme.typography.bodySmall)

                                if (visit.result == "PROMESA_PAGO") {
                                    Text(
                                        "Promesa de pago: \$${visit.promisedAmount} el ${visit.promisedPaymentDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GPS: ${visit.latitude}, ${visit.longitude}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val selectedClient = clients.find { it.id == selectedClientId } ?: clients.firstOrNull()

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Registrar Visita de Cobranza") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = clientDropdownExpanded,
                        onExpandedChange = { clientDropdownExpanded = !clientDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedClient?.fullName ?: "Seleccionar Cliente",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cliente") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = clientDropdownExpanded,
                            onDismissRequest = { clientDropdownExpanded = false }
                        ) {
                            clients.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.fullName) },
                                    onClick = {
                                        selectedClientId = c.id
                                        clientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Resultado de la Visita", fontWeight = FontWeight.Bold)
                    Column {
                        listOf("PAGO_REALIZADO", "PROMESA_PAGO", "NO_ENCONTRADO", "RECHAZO").forEach { res ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = resultType == res,
                                    onClick = { resultType = res }
                                )
                                Text(res.replace("_", " "))
                            }
                        }
                    }

                    if (resultType == "PROMESA_PAGO") {
                        OutlinedTextField(
                            value = promisedAmountInput,
                            onValueChange = { promisedAmountInput = it },
                            label = { Text("Monto Prometido (MXN)") }
                        )
                        OutlinedTextField(
                            value = promisedDateInput,
                            onValueChange = { promisedDateInput = it },
                            label = { Text("Fecha Promesa (YYYY-MM-DD)") }
                        )
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Observaciones / Comentarios") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val client = selectedClient ?: return@Button
                        val visit = CollectionVisitEntity(
                            clientId = client.id,
                            clientName = client.fullName,
                            loanId = 1L,
                            collectorId = viewModel.userSession.value.user?.id ?: 1L,
                            collectorName = viewModel.userSession.value.user?.fullName ?: "Cobrador",
                            latitude = client.latitude,
                            longitude = client.longitude,
                            result = resultType,
                            promisedAmount = promisedAmountInput.toDoubleOrNull(),
                            notes = notesInput
                        )
                        viewModel.recordVisit(visit, viewModel.userSession.value.user?.username ?: "cobrador")
                        showDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_visit")
                ) {
                    Text("Guardar Visita")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
