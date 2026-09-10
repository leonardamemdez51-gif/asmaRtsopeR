package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.*
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf("HOJA_COBRANZA") }
    var selectedSubReport by remember { mutableStateOf("COBRANZA_DIARIA") }

    // Quick filter states
    var selectedDateRange by remember { mutableStateOf("Todo el tiempo") }
    var selectedCobrador by remember { mutableStateOf("TODOS") }
    var selectedRuta by remember { mutableStateOf("TODAS") }
    var selectedMetodo by remember { mutableStateOf("TODOS") }
    var searchQuery by remember { mutableStateOf("") }
    
    // Audit-specific states
    var selectedAuditActionFilter by remember { mutableStateOf("TODOS") }
    var selectedAuditResultFilter by remember { mutableStateOf("TODOS") }
    var selectedAuditTab by remember { mutableStateOf("BITACORA") }
    var selectedAuditForDetail by remember { mutableStateOf<AuditLogEntity?>(null) }

    // Collect global entities
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val clients by viewModel.allClientsList.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val registers by viewModel.allCashRegisters.collectAsStateWithLifecycle()
    val movements by viewModel.allCashMovements.collectAsStateWithLifecycle()
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val installments by viewModel.allInstallments.collectAsStateWithLifecycle(initialValue = emptyList())
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val currentUserRole = userSession.user?.role ?: "CONSULTA"

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    // Auto-align sub-reports on tab change to match UI tags
    LaunchedEffect(selectedReport) {
        selectedSubReport = when (selectedReport) {
            "HOJA_COBRANZA" -> "COBRANZA_DIARIA"
            "ESTADO_CARTERA" -> "CARTERA_GLOBAL"
            "CIERRE_CAJA" -> "ARQUEO_GENERAL"
            "EVALUACION_SCORE" -> "EVALUACION_RIESGO"
            else -> ""
        }
    }

    // Filter computation helpers
    fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Apply Filter predicates
    val filteredLoans = remember(loans, selectedDateRange, selectedCobrador, selectedRuta) {
        loans.filter { loan ->
            val matchesCobrador = selectedCobrador == "TODOS" || loan.collectorName == selectedCobrador
            val client = clients.find { it.id == loan.clientId }
            val matchesRuta = selectedRuta == "TODAS" || client?.zone == selectedRuta
            val matchesDate = when (selectedDateRange) {
                "Hoy" -> loan.createdAt >= getStartOfDay()
                "Esta Semana" -> loan.createdAt >= getStartOfWeek()
                "Este Mes" -> loan.createdAt >= getStartOfMonth()
                else -> true
            }
            matchesCobrador && matchesRuta && matchesDate
        }
    }

    val filteredPayments = remember(payments, selectedDateRange, selectedCobrador, selectedMetodo) {
        payments.filter { payment ->
            val matchesCobrador = selectedCobrador == "TODOS" || payment.collectorName == selectedCobrador
            val matchesMetodo = selectedMetodo == "TODOS" || payment.method.equals(selectedMetodo, ignoreCase = true)
            val matchesDate = when (selectedDateRange) {
                "Hoy" -> payment.paymentDate >= getStartOfDay()
                "Esta Semana" -> payment.paymentDate >= getStartOfWeek()
                "Este Mes" -> payment.paymentDate >= getStartOfMonth()
                else -> true
            }
            matchesCobrador && matchesMetodo && matchesDate
        }
    }

    Scaffold(
        topBar = {
            RamaTopBar(title = "Reportes y Estados de Cuenta", onBackClick = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main tabs with backward-compatible test tags
            val reportTabs = listOf("HOJA_COBRANZA", "ESTADO_CARTERA", "CIERRE_CAJA", "EVALUACION_SCORE")
            val selectedTabIndex = reportTabs.indexOf(selectedReport).coerceAtLeast(0)

            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedReport == "HOJA_COBRANZA",
                    onClick = { selectedReport = "HOJA_COBRANZA" },
                    text = { Text("Hoja Cobranza") },
                    modifier = Modifier.testTag("tab_report_cobranza")
                )
                Tab(
                    selected = selectedReport == "ESTADO_CARTERA",
                    onClick = { selectedReport = "ESTADO_CARTERA" },
                    text = { Text("Estado Cartera") },
                    modifier = Modifier.testTag("tab_report_cartera")
                )
                Tab(
                    selected = selectedReport == "CIERRE_CAJA",
                    onClick = { selectedReport = "CIERRE_CAJA" },
                    text = { Text("Resumen Caja") },
                    modifier = Modifier.testTag("tab_report_caja")
                )
                Tab(
                    selected = selectedReport == "EVALUACION_SCORE",
                    onClick = { selectedReport = "EVALUACION_SCORE" },
                    text = { Text("Score & Riesgo") },
                    modifier = Modifier.testTag("tab_report_evaluacion")
                )
            }

            // Sub-Report Category Selection Chips Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Seleccione Reporte Específico:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (selectedReport) {
                            "HOJA_COBRANZA" -> {
                                FilterChip(
                                    selected = selectedSubReport == "COBRANZA_DIARIA",
                                    onClick = { selectedSubReport = "COBRANZA_DIARIA" },
                                    label = { Text("Cobranza Diaria") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "POR_COBRADOR",
                                    onClick = { selectedSubReport = "POR_COBRADOR" },
                                    label = { Text("Por Cobrador") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "POR_RUTA",
                                    onClick = { selectedSubReport = "POR_RUTA" },
                                    label = { Text("Por Ruta") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "PROMESAS_PAGO",
                                    onClick = { selectedSubReport = "PROMESAS_PAGO" },
                                    label = { Text("Promesas") }
                                )
                            }
                            "ESTADO_CARTERA" -> {
                                FilterChip(
                                    selected = selectedSubReport == "CARTERA_GLOBAL",
                                    onClick = { selectedSubReport = "CARTERA_GLOBAL" },
                                    label = { Text("Cartera Global") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "LISTA_PRESTAMOS",
                                    onClick = { selectedSubReport = "LISTA_PRESTAMOS" },
                                    label = { Text("Lista Préstamos") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "HISTORIAL_MORA",
                                    onClick = { selectedSubReport = "HISTORIAL_MORA" },
                                    label = { Text("Historial Mora") }
                                )
                            }
                            "CIERRE_CAJA" -> {
                                FilterChip(
                                    selected = selectedSubReport == "ARQUEO_GENERAL",
                                    onClick = { selectedSubReport = "ARQUEO_GENERAL" },
                                    label = { Text("Arqueo General") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "MOVIMIENTOS_CAJA",
                                    onClick = { selectedSubReport = "MOVIMIENTOS_CAJA" },
                                    label = { Text("Movimientos") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "RENOVACIONES",
                                    onClick = { selectedSubReport = "RENOVACIONES" },
                                    label = { Text("Renovaciones") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "AUTORIZACIONES",
                                    onClick = { selectedSubReport = "AUTORIZACIONES" },
                                    label = { Text("Autorizaciones") }
                                )
                            }
                            "EVALUACION_SCORE" -> {
                                FilterChip(
                                    selected = selectedSubReport == "EVALUACION_RIESGO",
                                    onClick = { selectedSubReport = "EVALUACION_RIESGO" },
                                    label = { Text("Riesgo Crediticio") }
                                )
                                FilterChip(
                                    selected = selectedSubReport == "AUDITORIA",
                                    onClick = { selectedSubReport = "AUDITORIA" },
                                    label = { Text("Auditoría Logs") }
                                )
                            }
                        }
                    }
                }
            }

            // FILTER CARD WITH DROP DOWNS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Filtros Generales del Reporte",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Range Select
                        var showDateDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showDateDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Fecha: $selectedDateRange",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            DropdownMenu(expanded = showDateDropdown, onDismissRequest = { showDateDropdown = false }) {
                                val ranges = listOf("Todo el tiempo", "Hoy", "Esta Semana", "Este Mes")
                                ranges.forEach { range ->
                                    DropdownMenuItem(
                                        text = { Text(range) },
                                        onClick = {
                                            selectedDateRange = range
                                            showDateDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Cobrador Select
                        var showCobradorDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showCobradorDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Cobrador: $selectedCobrador",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            DropdownMenu(expanded = showCobradorDropdown, onDismissRequest = { showCobradorDropdown = false }) {
                                DropdownMenuItem(text = { Text("TODOS") }, onClick = { selectedCobrador = "TODOS"; showCobradorDropdown = false })
                                val collectors = users.filter { it.role == "COBRADOR" }.map { it.fullName }
                                collectors.forEach { name ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = { selectedCobrador = name; showCobradorDropdown = false })
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Route Select
                        var showRutaDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showRutaDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Zona: $selectedRuta",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            DropdownMenu(expanded = showRutaDropdown, onDismissRequest = { showRutaDropdown = false }) {
                                DropdownMenuItem(text = { Text("TODAS") }, onClick = { selectedRuta = "TODAS"; showRutaDropdown = false })
                                val zones = clients.map { it.zone }.distinct().filter { it.isNotBlank() }
                                zones.forEach { zone ->
                                    DropdownMenuItem(text = { Text(zone) }, onClick = { selectedRuta = zone; showRutaDropdown = false })
                                }
                            }
                        }

                        // Method Select (Efectivo/Transferencia)
                        var showMetodoDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showMetodoDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Método: $selectedMetodo",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            DropdownMenu(expanded = showMetodoDropdown, onDismissRequest = { showMetodoDropdown = false }) {
                                val methods = listOf("TODOS", "EFECTIVO", "TRANSFERENCIA")
                                methods.forEach { method ->
                                    DropdownMenuItem(text = { Text(method) }, onClick = { selectedMetodo = method; showMetodoDropdown = false })
                                }
                            }
                        }
                    }

                    // Reset Filters Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedDateRange = "Todo el tiempo"
                            selectedCobrador = "TODOS"
                            selectedRuta = "TODAS"
                            selectedMetodo = "TODOS"
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar Filtros", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar cliente/folio...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier
                                .width(200.dp)
                                .height(48.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // DETAILED REPORT CARD CONTENT WITH AGGREGATE COMPUTATIONS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header logo & company details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RAMA MICROFINANZAS - ERP",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Reporte: ${selectedSubReport.replace("_", " ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Divider()

                    // Dynamic Sub-Report UI Engine
                    when (selectedSubReport) {
                        "COBRANZA_DIARIA" -> {
                            val dueToday = installments.filter { inst ->
                                val matchesDate = inst.dueDateFormatted == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
                                matchesDate
                            }

                            val totalExpectedToday = dueToday.fold(0.0) { sum, inst -> sum + inst.targetAmount }
                            val totalCollectedToday = dueToday.fold(0.0) { sum, inst -> sum + inst.paidAmount }
                            val complianceRate = if (totalExpectedToday > 0) (totalCollectedToday / totalExpectedToday * 100) else 0.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Cobranza Esperada", style = MaterialTheme.typography.labelSmall)
                                    Text(currencyFormat.format(totalExpectedToday), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Cobranza Realizada", style = MaterialTheme.typography.labelSmall)
                                    Text(currencyFormat.format(totalCollectedToday), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                }
                                Column {
                                    Text("Cumplimiento", style = MaterialTheme.typography.labelSmall)
                                    Text(String.format("%.1f%%", complianceRate), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Divider()

                            // Render dynamic groups: PENDIENTES, ATRASADOS, CRITICOS, AL CORRIENTE
                            val pendingList = dueToday.filter { it.status == "PENDIENTE" }
                            val paidList = dueToday.filter { it.status == "PAGADO" }
                            val criticalCount = dueToday.count { it.status == "PENDIENTE" && it.dueDateFormatted < "2026" } // Simulate legacy criticals

                            Text("Resumen de Operación Diaria:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Críticos", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                                        Text("$criticalCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFFC62828))
                                    }
                                }
                                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Pendientes", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                                        Text("${pendingList.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFFE65100))
                                    }
                                }
                                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Cobrado", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                                        Text("${paidList.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                                    }
                                }
                            }
                        }

                        "POR_COBRADOR" -> {
                            // Group payments by collector name and display performance summary
                            val paymentsByCollector = filteredPayments.groupBy { it.collectorName }
                            Text("Rendimiento por Cobrador:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (paymentsByCollector.isEmpty()) {
                                Text("Sin movimientos de cobranza para los filtros seleccionados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                paymentsByCollector.forEach { (collector, collectorPayments) ->
                                    val totalAmount = collectorPayments.sumOf { it.amount }
                                    val efectivo = collectorPayments.filter { it.method == "EFECTIVO" }.sumOf { it.amount }
                                    val transferencia = collectorPayments.filter { it.method == "TRANSFERENCIA" }.sumOf { it.amount }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(collector, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Efectivo: ${currencyFormat.format(efectivo)} | Transf: ${currencyFormat.format(transferencia)}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(currencyFormat.format(totalAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    }
                                    Divider()
                                }
                            }
                        }

                        "POR_RUTA" -> {
                            val paymentsByRuta = filteredPayments.groupBy {
                                val client = clients.find { c -> c.fullName == it.clientName }
                                client?.zone ?: "Sin Zona"
                            }
                            Text("Cobranza Agregada por Zona / Ruta:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (paymentsByRuta.isEmpty()) {
                                Text("No hay datos de cobranza para las zonas indicadas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                paymentsByRuta.forEach { (zone, zonePayments) ->
                                    val total = zonePayments.sumOf { it.amount }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(zone, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(currencyFormat.format(total), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    }
                                    Divider()
                                }
                            }
                        }

                        "PROMESAS_PAGO" -> {
                            val promises = visits.filter { it.result == "PROMESA_PAGO" }
                            Text("Control de Promesas de Pago:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (promises.isEmpty()) {
                                Text("No se registran promesas de pago actualmente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                promises.forEach { promise ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(promise.clientName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Prometido: ${currencyFormat.format(promise.promisedAmount ?: 0.0)} el ${promise.promisedPaymentDate?.let { dateFormat.format(java.util.Date(it)) } ?: "fecha N/A"}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if ((promise.promisedPaymentDate ?: 0) < System.currentTimeMillis()) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                            )
                                        ) {
                                            Text(
                                                text = if ((promise.promisedPaymentDate ?: 0) < System.currentTimeMillis()) "Vencido/Brecha" else "Próxima",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = if ((promise.promisedPaymentDate ?: 0) < System.currentTimeMillis()) Color(0xFFC62828) else Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                    Divider()
                                }
                            }
                        }

                        "CARTERA_GLOBAL" -> {
                            val totalColocado = filteredLoans.sumOf { it.capital }
                            val totalRecuperado = filteredLoans.sumOf { it.paidAmount }
                            val totalPendiente = filteredLoans.sumOf { it.remainingBalance }

                            Text("Métricas Generales de Cartera Activa:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Capital Colocado", style = MaterialTheme.typography.labelSmall)
                                    Text(currencyFormat.format(totalColocado), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Recuperado", style = MaterialTheme.typography.labelSmall)
                                    Text(currencyFormat.format(totalRecuperado), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                }
                                Column {
                                    Text("Saldo Pendiente", style = MaterialTheme.typography.labelSmall)
                                    Text(currencyFormat.format(totalPendiente), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                                }
                            }

                            // Dynamic distribution graphics using canvas
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Distribución de Cartera:", style = MaterialTheme.typography.labelSmall)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                            ) {
                                val colPct = if (totalColocado > 0) (totalRecuperado / totalColocado).toFloat() else 0f
                                drawRect(
                                    color = Color(0xFF4CAF50),
                                    size = size.copy(width = size.width * colPct)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Recuperado: ${String.format("%.1f%%", if (totalColocado > 0) (totalRecuperado/totalColocado*100) else 0.0)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                                Text("Pendiente: ${String.format("%.1f%%", if (totalColocado > 0) (totalPendiente/totalColocado*100) else 100.0)}", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                            }
                        }

                        "LISTA_PRESTAMOS" -> {
                            Text("Detalle de Préstamos Emitidos:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (filteredLoans.isEmpty()) {
                                Text("No hay préstamos para el rango seleccionado.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                filteredLoans.forEach { loan ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Folio: ${loan.folio} | Client: ${loan.clientName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Capital: ${currencyFormat.format(loan.capital)} | Plan: ${loan.planType.replace("PLAN_", "")} | Status: ${loan.status}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(currencyFormat.format(loan.remainingBalance), style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold))
                                    }
                                    Divider()
                                }
                            }
                        }

                        "HISTORIAL_MORA" -> {
                            val overdueLoans = loans.filter { it.status == "ATRASADO" }
                            Text("Cálculo y Conciliación de Mora:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (overdueLoans.isEmpty()) {
                                Text("Sano: 100% de la cartera al corriente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            } else {
                                overdueLoans.forEach { loan ->
                                    val originalOverdueAmount = loan.dailyPayment // Assume 1 overdue installment
                                    val calculatedLateFee = originalOverdueAmount * loan.lateFeePercentage // Standard 5%
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(loan.clientName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Cuota Afectada: ${currencyFormat.format(originalOverdueAmount)} | Mora (5%): ${currencyFormat.format(calculatedLateFee)}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text("Retrasado", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Red, fontWeight = FontWeight.Bold))
                                    }
                                    Divider()
                                }
                            }
                        }

                        "ARQUEO_GENERAL" -> {
                            Text("Resumen de Cajas y Arqueos:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (registers.isEmpty()) {
                                Text("Sin registros de cajas guardadas.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                registers.forEach { reg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Cobrador: ${reg.collectorName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Apertura: ${currencyFormat.format(reg.initialCash)} | Efectivo: ${currencyFormat.format(reg.cashInflows)} | Transf: ${currencyFormat.format(reg.transferInflows)}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(reg.status, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            if (reg.discrepancy != null && reg.discrepancy != 0.0) {
                                                Text("Diferencia: ${currencyFormat.format(reg.discrepancy)}", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                                            }
                                        }
                                    }
                                    Divider()
                                }
                            }
                        }

                        "MOVIMIENTOS_CAJA" -> {
                            Text("Movimientos Detallados de Caja:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (movements.isEmpty()) {
                                Text("Sin transacciones registradas.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                movements.forEach { mvt ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(mvt.concept, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("${mvt.type} | ${mvt.paymentMethod} | ${dateTimeFormat.format(java.util.Date(mvt.timestamp))}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(
                                            text = "${if (mvt.type == "INGRESO") "+" else "-"}${currencyFormat.format(mvt.amount)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (mvt.type == "INGRESO") Color(0xFF2E7D32) else Color(0xFFC62828),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Divider()
                                }
                            }
                        }

                        "RENOVACIONES" -> {
                            val renewals = loans.filter { it.isRenewal }
                            Text("Control y Bitácora de Renovaciones:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (renewals.isEmpty()) {
                                Text("No hay registros de renovación.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                renewals.forEach { ren ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(ren.clientName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Préstamo Anterior ID: #${ren.previousLoanId ?: "N/A"} | Renovado: ${currencyFormat.format(ren.capital)}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text("Renovado", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                                    }
                                    Divider()
                                }
                            }
                        }

                        "AUTORIZACIONES" -> {
                            val approvedList = loans.filter { it.approvedByUserId != null }
                            Text("Bitácora de Autorizaciones Emitidas:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (approvedList.isEmpty()) {
                                Text("No se registran autorizaciones administrativas.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                approvedList.forEach { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Préstamo #${app.id} | Cliente: ${app.clientName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Autorizado por: ${app.approvedByUserId} el ${app.approvedAt?.let { dateFormat.format(java.util.Date(it)) } ?: "Fecha N/A"}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text("Aprobado", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold))
                                    }
                                    Divider()
                                }
                            }
                        }

                        "EVALUACION_RIESGO" -> {
                            Text("Score y Riesgo Crediticio de Cartera:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (clients.isEmpty()) {
                                Text("Cargando perfil de clientes...", style = MaterialTheme.typography.bodySmall)
                            } else {
                                clients.forEach { client ->
                                    val score = client.punctualityScore
                                    val risk = when {
                                        score >= 80 -> "BAJO"
                                        score >= 50 -> "MEDIO"
                                        else -> "ALTO"
                                    }
                                    val color = when (risk) {
                                        "BAJO" -> Color(0xFF2E7D32)
                                        "MEDIO" -> Color(0xFFE65100)
                                        else -> Color(0xFFC62828)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(client.fullName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Score: $score pts | Tel: ${client.phone}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(risk, style = MaterialTheme.typography.bodyMedium.copy(color = color, fontWeight = FontWeight.Bold))
                                    }
                                    Divider()
                                }
                            }
                        }

                        "AUDITORIA" -> {
                            if (currentUserRole == "COBRADOR") {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Lock, contentDescription = "Acceso Denegado", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Acceso Denegado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text("Su rol (Cobrador) no tiene los permisos requeridos (AUDIT_VIEW) para consultar la bitácora de auditoría global.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            } else {
                                val processedLogs = remember(auditLogs, searchQuery, selectedAuditActionFilter, selectedAuditResultFilter, currentUserRole) {
                                    auditLogs.filter { log ->
                                        if (currentUserRole == "SUPERVISOR") {
                                            val sensitiveActions = listOf("RESET_PASSWORD", "BLOQUEAR_USUARIO", "DESBLOQUEAR_USUARIO", "USER_DISABLE")
                                            if (sensitiveActions.contains(log.action)) return@filter false
                                        }
                                        val matchesSearch = searchQuery.isBlank() || 
                                                log.id.toString() == searchQuery ||
                                                (log.traceId ?: "").contains(searchQuery, ignoreCase = true) ||
                                                log.username.contains(searchQuery, ignoreCase = true) ||
                                                log.action.contains(searchQuery, ignoreCase = true) ||
                                                (log.notes ?: "").contains(searchQuery, ignoreCase = true) ||
                                                (log.reason ?: "").contains(searchQuery, ignoreCase = true) ||
                                                log.entityId.contains(searchQuery, ignoreCase = true) ||
                                                log.entityType.contains(searchQuery, ignoreCase = true)

                                        val matchesAction = when (selectedAuditActionFilter) {
                                            "TODOS" -> true
                                            "USUARIOS" -> log.action.contains("USER", ignoreCase = true) || log.action.contains("USUARIO", ignoreCase = true) || log.entityType.contains("User", ignoreCase = true)
                                            "CLIENTES" -> log.action.contains("CLIENT", ignoreCase = true) || log.action.contains("CLIENTE", ignoreCase = true) || log.entityType.contains("Client", ignoreCase = true)
                                            "PRESTAMOS" -> log.action.contains("LOAN", ignoreCase = true) || log.action.contains("PRESTAMO", ignoreCase = true) || log.action.contains("AUTORIZAR", ignoreCase = true) || log.entityType.contains("Loan", ignoreCase = true)
                                            "PAGOS" -> log.action.contains("PAYMENT", ignoreCase = true) || log.action.contains("PAGO", ignoreCase = true) || log.entityType.contains("Payment", ignoreCase = true)
                                            "CAJA" -> log.action.contains("CASH", ignoreCase = true) || log.action.contains("CAJA", ignoreCase = true) || log.entityType.contains("Cash", ignoreCase = true)
                                            "CONFIGURACION" -> log.action == "CONFIG_UPDATED" || log.entityType.contains("Config", ignoreCase = true)
                                            else -> true
                                        }

                                        val matchesResult = selectedAuditResultFilter == "TODOS" || log.result.equals(selectedAuditResultFilter, ignoreCase = true)

                                        matchesSearch && matchesAction && matchesResult
                                    }
                                }

                                val configChanges = remember(processedLogs) {
                                    processedLogs.filter { it.action == "CONFIG_UPDATED" }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Header with Sub-tabs (only for Admin/Supervisor)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Control y Auditoría de Seguridad", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        
                                        if (currentUserRole == "ADMINISTRADOR") {
                                            // Admin specific switcher
                                            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(4.dp)) {
                                                Text(
                                                    "Bitácora",
                                                    modifier = Modifier
                                                        .background(if (selectedAuditTab == "BITACORA") MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                                                        .clickable { selectedAuditTab = "BITACORA" }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    color = if (selectedAuditTab == "BITACORA") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                Text(
                                                    "Configuración",
                                                    modifier = Modifier
                                                        .background(if (selectedAuditTab == "CONFIG") MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                                                        .clickable { selectedAuditTab = "CONFIG" }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    color = if (selectedAuditTab == "CONFIG") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }

                                    // Display category filters row
                                    if (selectedAuditTab == "BITACORA") {
                                        Text("Filtros de Categoría:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val cats = listOf("TODOS", "USUARIOS", "CLIENTES", "PRESTAMOS")
                                            cats.forEach { cat ->
                                                FilterChip(
                                                    selected = selectedAuditActionFilter == cat,
                                                    onClick = { selectedAuditActionFilter = cat },
                                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val cats = listOf("PAGOS", "CAJA", "CONFIGURACION")
                                            cats.forEach { cat ->
                                                FilterChip(
                                                    selected = selectedAuditActionFilter == cat,
                                                    onClick = { selectedAuditActionFilter = cat },
                                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Resultado:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
                                            FilterChip(
                                                selected = selectedAuditResultFilter == "TODOS",
                                                onClick = { selectedAuditResultFilter = "TODOS" },
                                                label = { Text("Todos") }
                                            )
                                            FilterChip(
                                                selected = selectedAuditResultFilter == "EXITOSO",
                                                onClick = { selectedAuditResultFilter = "EXITOSO" },
                                                label = { Text("Exitoso") }
                                            )
                                            FilterChip(
                                                selected = selectedAuditResultFilter == "FALLIDO",
                                                onClick = { selectedAuditResultFilter = "FALLIDO" },
                                                label = { Text("Fallido") }
                                            )
                                        }
                                    }

                                    // Render Lists based on sub-tab
                                    if (selectedAuditTab == "BITACORA") {
                                        if (processedLogs.isEmpty()) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            ) {
                                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                                    Text("No se encontraron registros de auditoría que coincidan con la búsqueda o filtros.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                                }
                                            }
                                        } else {
                                            processedLogs.forEach { log ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { selectedAuditForDetail = log },
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "${log.username} (${log.userRole ?: "Usuario"})",
                                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                                )
                                                                Text(
                                                                    text = dateTimeFormat.format(java.util.Date(log.timestamp)),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                SuggestionChip(
                                                                    onClick = {},
                                                                    label = { Text(log.action, style = MaterialTheme.typography.labelSmall) }
                                                                )
                                                                val isSuccess = log.result.equals("EXITOSO", ignoreCase = true)
                                                                AssistChip(
                                                                    onClick = {},
                                                                    label = { Text(if (isSuccess) "Éxito" else "Fallo", style = MaterialTheme.typography.labelSmall) },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                                            contentDescription = null,
                                                                            tint = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                    }
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "Trazabilidad: ${log.traceId}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                text = "ID Entidad (${log.entityType}): ${log.entityId}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            log.notes?.let { notes ->
                                                                if (notes.isNotBlank()) {
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        text = "Obs: $notes",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                            }
                                        }
                                    } else {
                                        // CONFIG TAB - Historial de Configuración
                                        Text("Cambios de Configuración Registrados:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        if (configChanges.isEmpty()) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            ) {
                                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                                    Text("No hay cambios de configuración registrados.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                                }
                                            }
                                        } else {
                                            configChanges.forEach { log ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { selectedAuditForDetail = log },
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Modificado por: ${log.username}",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                text = dateTimeFormat.format(java.util.Date(log.timestamp)),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text("Motivo: ${log.notes ?: "N/A"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Parámetro Anterior:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                                        Text(log.previousValues ?: "N/A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Parámetro Nuevo:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                                                        Text(log.newValues ?: "N/A", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // AuditDetailDialog integrated directly in place
                            if (selectedAuditForDetail != null) {
                                val log = selectedAuditForDetail!!
                                AlertDialog(
                                    onDismissRequest = { selectedAuditForDetail = null },
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Text("Detalle de Evento #${log.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    text = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("OPERACIÓN:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(log.action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            
                                            Text("TRAZABILIDAD (ID):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(log.traceId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            Toast.makeText(context, "ID Copiado: ${log.traceId}", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Copiar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                }
                                            }

                                            Text("ACTOR:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("${log.username} (${log.userRole ?: "Rol N/A"})", style = MaterialTheme.typography.bodyMedium)

                                            Text("FECHA Y HORA:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(dateTimeFormat.format(java.util.Date(log.timestamp)), style = MaterialTheme.typography.bodyMedium)

                                            Text("ENTIDAD AFECTADA:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("Tipo: ${log.entityType} | ID: ${log.entityId}", style = MaterialTheme.typography.bodyMedium)

                                            Text("RESULTADO:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            val isSuccess = log.result.equals("EXITOSO", ignoreCase = true)
                                            Text(
                                                text = if (isSuccess) "ÉXITO" else "FALLIDO",
                                                color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            log.reason?.let { r ->
                                                if (r.isNotBlank()) {
                                                    Text("MOTIVO:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Text(r, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }

                                            log.notes?.let { n ->
                                                if (n.isNotBlank()) {
                                                    Text("NOTAS / OBSERVACIONES:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Text(n, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }

                                            Text("DISPOSITIVO / ORIGEN:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(log.ipDevice, style = MaterialTheme.typography.bodyMedium)

                                            if (log.latitude != null && log.longitude != null) {
                                                Text("UBICACIÓN GPS:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text("Lat: ${log.latitude}, Lon: ${log.longitude}", style = MaterialTheme.typography.bodyMedium)
                                            }

                                            log.previousValues?.let { prev ->
                                                if (prev.isNotBlank()) {
                                                    Text("ESTADO ANTERIOR:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(prev, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                                    }
                                                }
                                            }

                                            log.newValues?.let { next ->
                                                if (next.isNotBlank()) {
                                                    Text("ESTADO NUEVO:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(next, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = { selectedAuditForDetail = null }
                                        ) {
                                            Text("Cerrar")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Divider()

                    // PDF / EXCEL EXPORTS AND PRINT MOCK TRIGGERS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    // Simulated high fidelity PDF layout via formatted share Intent
                                    val textBuilder = StringBuilder()
                                    textBuilder.append("--- RAMA MICROFINANZAS - REPORTE DE ${selectedSubReport} ---\n")
                                    textBuilder.append("Generado: ${dateTimeFormat.format(java.util.Date())}\n")
                                    textBuilder.append("Filtros aplicados - Fecha: $selectedDateRange, Cobrador: $selectedCobrador, Zona: $selectedRuta\n\n")

                                    textBuilder.append("--- DATOS DEL INFORME ---\n")
                                    when (selectedSubReport) {
                                        "COBRANZA_DIARIA" -> textBuilder.append("Frecuencia: Diaria\nTotal Préstamos: ${loans.size}\nCapital Colocado: ${currencyFormat.format(loans.sumOf { it.capital })}")
                                        "LISTA_PRESTAMOS" -> filteredLoans.forEach { textBuilder.append("${it.folio} | ${it.clientName} | ${currencyFormat.format(it.capital)}\n") }
                                        else -> textBuilder.append("Detalles guardados y listados en sistema.")
                                    }

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Reporte ${selectedSubReport}")
                                        putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Exportar PDF / Imprimir"))
                                    Toast.makeText(context, "Exportando PDF...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("btn_export_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar PDF")
                        }

                        Button(
                            onClick = {
                                try {
                                    // CSV Generation Engine for Excel Compatibility
                                    val csvFile = File(context.cacheDir, "reporte_${selectedSubReport.lowercase()}_export.csv")
                                    FileWriter(csvFile).use { writer ->
                                        // Header
                                        writer.write("Folio/ID,Cliente,Información,Monto,Mora,Fecha\n")
                                        when (selectedSubReport) {
                                            "COBRANZA_DIARIA" -> {
                                                installments.forEach {
                                                    writer.write("${it.id},${it.loanId},Vencimiento: ${it.dueDateFormatted},${it.targetAmount},${it.paidAmount},${it.dueDateFormatted}\n")
                                                }
                                            }
                                            "LISTA_PRESTAMOS" -> {
                                                filteredLoans.forEach {
                                                    writer.write("${it.folio},${it.clientName},Plan: ${it.planType},${it.capital},0.0,${dateFormat.format(Date(it.createdAt))}\n")
                                                }
                                            }
                                            else -> {
                                                writer.write("1,Ejemplo General,Filtros: $selectedDateRange,100.0,0.0,${dateFormat.format(Date())}\n")
                                            }
                                        }
                                    }

                                    Toast.makeText(context, "Excel compatible (CSV) guardado en caché", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al generar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("btn_print")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Imprimir / Excel")
                        }
                    }
                }
            }
        }
    }
}
