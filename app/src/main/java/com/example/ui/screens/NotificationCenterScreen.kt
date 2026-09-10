package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val context = LocalContext.current
    val notifications by viewModel.allNotifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val templates by viewModel.allMessageTemplates.collectAsState()
    val reminderCfg by viewModel.reminderConfig.collectAsState()
    val commLogs by viewModel.allCommunicationLogs.collectAsState()
    val userPrefs by viewModel.userNotificationPreferences.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Notificaciones", "Plantillas WhatsApp", "Programación", "Historial Envíos", "Preferencias Push")

    // Filter state for notifications
    var notifFilterType by remember { mutableStateOf("TODAS") } // "TODAS", "NO_LEIDAS", "URGENTES", "PAGOS", "CAJA"

    // Edit Template State
    var editingTemplate by remember { mutableStateOf<MessageTemplateEntity?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Centro de Comunicaciones", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text("$unreadCount", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.evaluateAndGeneratePaymentReminders() },
                        modifier = Modifier.testTag("evaluate_reminders_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Evaluar Vencimientos")
                    }
                    IconButton(
                        onClick = { viewModel.markAllNotificationsAsRead() },
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Marcar Todo Leído")
                    }
                    IconButton(
                        onClick = { viewModel.clearAllNotifications() },
                        modifier = Modifier.testTag("clear_notifications_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Vaciar Notificaciones")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(title)
                                if (index == 0 && unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> NotificationsTabContent(
                    notifications = notifications,
                    filterType = notifFilterType,
                    onFilterChange = { notifFilterType = it },
                    onItemClick = { notif ->
                        viewModel.markNotificationAsRead(notif.id)
                        notif.navigationRoute?.let { route ->
                            if (route.isNotBlank()) {
                                onNavigateToRoute(route)
                            }
                        }
                    },
                    onDelete = { viewModel.deleteNotification(it.id) },
                    dateFormat = dateFormat
                )
                1 -> TemplatesTabContent(
                    templates = templates,
                    onEditTemplate = { tmpl ->
                        editingTemplate = tmpl
                        showTemplateDialog = true
                    },
                    onAddTemplate = {
                        editingTemplate = MessageTemplateEntity(
                            code = "NUEVA_PLANTILLA_${System.currentTimeMillis() % 1000}",
                            name = "Nueva Plantilla de Mensaje",
                            templateText = "Estimado(a) {nombre_cliente}, "
                        )
                        showTemplateDialog = true
                    }
                )
                2 -> ReminderScheduleTabContent(
                    config = reminderCfg ?: ReminderConfigEntity(),
                    onSaveConfig = { newCfg ->
                        viewModel.saveReminderConfig(newCfg)
                    }
                )
                3 -> CommunicationLogsTabContent(
                    logs = commLogs,
                    dateFormat = dateFormat
                )
                4 -> NotificationPreferencesTabContent(
                    pref = userPrefs ?: NotificationPreferenceEntity(userId = 1L),
                    onSavePref = { newPref ->
                        viewModel.saveNotificationPreferences(newPref)
                    },
                    onRegisterFcm = { token, model ->
                        viewModel.registerFcmToken(token, model)
                    }
                )
            }
        }
    }

    // Template Edit Dialog
    if (showTemplateDialog && editingTemplate != null) {
        TemplateEditDialog(
            template = editingTemplate!!,
            onDismiss = { showTemplateDialog = false },
            onSave = { updatedTmpl ->
                viewModel.saveMessageTemplate(updatedTmpl)
                showTemplateDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// TAB 0: NOTIFICACIONES INTERNAS
// -----------------------------------------------------------------------------------------
@Composable
private fun NotificationsTabContent(
    notifications: List<NotificationEntity>,
    filterType: String,
    onFilterChange: (String) -> Unit,
    onItemClick: (NotificationEntity) -> Unit,
    onDelete: (NotificationEntity) -> Unit,
    dateFormat: SimpleDateFormat
) {
    val filteredList = remember(notifications, filterType) {
        when (filterType) {
            "NO_LEIDAS" -> notifications.filter { !it.isRead }
            "URGENTES" -> notifications.filter { it.priority == "URGENTE" || it.priority == "ALTA" }
            "PAGOS" -> notifications.filter { it.type == "PAGO_PENDIENTE" || it.type == "PAGO_VENCIDO" }
            "CAJA" -> notifications.filter { it.type == "ALERTA_CAJA" }
            else -> notifications
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterType == "TODAS",
                onClick = { onFilterChange("TODAS") },
                label = { Text("Todas (${notifications.size})") }
            )
            FilterChip(
                selected = filterType == "NO_LEIDAS",
                onClick = { onFilterChange("NO_LEIDAS") },
                label = { Text("No leídas") }
            )
            FilterChip(
                selected = filterType == "URGENTES",
                onClick = { onFilterChange("URGENTES") },
                label = { Text("Urgentes") }
            )
            FilterChip(
                selected = filterType == "PAGOS",
                onClick = { onFilterChange("PAGOS") },
                label = { Text("Cobranza") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sin notificaciones en esta categoría",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { notif ->
                    NotificationItemCard(
                        notif = notif,
                        onClick = { onItemClick(notif) },
                        onDelete = { onDelete(notif) },
                        dateFormat = dateFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notif: NotificationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    dateFormat: SimpleDateFormat
) {
    val priorityColor = when (notif.priority) {
        "URGENTE" -> Color(0xFFD32F2F)
        "ALTA" -> Color(0xFFE65100)
        "MEDIA" -> Color(0xFF0288D1)
        else -> Color(0xFF757575)
    }

    val typeIcon = when (notif.type) {
        "PAGO_PENDIENTE" -> Icons.Default.Schedule
        "PAGO_VENCIDO" -> Icons.Default.Warning
        "ALERTA_CAJA" -> Icons.Default.AccountBalanceWallet
        "PROMESA_PROXIMA", "PROMESA_INCUMPLIDA" -> Icons.Default.Handshake
        else -> Icons.Default.Notifications
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notif.isRead) 1.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(priorityColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, contentDescription = null, tint = priorityColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notif.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = priorityColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = notif.priority,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notif.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(notif.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (!notif.navigationRoute.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ver detalle",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 1: PLANTILLAS DE MENSAJES (WHATSAPP)
// -----------------------------------------------------------------------------------------
@Composable
private fun TemplatesTabContent(
    templates: List<MessageTemplateEntity>,
    onEditTemplate: (MessageTemplateEntity) -> Unit,
    onAddTemplate: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Plantillas Configurables de WhatsApp (${templates.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddTemplate,
                modifier = Modifier.testTag("add_template_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva Plantilla")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Variables dinámicas soportadas en las plantillas:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "{nombre_cliente}, {monto}, {cuota}, {fecha_pago}, {dias_atraso}, {saldo}, {numero_prestamo}, {nombre_cobrador}, {telefono_empresa}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(templates, key = { it.id }) { tmpl ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = Color(0xFF25D366),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = tmpl.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = tmpl.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(onClick = { onEditTemplate(tmpl) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar Plantilla", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = tmpl.templateText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateEditDialog(
    template: MessageTemplateEntity,
    onDismiss: () -> Unit,
    onSave: (MessageTemplateEntity) -> Unit
) {
    var code by remember { mutableStateOf(template.code) }
    var name by remember { mutableStateOf(template.name) }
    var category by remember { mutableStateOf(template.category) }
    var templateText by remember { mutableStateOf(template.templateText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Plantilla de Mensaje") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Código Único") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Plantilla") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = templateText,
                    onValueChange = { templateText = it },
                    label = { Text("Texto de la Plantilla") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        template.copy(
                            code = code,
                            name = name,
                            category = category,
                            templateText = templateText,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// -----------------------------------------------------------------------------------------
// TAB 2: PROGRAMACIÓN DE RECORDATORIOS
// -----------------------------------------------------------------------------------------
@Composable
private fun ReminderScheduleTabContent(
    config: ReminderConfigEntity,
    onSaveConfig: (ReminderConfigEntity) -> Unit
) {
    var daysAdvance by remember(config) { mutableStateOf(config.daysAdvanceNotice.toString()) }
    var sendingTime by remember(config) { mutableStateOf(config.sendingTime) }
    var allowedDays by remember(config) { mutableStateOf(config.allowedDaysCsv) }
    var preventDupHours by remember(config) { mutableStateOf(config.preventDuplicatesWithinHours.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Parámetros de Programación Automática de Recordatorios",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = daysAdvance,
            onValueChange = { daysAdvance = it },
            label = { Text("Anticipación de Aviso (Días previos al vencimiento)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = sendingTime,
            onValueChange = { sendingTime = it },
            label = { Text("Hora Preferida de Envío (Formato HH:mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = allowedDays,
            onValueChange = { allowedDays = it },
            label = { Text("Días Permitidos de Envío (Separados por coma)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = preventDupHours,
            onValueChange = { preventDupHours = it },
            label = { Text("Ventana para evitar duplicados (Horas)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                val newConfig = config.copy(
                    daysAdvanceNotice = daysAdvance.toIntOrNull() ?: 1,
                    sendingTime = sendingTime,
                    allowedDaysCsv = allowedDays,
                    preventDuplicatesWithinHours = preventDupHours.toIntOrNull() ?: 24
                )
                onSaveConfig(newConfig)
            },
            modifier = Modifier.fillMaxWidth().testTag("save_reminder_config_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Guardar Configuración de Programación")
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 3: HISTORIAL & AUDITORÍA DE ENVÍOS
// -----------------------------------------------------------------------------------------
@Composable
private fun CommunicationLogsTabContent(
    logs: List<CommunicationLogEntity>,
    dateFormat: SimpleDateFormat
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs
        else logs.filter {
            it.clientName.contains(searchQuery, ignoreCase = true) ||
            it.clientPhone.contains(searchQuery) ||
            it.templateCodeUsed.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar por cliente, teléfono o plantilla...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay registros de comunicaciones enviadas", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.clientName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (log.resultStatus.contains("API")) Color(0xFF00897B) else Color(0xFF0288D1),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = log.resultStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Tel: ${log.clientPhone} | Plantilla: ${log.templateCodeUsed} | Usuario: ${log.userName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = log.messageContent,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = dateFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TAB 4: PREFERENCIAS PUSH & FCM
// -----------------------------------------------------------------------------------------
@Composable
private fun NotificationPreferencesTabContent(
    pref: NotificationPreferenceEntity,
    onSavePref: (NotificationPreferenceEntity) -> Unit,
    onRegisterFcm: (String, String) -> Unit
) {
    var pPayments by remember(pref) { mutableStateOf(pref.enablePaymentsAlerts) }
    var pOverdue by remember(pref) { mutableStateOf(pref.enableOverdueAlerts) }
    var pPromises by remember(pref) { mutableStateOf(pref.enablePromisesAlerts) }
    var pAuths by remember(pref) { mutableStateOf(pref.enableAuthorizationsAlerts) }
    var pCash by remember(pref) { mutableStateOf(pref.enableCashAlerts) }
    var pSync by remember(pref) { mutableStateOf(pref.enableSyncAlerts) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Preferencias de Notificaciones Push",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alertas de Pagos por Vencer")
            Switch(checked = pPayments, onCheckedChange = { pPayments = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alertas de Mora y Pagos Vencidos")
            Switch(checked = pOverdue, onCheckedChange = { pOverdue = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recordatorios de Promesas de Pago")
            Switch(checked = pPromises, onCheckedChange = { pPromises = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Solicitudes y Autorizaciones de Préstamos")
            Switch(checked = pAuths, onCheckedChange = { pAuths = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alertas Operativas de Caja y Arqueo")
            Switch(checked = pCash, onCheckedChange = { pCash = it })
        }

        Button(
            onClick = {
                val updated = pref.copy(
                    enablePaymentsAlerts = pPayments,
                    enableOverdueAlerts = pOverdue,
                    enablePromisesAlerts = pPromises,
                    enableAuthorizationsAlerts = pAuths,
                    enableCashAlerts = pCash,
                    enableSyncAlerts = pSync
                )
                onSavePref(updated)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Preferencias")
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Registro FCM (Firebase Cloud Messaging):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (pref.fcmToken.isNotBlank()) "Token FCM Activo: ${pref.fcmToken.take(25)}..." else "Dispositivo sin Token FCM registrado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        val mockToken = "fcm_token_device_${System.currentTimeMillis() % 10000}"
                        onRegisterFcm(mockToken, android.os.Build.MODEL)
                    }
                ) {
                    Text("Re-registrar Token FCM")
                }
            }
        }
    }
}
