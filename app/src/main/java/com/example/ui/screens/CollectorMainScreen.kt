package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorMainScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.collectorScreenState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    // Dialog state for management registration ("Registrar Gestión")
    var showManagementDialog by remember { mutableStateOf(false) }
    var selectedTaskForManagement by remember { mutableStateOf<CollectorTaskItem?>(null) }
    var managementResultInput by remember { mutableStateOf("PROMESA_PAGO") }
    var managementNotesInput by remember { mutableStateOf("") }
    var managementPromisedAmountInput by remember { mutableStateOf("") }
    var managementPromisedDaysOffset by remember { mutableStateOf(1) } // 1 = Mañana, 3 = 3 días, 7 = 1 semana

    Scaffold(
        topBar = {
            RamaTopBar(
                title = "Pantalla Principal del Cobrador",
                subtitle = "Tareas del Día • Modo Offline Activo",
                onBackClick = onBack,
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline OK",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Header Metrics Banner (Resumen en tiempo real)
            CollectorHeaderSummaryCard(
                summary = state.summary,
                currencyFormat = currencyFormat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // 2. Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateCollectorSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("collector_search_input"),
                placeholder = { Text("Buscar cliente, dirección, teléfono o préstamo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (state.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateCollectorSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Scrollable Category Classification Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CollectorTaskCategory.values()) { cat ->
                    val isSelected = state.selectedCategory == cat
                    val count = when (cat) {
                        CollectorTaskCategory.TODAS -> state.summary.totalTasksCount
                        CollectorTaskCategory.EN_MORA -> state.summary.overdueTasksCount
                        else -> state.tasks.count { it.category == cat }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateCollectorCategory(cat) },
                        label = {
                            Text(
                                text = "${cat.displayName} ($count)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("collector_category_${cat.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Main Tasks List
            if (state.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron tareas en esta categoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Intenta cambiar el filtro o realizar una búsqueda distinta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        CollectorTaskCardItem(
                            task = task,
                            currencyFormat = currencyFormat,
                            canRegisterPayment = state.canRegisterPayment,
                            onOpenMaps = { launchGoogleMaps(context, task.client.address) },
                            onOpenWhatsApp = { launchWhatsApp(context, task.client.phone, task.client.fullName, task.amountToCollect) },
                            onCallPhone = { launchPhoneCall(context, task.client.phone) },
                            onRegisterPayment = { onNavigate("process_payment/${task.loan.id}") },
                            onRegisterManagement = {
                                selectedTaskForManagement = task
                                managementResultInput = "PROMESA_PAGO"
                                managementNotesInput = ""
                                managementPromisedAmountInput = task.amountToCollect.toString()
                                showManagementDialog = true
                            },
                            onViewFile = { onNavigate("client_detail/${task.client.id}") }
                        )
                    }
                }
            }
        }
    }

    // 5. Registration Management Modal ("Registrar Gestión")
    if (showManagementDialog && selectedTaskForManagement != null) {
        val targetTask = selectedTaskForManagement!!
        AlertDialog(
            onDismissRequest = { showManagementDialog = false },
            icon = { Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = "Registrar Gestión de Visita",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cliente: ${targetTask.client.fullName}\nPréstamo #${targetTask.loan.id} • Monto Cobrar: ${currencyFormat.format(targetTask.amountToCollect)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Resultado de la Visita:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    listOf(
                        "PROMESA_PAGO" to "Promesa de Pago",
                        "PAGO_PARCIAL" to "Abonó Parcial",
                        "NO_ENCONTRADO" to "No se encontró en domicilio",
                        "RECHAZO" to "Se niega a pagar / Conflicto",
                        "CAMBIO_FECHA" to "Solicita cambio de fecha"
                    ).forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { managementResultInput = code }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = managementResultInput == code,
                                onClick = { managementResultInput = code }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (managementResultInput == "PROMESA_PAGO" || managementResultInput == "CAMBIO_FECHA") {
                        Text(
                            text = "Plazo de Promesa:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                1 to "Mañana",
                                3 to "3 Días",
                                7 to "1 Semana"
                            ).forEach { (days, label) ->
                                FilterChip(
                                    selected = managementPromisedDaysOffset == days,
                                    onClick = { managementPromisedDaysOffset = days },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = managementPromisedAmountInput,
                            onValueChange = { managementPromisedAmountInput = it },
                            label = { Text("Monto Prometido (MXN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = managementNotesInput,
                        onValueChange = { managementNotesInput = it },
                        label = { Text("Observaciones / Notas del Cobrador") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val offsetMs = managementPromisedDaysOffset * 24L * 60 * 60 * 1000
                        val promisedDate = System.currentTimeMillis() + offsetMs
                        val promisedAmt = managementPromisedAmountInput.toDoubleOrNull() ?: targetTask.amountToCollect

                        viewModel.recordCollectorManagement(
                            clientId = targetTask.client.id,
                            loanId = targetTask.loan.id,
                            result = managementResultInput,
                            promisedDate = if (managementResultInput in listOf("PROMESA_PAGO", "CAMBIO_FECHA")) promisedDate else null,
                            promisedAmount = if (managementResultInput in listOf("PROMESA_PAGO", "CAMBIO_FECHA")) promisedAmt else null,
                            notes = managementNotesInput
                        )
                        showManagementDialog = false
                    },
                    modifier = Modifier.testTag("save_management_btn")
                ) {
                    Text("Guardar Gestión")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showManagementDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CollectorHeaderSummaryCard(
    summary: CollectorDailySummary,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("collector_header_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Meta de Cobro del Día",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${summary.completedTasksCount} de ${summary.totalTasksCount} tareas cobradas hoy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${summary.compliancePercentage.toInt()}% Cumplido",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (summary.compliancePercentage / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Esperado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(
                        currencyFormat.format(summary.totalExpectedAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text("Total Cobrado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(
                        currencyFormat.format(summary.totalCollectedAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    )
                }

                Column {
                    Text("Pendiente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(
                        currencyFormat.format(summary.totalPendingAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectorTaskCardItem(
    task: CollectorTaskItem,
    currencyFormat: NumberFormat,
    canRegisterPayment: Boolean,
    onOpenMaps: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onCallPhone: () -> Unit,
    onRegisterPayment: () -> Unit,
    onRegisterManagement: () -> Unit,
    onViewFile: () -> Unit
) {
    val priorityBgColor = when (task.priority) {
        TaskPriority.CRITICO -> Color(0xFFFEE2E2)
        TaskPriority.ATRASADO -> Color(0xFFFFEDD5)
        TaskPriority.PAGO_HOY -> Color(0xFFE0F2FE)
        TaskPriority.PROMESA -> Color(0xFFFEF3C7)
        TaskPriority.PENDIENTE -> Color(0xFFF3F4F6)
        TaskPriority.AL_CORRIENTE -> Color(0xFFD1FAE5)
    }

    val priorityTextColor = when (task.priority) {
        TaskPriority.CRITICO -> Color(0xFF991B1B)
        TaskPriority.ATRASADO -> Color(0xFFC2410C)
        TaskPriority.PAGO_HOY -> Color(0xFF0369A1)
        TaskPriority.PROMESA -> Color(0xFFB45309)
        TaskPriority.PENDIENTE -> Color(0xFF374151)
        TaskPriority.AL_CORRIENTE -> Color(0xFF065F46)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("collector_task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Name & Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = task.client.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Préstamo #${task.loan.id} • ${task.remainingInstallmentsCount} de ${task.totalInstallments} cuotas restantes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = priorityBgColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = task.priority.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = priorityTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailTextItem(icon = Icons.Outlined.LocationOn, label = "Dirección", value = task.client.address)
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailTextItem(icon = Icons.Outlined.Phone, label = "Teléfono", value = task.client.phone)
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailTextItem(icon = Icons.Outlined.Payment, label = "Pago Preferido", value = task.preferredPaymentMethod)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    DetailTextItem(icon = Icons.Outlined.EventRepeat, label = "Última Visita", value = task.lastVisitDateFormatted)
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailTextItem(icon = Icons.Outlined.Star, label = "Score Puntualidad", value = "${task.punctualityScore} / 100 pts")
                    Spacer(modifier = Modifier.height(6.dp))
                    if (task.promisedPaymentDateFormatted != null) {
                        DetailTextItem(icon = Icons.Outlined.Alarm, label = "Promesa Pago", value = task.promisedPaymentDateFormatted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amounts Row Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("A Cobrar Hoy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            currencyFormat.format(task.amountToCollect),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (task.overdueAmount > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Monto Vencido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                currencyFormat.format(task.overdueAmount),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Saldo Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            currencyFormat.format(task.totalRemainingBalance),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row (6 required buttons: Maps, WhatsApp, Call, Registrar Pago, Registrar Gestión, Ver Expediente)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Google Maps Button
                IconButton(
                    onClick = onOpenMaps,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.12f))
                        .testTag("btn_maps_${task.id}")
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Google Maps", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                }

                // 2. WhatsApp Button
                IconButton(
                    onClick = onOpenWhatsApp,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A).copy(alpha = 0.12f))
                        .testTag("btn_whatsapp_${task.id}")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                }

                // 3. Call Phone Button
                IconButton(
                    onClick = onCallPhone,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4F46E5).copy(alpha = 0.12f))
                        .testTag("btn_call_${task.id}")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // 4. Registrar Gestión Button
                OutlinedButton(
                    onClick = onRegisterManagement,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_management_${task.id}")
                ) {
                    Icon(Icons.Default.AssignmentLate, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gestión", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 5. Registrar Pago Button
                Button(
                    onClick = onRegisterPayment,
                    enabled = canRegisterPayment,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.testTag("btn_pay_${task.id}")
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cobrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 6. Ver Expediente Button
                IconButton(
                    onClick = onViewFile,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("btn_expediente_${task.id}")
                ) {
                    Icon(Icons.Default.FolderShared, contentDescription = "Ver Expediente", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailTextItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun launchGoogleMaps(context: Context, address: String) {
    val encodedAddress = Uri.encode(address)
    val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedAddress")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$encodedAddress"))
        context.startActivity(browserIntent)
    }
}

private fun launchWhatsApp(context: Context, phone: String, clientName: String, amount: Double) {
    val cleanPhone = phone.replace(Regex("[^0-9]"), "")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    val message = "Hola $clientName, le saludamos de RAMA Microfinanzas para recordarle su pago de hoy por la cantidad de ${currencyFormat.format(amount)}. ¡Gracias por su puntualidad!"
    val encodedMsg = Uri.encode(message)
    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp en este dispositivo", Toast.LENGTH_SHORT).show()
    }
}

private fun launchPhoneCall(context: Context, phone: String) {
    val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo realizar la llamada", Toast.LENGTH_SHORT).show()
    }
}
