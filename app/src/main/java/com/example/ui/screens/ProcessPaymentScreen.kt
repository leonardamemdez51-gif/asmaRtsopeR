package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.InstallmentEntity
import com.example.data.local.LoanEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SystemConfigEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProcessPaymentScreen(
    loanId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPaymentCompleted: () -> Unit
) {
    val context = LocalContext.current
    val systemConfigState by viewModel.systemConfig.collectAsStateWithLifecycle()
    val config = systemConfigState ?: SystemConfigEntity()

    var loan by remember { mutableStateOf<LoanEntity?>(null) }
    var installments by remember { mutableStateOf<List<InstallmentEntity>>(emptyList()) }

    var paymentType by remember { mutableStateOf("NORMAL") } // NORMAL, PARCIAL, ADELANTADO, LIQUIDACION_TOTAL, MULTI_CUOTA
    var amountInput by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("EFECTIVO") }
    var quotaCount by remember { mutableIntStateOf(2) }
    var redistributeAdvance by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var proofPhotoUri by remember { mutableStateOf<String?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var showAddMethodDialog by remember { mutableStateOf(false) }
    var customMethodName by remember { mutableStateOf("") }

    var registeredPayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var showReceiptModal by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX")) }

    // Parse configured payment methods
    val configuredMethods = remember(config.paymentMethodsCsv) {
        config.paymentMethodsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("Efectivo", "Transferencia", "Tarjeta", "Depósito") }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            proofPhotoUri = uri.toString()
        }
    }

    // Load Loan and Installments
    LaunchedEffect(loanId) {
        val l = viewModel.loanRepo.getLoanById(loanId)
        loan = l
        if (l != null) {
            val insts = viewModel.loanRepo.getInstallmentsByLoanSync(loanId)
            installments = insts

            // Calculate default amount for single quota
            val nextUnpaid = insts.find { it.status != "PAGADO" && it.remainingAmount > 0 }
            val defaultAmt = if (nextUnpaid != null) {
                nextUnpaid.remainingAmount + nextUnpaid.lateFee
            } else {
                l.dailyPayment
            }
            amountInput = defaultAmt.toString()
        }
    }

    val l = loan
    val totalRemainingBalance = l?.remainingBalance ?: 0.0
    val totalLateFees = installments.sumOf { it.lateFee }
    val totalDebtToLiquidate = (totalRemainingBalance + totalLateFees).coerceAtLeast(0.0)

    // Helper to calculate multi-quota total
    fun calculateMultiQuotaTotal(count: Int): Double {
        val unpaid = installments.filter { it.status != "PAGADO" && it.remainingAmount > 0 }
            .take(count)
        val sum = unpaid.sumOf { it.remainingAmount + it.lateFee }
        return if (sum > 0) sum else (l?.dailyPayment ?: 0.0) * count
    }

    Scaffold(
        topBar = {
            RamaTopBar(
                title = "Registro de Pagos",
                subtitle = l?.let { "Préstamo #${it.id} - ${it.clientName}" } ?: "Módulo de Cobranza",
                onBackClick = onBack
            )
        }
    ) { padding ->
        if (l == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Loan Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = l.clientName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Préstamo #${l.id} | ${l.planType.replace("PLAN_", "").replace("_", " ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (l.status) {
                                    "LIQUIDADO" -> Color(0xFF2E7D32)
                                    "MORA" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ) {
                                Text(
                                    text = l.status,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Saldo Restante", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(l.remainingBalance),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text("Cuota Diaria", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(l.dailyPayment),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Column {
                                Text("Mora Acumulada", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(totalLateFees),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (totalLateFees > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Payment Type Selection Tabs (Formulario Rápido)
                Text(
                    text = "Tipo / Modalidad de Pago *",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paymentType == "NORMAL",
                        onClick = {
                            paymentType = "NORMAL"
                            val nextUnpaid = installments.find { it.status != "PAGADO" && it.remainingAmount > 0 }
                            val defaultAmt = if (nextUnpaid != null) nextUnpaid.remainingAmount + nextUnpaid.lateFee else l.dailyPayment
                            amountInput = defaultAmt.toString()
                        },
                        label = { Text("⚡ Pago Normal") },
                        modifier = Modifier.testTag("chip_pago_normal")
                    )

                    FilterChip(
                        selected = paymentType == "PARCIAL",
                        onClick = {
                            paymentType = "PARCIAL"
                            val halfQuota = Math.round((l.dailyPayment / 2.0) * 100.0) / 100.0
                            amountInput = halfQuota.toString()
                        },
                        label = { Text("🌓 Pago Parcial") },
                        modifier = Modifier.testTag("chip_pago_parcial")
                    )

                    FilterChip(
                        selected = paymentType == "ADELANTADO",
                        onClick = {
                            paymentType = "ADELANTADO"
                            val doubleQuota = Math.round((l.dailyPayment * 2.0) * 100.0) / 100.0
                            amountInput = doubleQuota.toString()
                        },
                        label = { Text("🚀 Pago Adelantado") },
                        modifier = Modifier.testTag("chip_pago_adelantado")
                    )

                    FilterChip(
                        selected = paymentType == "MULTI_CUOTA",
                        onClick = {
                            paymentType = "MULTI_CUOTA"
                            amountInput = calculateMultiQuotaTotal(quotaCount).toString()
                        },
                        label = { Text("🎯 Varias Cuotas") },
                        modifier = Modifier.testTag("chip_pago_multi_cuota")
                    )

                    FilterChip(
                        selected = paymentType == "LIQUIDACION_TOTAL",
                        onClick = {
                            paymentType = "LIQUIDACION_TOTAL"
                            amountInput = totalDebtToLiquidate.toString()
                        },
                        label = { Text("🏆 Liquidación Total") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.testTag("chip_pago_liquidacion")
                    )
                }

                // Options specific to payment mode
                if (paymentType == "MULTI_CUOTA") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Número de cuotas a pagar:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedIconButton(
                                    onClick = {
                                        if (quotaCount > 1) {
                                            quotaCount--
                                            amountInput = calculateMultiQuotaTotal(quotaCount).toString()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Menos")
                                }

                                Text(
                                    text = "$quotaCount Cuotas (${currencyFormat.format(calculateMultiQuotaTotal(quotaCount))})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedIconButton(
                                    onClick = {
                                        if (quotaCount < 15) {
                                            quotaCount++
                                            amountInput = calculateMultiQuotaTotal(quotaCount).toString()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Más")
                                }
                            }
                        }
                    }
                }

                if (paymentType == "ADELANTADO") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Redistribuir saldo en cuotas restantes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Reduce el monto de las cuotas futuras manteniendo la fecha final del préstamo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = redistributeAdvance,
                            onCheckedChange = { redistributeAdvance = it }
                        )
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        errorMessage = null
                    },
                    label = { Text("Monto Aportado (MXN) *") },
                    leadingIcon = { Text("$", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp)) },
                    trailingIcon = {
                        if (amountInput.isNotEmpty()) {
                            IconButton(onClick = { amountInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_payment_amount"),
                    singleLine = true
                )

                // Quick Preset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { amountInput = l.dailyPayment.toString() },
                        label = { Text("Cuota Exacta") }
                    )
                    SuggestionChip(
                        onClick = { amountInput = totalDebtToLiquidate.toString() },
                        label = { Text("Liquidación (${currencyFormat.format(totalDebtToLiquidate)})") }
                    )
                }

                // Payment Method
                Text("Método de Pago *", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    configuredMethods.forEach { method ->
                        FilterChip(
                            selected = paymentMethod.equals(method, ignoreCase = true),
                            onClick = { paymentMethod = method.uppercase() },
                            label = { Text(method) },
                            leadingIcon = {
                                if (method.contains("Efectivo", ignoreCase = true)) {
                                    Icon(Icons.Default.Payments, contentDescription = null)
                                } else if (method.contains("Transfer", ignoreCase = true)) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null)
                                } else {
                                    Icon(Icons.Default.CreditCard, contentDescription = null)
                                }
                            }
                        )
                    }

                    AssistChip(
                        onClick = { showAddMethodDialog = true },
                        label = { Text("+ Método") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                }

                // Proof of Payment Section (Comprobantes)
                Text("Comprobante de Pago (Opcional)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (proofPhotoUri != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Comprobante Adjuntado", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { proofPhotoUri = null },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Eliminar")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Adjuntar Foto")
                                }

                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tomar Foto")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Observaciones / N° de Referencia") },
                            placeholder = { Text("Ej: Ref. Banamex 9812739") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Error Message Display
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Action Buttons (Confirm, Cancel)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isProcessing) return@Button
                            val amt = amountInput.toDoubleOrNull() ?: 0.0

                            if (l.status == "LIQUIDADO") {
                                errorMessage = "El préstamo ya se encuentra LIQUIDADO."
                                return@Button
                            }

                            if (amt <= 0.0) {
                                errorMessage = "Ingrese un monto mayor a $0 MXN"
                                return@Button
                            }

                            if (amt > totalDebtToLiquidate + 0.5 && paymentType != "LIQUIDACION_TOTAL") {
                                errorMessage = "El monto excede la deuda total restante de ${currencyFormat.format(totalDebtToLiquidate)}"
                                return@Button
                            }

                            errorMessage = null
                            isProcessing = true

                            viewModel.registerSmartPayment(
                                loanId = loanId,
                                amount = amt,
                                method = paymentMethod,
                                notes = notes,
                                paymentType = paymentType,
                                proofPhotoUri = proofPhotoUri,
                                redistributeAdvance = redistributeAdvance,
                                onDone = { success ->
                                    isProcessing = false
                                    if (success) {
                                        registeredPayment = PaymentEntity(
                                            loanId = loanId,
                                            clientId = l.clientId,
                                            clientName = l.clientName,
                                            collectorId = 1L,
                                            collectorName = "Cobrador Principal",
                                            amount = amt,
                                            method = paymentMethod,
                                            paymentDate = System.currentTimeMillis(),
                                            receiptNumber = "REC-${System.currentTimeMillis().toString().takeLast(8)}",
                                            notes = notes,
                                            isAdvance = redistributeAdvance,
                                            paymentType = paymentType,
                                            proofPhotoUri = proofPhotoUri
                                        )
                                        showReceiptModal = true
                                    } else {
                                        errorMessage = "Error al procesar el pago. Intente nuevamente."
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_payment_button"),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Procesando Pago...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmar Pago de ${currencyFormat.format(amountInput.toDoubleOrNull() ?: 0.0)}", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }

    // Add Custom Payment Method Dialog
    if (showAddMethodDialog) {
        AlertDialog(
            onDismissRequest = { showAddMethodDialog = false },
            title = { Text("Agregar Método de Pago") },
            text = {
                OutlinedTextField(
                    value = customMethodName,
                    onValueChange = { customMethodName = it },
                    label = { Text("Nombre del método (Ej: Coppel Pay, Cheque)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customMethodName.trim()
                        if (name.isNotEmpty()) {
                            val updated = (configuredMethods + name).distinct().joinToString(",")
                            viewModel.updateSystemConfig(config.copy(paymentMethodsCsv = updated))
                            paymentMethod = name.uppercase()
                            customMethodName = ""
                            showAddMethodDialog = false
                            Toast.makeText(context, "Método '$name' agregado a Configuración", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMethodDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Receipt Modal (Comprobante / Imprimir Recibo)
    if (showReceiptModal && registeredPayment != null && loan != null) {
        val pay = registeredPayment!!
        val currLoan = loan!!

        Dialog(onDismissRequest = { /* Prevent accidental dismiss */ }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Success Icon
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                    }

                    Text("¡Pago Registrado con Éxito!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                    // Printable Receipt Layout
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = config.companyName.uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "COMPROBANTE DE PAGO",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Folio: ${pay.receiptNumber}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fecha:", style = MaterialTheme.typography.bodySmall)
                                Text(dateFormat.format(Date(pay.paymentDate)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cliente:", style = MaterialTheme.typography.bodySmall)
                                Text(pay.clientName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Préstamo:", style = MaterialTheme.typography.bodySmall)
                                Text("#${pay.loanId}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Modalidad:", style = MaterialTheme.typography.bodySmall)
                                Text(pay.paymentType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Método:", style = MaterialTheme.typography.bodySmall)
                                Text(pay.method, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monto Pagado:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    currencyFormat.format(pay.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nuevo Saldo Restante:", style = MaterialTheme.typography.bodySmall)
                                val newBal = (currLoan.remainingBalance - pay.amount).coerceAtLeast(0.0)
                                Text(currencyFormat.format(newBal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }

                            if (pay.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Obs: ${pay.notes}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Print & Share Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "🖨️ Enviando recibo ${pay.receiptNumber} a Impresora Térmica...", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Imprimir")
                        }

                        Button(
                            onClick = {
                                try {
                                    val msg = "RAMA Microfinanzas - Comprobante de Pago\nFolio: ${pay.receiptNumber}\nCliente: ${pay.clientName}\nMonto: ${currencyFormat.format(pay.amount)}\nMétodo: ${pay.method}\n¡Gracias por tu pago!"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, msg)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Compartir Recibo"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir app de mensajería", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartir")
                        }
                    }

                    Button(
                        onClick = {
                            showReceiptModal = false
                            onPaymentCompleted()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Finalizar y Volver")
                    }
                }
            }
        }
    }
}
