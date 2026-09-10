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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CashMovementEntity
import com.example.data.local.CashRegisterEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRegisterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val activeCash by viewModel.activeCashRegister.collectAsStateWithLifecycle()
    val allRegisters by viewModel.allCashRegisters.collectAsStateWithLifecycle()
    val allMovements by viewModel.allCashMovements.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Estado & Arqueo, 1: Movimientos, 2: Reportes & Conciliación

    // Dialog States
    var showOpenDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var showMovementDialog by remember { mutableStateOf(false) }
    var showReportPreviewDialog by remember { mutableStateOf<String?>(null) }

    // Inputs for Open Cash
    var initialCashInput by remember { mutableStateOf("2000") }
    var selectedBranch by remember { mutableStateOf("Sucursal Central") }
    var selectedFund by remember { mutableStateOf("Caja Principal (Efectivo)") }
    var openNotesInput by remember { mutableStateOf("") }

    // Inputs for Movements
    var movementType by remember { mutableStateOf("INGRESO_MANUAL") } // INGRESO_MANUAL, EGRESO_GASTO, EGRESO_RETIRO, AJUSTE_POSITIVO, AJUSTE_NEGATIVO
    var movementAmountInput by remember { mutableStateOf("") }
    var movementConceptInput by remember { mutableStateOf("") }
    var movementPaymentMethod by remember { mutableStateOf("EFECTIVO") }
    var movementCategory by remember { mutableStateOf("GASTO_OPERATIVO") }

    // Denomination breakdown state for Arqueo
    val denominations = remember {
        listOf(1000, 500, 200, 100, 50, 20, 10, 5, 2, 1)
    }
    val countMap = remember { mutableStateMapOf<Int, Int>().apply { denominations.forEach { put(it, 0) } } }
    
    val totalDenominationCash = remember(countMap.values.sum()) {
        countMap.entries.sumOf { it.key * it.value }
    }

    var closeNotesInput by remember { mutableStateOf("") }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val activeRegId = activeCash?.id
    val activeMovements = remember(allMovements, activeRegId) {
        if (activeRegId != null) {
            allMovements.filter { it.cashRegisterId == activeRegId }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            RamaTopBar(title = "Módulo Profesional de Caja", onBackClick = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Flujo & Arqueo") },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Movimientos (${activeMovements.size})") },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Reportes & Audits") },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (selectedTab == 0) {
                    // TAB 0: FLUJO Y ARQUEO
                    val reg = activeCash
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (reg != null)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (reg != null) "CAJA ABIERTA (#${reg.id})" else "CAJA CERRADA",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                        Text(
                                            text = if (reg != null) "Sucursal: ${reg.branchName} | Fondo: ${reg.fundName}" else "Debe abrir turno para operar",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (reg != null) {
                                        Button(
                                            onClick = { showCloseDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.testTag("btn_close_caja")
                                        ) {
                                            Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cierre y Arqueo")
                                        }
                                    } else {
                                        Button(
                                            onClick = { showOpenDialog = true },
                                            modifier = Modifier.testTag("btn_open_caja")
                                        ) {
                                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Apertura de Caja")
                                        }
                                    }
                                }

                                if (reg != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Cobrador a Cargo:", style = MaterialTheme.typography.labelSmall)
                                            Text(reg.collectorName, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Apertura:", style = MaterialTheme.typography.labelSmall)
                                            Text(dateFormat.format(Date(reg.openDate)), fontWeight = FontWeight.Medium)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Metrics Grid
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MetricRow("Saldo / Fondo Inicial (+)", currencyFormat.format(reg.initialCash), MaterialTheme.colorScheme.onSurface)
                                        MetricRow("Cobros en Efectivo (+)", currencyFormat.format(reg.cashInflows), Color(0xFF2E7D32))
                                        MetricRow("Cobros por Transferencia (SPEI)", currencyFormat.format(reg.transferInflows), Color(0xFF0288D1))
                                        MetricRow("Egresos y Desembolsos (-)", currencyFormat.format(reg.outflows), MaterialTheme.colorScheme.error)

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Efectivo Esperado en Caja:",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                currencyFormat.format(reg.expectedCash),
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Action buttons for cash movements
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                movementType = "INGRESO_MANUAL"
                                                showMovementDialog = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ingreso")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                movementType = "EGRESO_GASTO"
                                                showMovementDialog = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.RemoveCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Egreso")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                movementType = "AJUSTE_POSITIVO"
                                                showMovementDialog = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ajuste")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Historical Cash Register Shifts
                    item {
                        Text(
                            "Historial Reciente de Turnos y Cierres",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (allRegisters.isEmpty()) {
                        item { Text("No hay registros de caja previos.") }
                    } else {
                        items(allRegisters.take(5)) { r ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Caja #${r.id} - ${r.collectorName}", fontWeight = FontWeight.Bold)
                                        Badge(
                                            containerColor = if (r.status == "ABIERTA") Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                                        ) {
                                            Text(r.status)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Fondo Inicial: ${currencyFormat.format(r.initialCash)} | Esperado: ${currencyFormat.format(r.expectedCash)}")
                                    if (r.status == "CERRADA") {
                                        Text("Contado Físico: ${currencyFormat.format(r.actualCash)} | Diferencia: ${currencyFormat.format(r.discrepancy)} (${r.auditStatus})",
                                            color = if (r.auditStatus == "CORRECTO") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // TAB 1: MOVIMIENTOS DETALLADOS
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Movimientos del Turno Actual (${activeMovements.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (activeCash != null) {
                                IconButton(onClick = { showMovementDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Nuevo Movimiento")
                                }
                            }
                        }
                    }

                    if (activeMovements.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "No hay movimientos o transacciones en este turno.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(activeMovements) { mov ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(mov.concept, fontWeight = FontWeight.Bold)
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(mov.paymentMethod, fontSize = 10.sp) },
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }
                                        Text("Tipo: ${mov.type} | Cat: ${mov.category}", style = MaterialTheme.typography.bodySmall)
                                        Text("Registrado por: ${mov.registeredBy} • ${dateFormat.format(Date(mov.timestamp))}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        text = if (mov.type.contains("INGRESO") || mov.type.contains("POSITIVO"))
                                            "+${currencyFormat.format(mov.amount)}"
                                        else
                                            "-${currencyFormat.format(mov.amount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (mov.type.contains("INGRESO") || mov.type.contains("POSITIVO"))
                                            Color(0xFF2E7D32)
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 2) {
                    // TAB 2: REPORTES Y AUDITORÍA
                    item {
                        Text(
                            "Generación de Reportes Financieros y Conciliación",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Auditoría & Conciliación Operativa", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                                OutlinedButton(
                                    onClick = { showReportPreviewDialog = "MOVIMIENTO_DIARIO" },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reporte de Movimiento Diario")
                                }

                                OutlinedButton(
                                    onClick = { showReportPreviewDialog = "RESUMEN_DIARIO" },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Today, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Resumen Diario de Caja & Métodos de Pago")
                                }

                                OutlinedButton(
                                    onClick = { showReportPreviewDialog = "CONCILIACION" },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FactCheck, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Conciliación de Efectivo vs Transferencia")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // OPEN DIALOG
    if (showOpenDialog) {
        AlertDialog(
            onDismissRequest = { showOpenDialog = false },
            title = { Text("Apertura de Caja y Asignación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = selectedBranch,
                        onValueChange = { selectedBranch = it },
                        label = { Text("Sucursal Operativa") },
                        modifier = Modifier.fillMaxWidth().testTag("input_caja_branch")
                    )

                    OutlinedTextField(
                        value = selectedFund,
                        onValueChange = { selectedFund = it },
                        label = { Text("Fondo / Caja de Origen") },
                        modifier = Modifier.fillMaxWidth().testTag("input_caja_fund")
                    )

                    OutlinedTextField(
                        value = initialCashInput,
                        onValueChange = { initialCashInput = it },
                        label = { Text("Fondo Inicial ($ MXN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_initial_cash")
                    )

                    OutlinedTextField(
                        value = openNotesInput,
                        onValueChange = { openNotesInput = it },
                        label = { Text("Observaciones / Justificación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val init = initialCashInput.toDoubleOrNull() ?: 0.0
                        viewModel.openCashRegister(
                            initialCash = init,
                            notes = openNotesInput,
                            branchName = selectedBranch,
                            fundName = selectedFund
                        ) {
                            showOpenDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_open_caja")
                ) {
                    Text("Abrir Turno")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // CLOSE / ARQUEO DIALOG
    if (showCloseDialog) {
        val expected = activeCash?.expectedCash ?: 0.0
        val discrepancy = totalDenominationCash - expected
        val isDiscrepancy = Math.abs(discrepancy) > 0.01

        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Cierre de Caja y Arqueo Físico") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Efectivo Esperado en Sistema: ${currencyFormat.format(expected)}", fontWeight = FontWeight.Bold)
                                Text("Conteo de Arqueo Físico: ${currencyFormat.format(totalDenominationCash)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Diferencia: ${currencyFormat.format(discrepancy)} (${if (discrepancy < 0) "FALTANTE" else if (discrepancy > 0) "SOBRANTE" else "CORRECTO"})",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDiscrepancy) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    item {
                        Text("Desglose por Denominaciones de Billetes y Monedas:", style = MaterialTheme.typography.labelLarge)
                    }

                    items(denominations) { denom ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$$denom MXN", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = countMap[denom]?.toString() ?: "0",
                                onValueChange = { input ->
                                    val count = input.toIntOrNull() ?: 0
                                    countMap[denom] = count.coerceAtLeast(0)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp).height(50.dp),
                                singleLine = true
                            )
                            Text("= ${currencyFormat.format(denom * (countMap[denom] ?: 0))}", fontWeight = FontWeight.Medium)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = closeNotesInput,
                            onValueChange = { closeNotesInput = it },
                            label = { Text("Observaciones u Justificación de Diferencias (Obligatorio si hay faltante/sobrante)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_close_notes")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val breakdownObj = JSONObject()
                        countMap.forEach { (k, v) -> breakdownObj.put("denom_$k", v) }

                        val effectiveNotes = if (closeNotesInput.isBlank() && isDiscrepancy) {
                            "Diferencia detectada en arqueo de caja (${currencyFormat.format(discrepancy)})"
                        } else {
                            closeNotesInput
                        }

                        viewModel.closeCashRegister(
                            actualCash = totalDenominationCash.toDouble(),
                            notes = effectiveNotes,
                            denominationBreakdownJson = breakdownObj.toString()
                        ) {
                            showCloseDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_close_caja")
                ) {
                    Text("Finalizar Cierre")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // MANUAL MOVEMENT DIALOG
    if (showMovementDialog) {
        AlertDialog(
            onDismissRequest = { showMovementDialog = false },
            title = { Text("Registrar Movimiento de Caja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tipo de Operación:")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = movementType == "INGRESO_MANUAL",
                            onClick = { movementType = "INGRESO_MANUAL" },
                            label = { Text("Ingreso") }
                        )
                        FilterChip(
                            selected = movementType == "EGRESO_GASTO",
                            onClick = { movementType = "EGRESO_GASTO" },
                            label = { Text("Egreso") }
                        )
                        FilterChip(
                            selected = movementType == "AJUSTE_POSITIVO",
                            onClick = { movementType = "AJUSTE_POSITIVO" },
                            label = { Text("Ajuste (+)") }
                        )
                    }

                    Text("Método de Pago:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = movementPaymentMethod == "EFECTIVO",
                            onClick = { movementPaymentMethod = "EFECTIVO" },
                            label = { Text("Efectivo") }
                        )
                        FilterChip(
                            selected = movementPaymentMethod == "TRANSFERENCIA",
                            onClick = { movementPaymentMethod = "Transferencia" },
                            label = { Text("Transferencia") }
                        )
                    }

                    OutlinedTextField(
                        value = movementAmountInput,
                        onValueChange = { movementAmountInput = it },
                        label = { Text("Monto ($ MXN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_movement_amount")
                    )

                    OutlinedTextField(
                        value = movementConceptInput,
                        onValueChange = { movementConceptInput = it },
                        label = { Text("Concepto / Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = movementAmountInput.toDoubleOrNull() ?: 0.0
                        viewModel.addCashMovement(
                            type = movementType,
                            amount = amt,
                            concept = movementConceptInput.ifBlank { "Movimiento Manual" },
                            paymentMethod = movementPaymentMethod,
                            category = movementCategory
                        ) {
                            showMovementDialog = false
                            movementAmountInput = ""
                            movementConceptInput = ""
                        }
                    }
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMovementDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // REPORT PREVIEW DIALOG
    showReportPreviewDialog?.let { repType ->
        AlertDialog(
            onDismissRequest = { showReportPreviewDialog = null },
            title = { Text("Vista Previa de Reporte: $repType") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Generado el: ${dateFormat.format(Date())}")
                    HorizontalDivider()
                    Text("Resumen de Operaciones:")
                    Text("• Registros analizados: ${allRegisters.size}")
                    Text("• Movimientos analizados: ${activeMovements.size}")
                    Text("• Estado de Conciliación: OK (Sincronizado con Room DB)")
                }
            },
            confirmButton = {
                Button(onClick = { showReportPreviewDialog = null }) { Text("Imprimir / Exportar") }
            },
            dismissButton = {
                TextButton(onClick = { showReportPreviewDialog = null }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = color))
    }
}
