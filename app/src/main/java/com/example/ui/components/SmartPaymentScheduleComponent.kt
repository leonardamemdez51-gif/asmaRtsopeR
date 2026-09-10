package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InstallmentEntity
import com.example.data.local.LateFeeHistoryEntity
import com.example.data.local.LoanEntity
import com.example.data.local.SkippedPaymentLogEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Custom Color Constants matching prompt requirements
val StatusColorPaid = Color(0xFF2E7D32)       // Pagado - Green
val StatusColorPending = Color(0xFF0288D1)    // Pendiente - Blue
val StatusColorOverdue = Color(0xFFED6C02)    // Vencido - Orange/Amber
val StatusColorLateFee = Color(0xFFD32F2F)    // Mora - Red
val StatusColorPartial = Color(0xFF7B1FA2)    // Pago Parcial - Purple

@Composable
fun SmartStatusChip(status: String, isSkipped: Boolean = false) {
    val (bgColor, fgColor, label) = when {
        isSkipped -> Triple(Color(0xFFE0E0E0), Color(0xFF424242), "OMITIDO")
        status == "PAGADO" -> Triple(StatusColorPaid.copy(alpha = 0.15f), StatusColorPaid, "PAGADO")
        status == "PARCIAL" -> Triple(StatusColorPartial.copy(alpha = 0.15f), StatusColorPartial, "PARCIAL")
        status == "MORA" -> Triple(StatusColorLateFee.copy(alpha = 0.15f), StatusColorLateFee, "EN MORA")
        status == "VENCIDO" -> Triple(StatusColorOverdue.copy(alpha = 0.15f), StatusColorOverdue, "VENCIDO")
        else -> Triple(StatusColorPending.copy(alpha = 0.15f), StatusColorPending, "PENDIENTE")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, fgColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fgColor)
            )
            Text(
                text = label,
                color = fgColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPaymentScheduleComponent(
    loan: LoanEntity,
    installments: List<InstallmentEntity>,
    lateFeeHistory: List<LateFeeHistoryEntity> = emptyList(),
    skippedLogs: List<SkippedPaymentLogEntity> = emptyList(),
    onRegisterPayment: (installmentId: Long, amount: Double, method: String, notes: String, redistributeAdvance: Boolean) -> Unit,
    onRecordSkippedPayment: (installmentId: Long, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isTableView by remember { mutableStateOf(false) } // false = Timeline, true = Table
    var selectedFilter by remember { mutableStateOf("TODOS") } 
    var selectedOrder by remember { mutableStateOf("FECHA") } // "FECHA", "NUMERO", "SALDO", "MORA", "ESTADO"
    var showOrderMenu by remember { mutableStateOf(false) }

    var selectedInstallmentForPay by remember { mutableStateOf<InstallmentEntity?>(null) }
    var selectedInstallmentForSkip by remember { mutableStateOf<InstallmentEntity?>(null) }
    var showLateFeeHistoryDialog by remember { mutableStateOf(false) }

    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val dateSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Summary calculations
    val totalInstallments = installments.size
    val paidCount = installments.count { it.status == "PAGADO" }
    val pendingCount = installments.count { it.status == "PENDIENTE" }
    val overdueCount = installments.count { it.status == "VENCIDO" }
    val lateFeeCount = installments.count { it.status == "MORA" }
    val partialCount = installments.count { it.status == "PARCIAL" }

    val totalCapital = installments.sumOf { it.capitalComponent }
    val totalInterest = installments.sumOf { it.interestComponent }
    val totalLateFeeActive = installments.sumOf { it.lateFee }
    val totalPaid = installments.sumOf { it.paidAmount }
    val remainingBalance = (loan.totalAmount - totalPaid).coerceAtLeast(0.0)

    val filteredInstallments = remember(installments, selectedFilter, selectedOrder) {
        val nowMs = System.currentTimeMillis()
        val todayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(nowMs))
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val filtered = when (selectedFilter) {
            "PAGADAS" -> installments.filter { it.status == "PAGADO" }
            "PENDIENTES" -> installments.filter { it.status == "PENDIENTE" }
            "VENCIDAS" -> installments.filter { it.status == "VENCIDO" }
            "PARCIALES" -> installments.filter { it.status == "PARCIAL" }
            "HOY" -> installments.filter { sdf.format(Date(it.dueDate)) == todayFmt && it.status != "PAGADO" }
            "PROXIMAS" -> installments.filter { it.dueDate > nowMs && it.status != "PAGADO" }
            "CON_MORA" -> installments.filter { it.lateFee > 0.0 }
            "SIN_MORA" -> installments.filter { it.lateFee <= 0.0 }
            else -> installments
        }
        
        when (selectedOrder) {
            "FECHA" -> filtered.sortedBy { it.dueDate }
            "NUMERO" -> filtered.sortedBy { it.installmentNumber }
            "SALDO" -> filtered.sortedByDescending { it.remainingAmount }
            "MORA" -> filtered.sortedByDescending { it.lateFee }
            "ESTADO" -> filtered.sortedBy { it.status }
            else -> filtered
        }
    }

    // Dialog: Payment / Advance Registration
    val payInst = selectedInstallmentForPay
    if (payInst != null) {
        var payAmountText by remember { mutableStateOf((payInst.remainingAmount + payInst.lateFee).toString()) }
        var payMethod by remember { mutableStateOf("EFECTIVO") }
        var payNotes by remember { mutableStateOf("Pago cuota #${payInst.installmentNumber}") }
        var redistributeAdvance by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { selectedInstallmentForPay = null },
            title = {
                Text("Abono / Adelanto Cuota #${payInst.installmentNumber}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Monto sugerido (Cuota + Mora): ${currency.format(payInst.remainingAmount + payInst.lateFee)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text("Monto a Pagar (MXN)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_smart_pay_amount"),
                        singleLine = true
                    )

                    Text("Método de Pago", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = payMethod == "EFECTIVO",
                            onClick = { payMethod = "EFECTIVO" },
                            label = { Text("Efectivo") }
                        )
                        FilterChip(
                            selected = payMethod == "TRANSFERENCIA",
                            onClick = { payMethod = "TRANSFERENCIA" },
                            label = { Text("Transferencia") }
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = redistributeAdvance,
                                onCheckedChange = { redistributeAdvance = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    "Redistribuir saldo en cuotas restantes",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Reduce la cuota diaria de las mensualidades futuras sin alterar la fecha final de vencimiento.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text("Notas del Cobro") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onRegisterPayment(payInst.id, amt, payMethod, payNotes, redistributeAdvance)
                            selectedInstallmentForPay = null
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_smart_payment")
                ) {
                    Text("Registrar Pago")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInstallmentForPay = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Record Skipped Payment
    val skipInst = selectedInstallmentForSkip
    if (skipInst != null) {
        var skipReasonText by remember { mutableStateOf("Cliente no se encontró en su domicilio") }

        AlertDialog(
            onDismissRequest = { selectedInstallmentForSkip = null },
            title = { Text("Registrar Pago Omitido - Cuota #${skipInst.installmentNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Indique el motivo por el cual se omitió el pago de la cuota:")
                    OutlinedTextField(
                        value = skipReasonText,
                        onValueChange = { skipReasonText = it },
                        label = { Text("Motivo de Omisión") },
                        modifier = Modifier.fillMaxWidth().testTag("input_skip_reason")
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onRecordSkippedPayment(skipInst.id, skipReasonText)
                        selectedInstallmentForSkip = null
                    },
                    modifier = Modifier.testTag("btn_confirm_skip_payment")
                ) {
                    Text("Registrar Omisión")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInstallmentForSkip = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Late Fee History
    if (showLateFeeHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showLateFeeHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusColorLateFee)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Historial de Mora de Préstamo #${loan.id}")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    if (lateFeeHistory.isEmpty()) {
                        Text(
                            "No hay registros de mora acumulados para este préstamo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lateFeeHistory.forEach { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Cuota #${log.installmentNumber} | ${log.notes}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("Días de atraso: ${log.daysOverdueAtApplication} días", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(currency.format(log.amountApplied), fontWeight = FontWeight.ExtraBold, color = StatusColorLateFee)
                                            SmartStatusChip(status = log.status)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLateFeeHistoryDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card & Engine Indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Motor Inteligente de Pagos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Plan: ${if (loan.planType == "PLAN_20_DIAS") "20 Días" else if (loan.planType == "PLAN_30_DIAS") "30 Días" else loan.planType} | Vencimiento Inmutable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // View Switcher (Timeline vs Table)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isTableView = false },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (!isTableView) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .testTag("btn_view_timeline")
                        ) {
                            Icon(
                                Icons.Default.ViewTimeline,
                                contentDescription = "Línea de Tiempo",
                                tint = if (!isTableView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { isTableView = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isTableView) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .testTag("btn_view_table")
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = "Tabla",
                                tint = if (isTableView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Key Financial Metrics Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Capital Original", style = MaterialTheme.typography.labelSmall)
                        Text(currency.format(totalCapital), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Column {
                        Text("Interés Total", style = MaterialTheme.typography.labelSmall)
                        Text(currency.format(totalInterest), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Column {
                        Text("Mora Activa", style = MaterialTheme.typography.labelSmall, color = StatusColorLateFee)
                        Text(currency.format(totalLateFeeActive), fontWeight = FontWeight.Bold, color = StatusColorLateFee, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Saldo Restante", style = MaterialTheme.typography.labelSmall)
                        Text(currency.format(remainingBalance), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Overall Schedule Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val progress = if (totalInstallments > 0) paidCount.toFloat() / totalInstallments.toFloat() else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Progreso: $paidCount de $totalInstallments cuotas pagadas", style = MaterialTheme.typography.labelSmall)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = StatusColorPaid,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Action Bar for Late Fee Audit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showLateFeeHistoryDialog = true },
                        modifier = Modifier.testTag("btn_open_late_fee_history")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Historial de Mora (${lateFeeHistory.size})", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                "TODOS" to "Todas",
                "PENDIENTES" to "Pendientes",
                "PAGADAS" to "Pagadas",
                "PARCIALES" to "Parciales",
                "VENCIDAS" to "Vencidas",
                "HOY" to "Hoy",
                "PROXIMAS" to "Próximas",
                "CON_MORA" to "Con mora",
                "SIN_MORA" to "Sin mora"
            )

            filterOptions.forEach { (code, label) ->
                FilterChip(
                    selected = selectedFilter == code,
                    onClick = { selectedFilter = code },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        // Main Schedule Render: Timeline View OR Table View
        if (isTableView) {
            // TABLE VIEW
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), fontSize = 12.sp)
                        Text("Fecha", fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp), fontSize = 12.sp)
                        Text("Capital", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), fontSize = 12.sp)
                        Text("Interés", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), fontSize = 12.sp)
                        Text("Total", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), fontSize = 12.sp)
                        Text("Recibido", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), fontSize = 12.sp)
                        Text("Mora", fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp), fontSize = 12.sp, color = StatusColorLateFee)
                        Text("Saldo", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), fontSize = 12.sp)
                        Text("Estado", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), fontSize = 12.sp)
                        Text("Acciones", fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp), fontSize = 12.sp)
                    }

                    HorizontalDivider()

                    filteredInstallments.forEachIndexed { index, inst ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${inst.installmentNumber}", modifier = Modifier.width(36.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(inst.dueDateFormatted, modifier = Modifier.width(90.dp), fontSize = 12.sp)
                            Text(currency.format(inst.capitalComponent), modifier = Modifier.width(80.dp), fontSize = 12.sp)
                            Text(currency.format(inst.interestComponent), modifier = Modifier.width(80.dp), fontSize = 12.sp)
                            Text(currency.format(inst.targetAmount), modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(currency.format(inst.paidAmount), modifier = Modifier.width(80.dp), fontSize = 12.sp, color = StatusColorPaid)
                            Text(currency.format(inst.lateFee), modifier = Modifier.width(70.dp), fontSize = 12.sp, color = StatusColorLateFee)
                            Text(currency.format(inst.remainingAmount), modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.width(100.dp)) {
                                SmartStatusChip(status = inst.status, isSkipped = inst.isSkipped)
                            }
                            Row(modifier = Modifier.width(110.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (inst.status != "PAGADO") {
                                    IconButton(
                                        onClick = { selectedInstallmentForPay = inst },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = "Abonar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { selectedInstallmentForSkip = inst },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "Omitir", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Pagado", tint = StatusColorPaid, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        } else {
            // TIMELINE VIEW
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                filteredInstallments.forEachIndexed { index, inst ->
                    val isLast = index == filteredInstallments.size - 1

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Timeline Column (Node + Connecting Line)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(32.dp)
                        ) {
                            val nodeColor = when {
                                inst.isSkipped -> Color.Gray
                                inst.status == "PAGADO" -> StatusColorPaid
                                inst.status == "PARCIAL" -> StatusColorPartial
                                inst.status == "MORA" -> StatusColorLateFee
                                inst.status == "VENCIDO" -> StatusColorOverdue
                                else -> StatusColorPending
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(nodeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${inst.installmentNumber}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                )
                            }

                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(100.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Timeline Card Content
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 12.dp)
                                .testTag("timeline_item_${inst.installmentNumber}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (inst.status) {
                                    "PAGADO" -> MaterialTheme.colorScheme.surface
                                    "MORA", "VENCIDO" -> StatusColorLateFee.copy(alpha = 0.05f)
                                    "PARCIAL" -> StatusColorPartial.copy(alpha = 0.05f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Cuota #${inst.installmentNumber}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            inst.dueDateFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    SmartStatusChip(status = inst.status, isSkipped = inst.isSkipped)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Capital: ${currency.format(inst.capitalComponent)}", style = MaterialTheme.typography.labelSmall)
                                        Text("Interés: ${currency.format(inst.interestComponent)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total: ${currency.format(inst.targetAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("Abonado: ${currency.format(inst.paidAmount)}", style = MaterialTheme.typography.labelSmall, color = StatusColorPaid)
                                    }
                                }

                                if (inst.lateFee > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StatusColorLateFee.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusColorLateFee, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Mora aplicada (${inst.daysOverdue}d atraso)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StatusColorLateFee
                                            )
                                        }
                                        Text(
                                            "+${currency.format(inst.lateFee)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = StatusColorLateFee
                                        )
                                    }
                                }

                                if (inst.isSkipped) {
                                    Text(
                                        "Pago Omitido: ${inst.skippedReason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                // Interactive Action Buttons
                                if (inst.status != "PAGADO") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { selectedInstallmentForSkip = inst },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Omitir", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = { selectedInstallmentForPay = inst },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Abonar / Adelanto", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
