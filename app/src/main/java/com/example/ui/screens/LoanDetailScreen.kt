package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.LoanEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaStatusBadge
import com.example.ui.components.RamaTopBar
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LoanDetailScreen(
    loanId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var loan by remember { mutableStateOf<LoanEntity?>(null) }
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val customPlans by viewModel.customLoanPlans.collectAsStateWithLifecycle()
    val installments by viewModel.loanRepo.getInstallmentsByLoan(loanId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val lateFeeHistory by viewModel.getLateFeeHistoryForLoan(loanId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val skippedLogs by viewModel.getSkippedPaymentsForLoan(loanId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val activeCash by viewModel.activeCashRegister.collectAsStateWithLifecycle()

    val userRole = userSession.user?.role?.uppercase() ?: "AGENTE"
    val isSupervisorOrAdmin = userRole == "SUPERVISOR" || userRole == "ADMINISTRADOR"

    var showEditDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    var editCapitalInput by remember { mutableStateOf("") }
    var editPlanCode by remember { mutableStateOf("PLAN_20_DIAS") }
    var editSkipSundays by remember { mutableStateOf(true) }

    var cancelReasonInput by remember { mutableStateOf("Cancelado por solicitud del cliente") }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    LaunchedEffect(loanId) {
        loan = viewModel.loanRepo.getLoanById(loanId)
        viewModel.evaluateLateFeesForLoan(loanId)
    }

    // Edit Loan Dialog
    if (showEditDialog && loan != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Préstamo #${loan?.id} (Pre-desembolso)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editCapitalInput,
                        onValueChange = { editCapitalInput = it },
                        label = { Text("Nuevo Capital (MXN)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_capital"),
                        singleLine = true
                    )
                    Text("Plan de Crédito", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        customPlans.forEach { plan ->
                            FilterChip(
                                selected = editPlanCode == plan.code,
                                onClick = { editPlanCode = plan.code },
                                label = { Text("${plan.name}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editSkipSundays, onCheckedChange = { editSkipSundays = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Omitir Domingos")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = editCapitalInput.toDoubleOrNull() ?: 0.0
                        if (cap > 0) {
                            viewModel.editLoanBeforeDisbursement(
                                loanId = loanId,
                                newCapital = cap,
                                newPlanTypeCode = editPlanCode,
                                newSkipSundays = editSkipSundays
                            ) {
                                showEditDialog = false
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_edit_loan_btn")
                ) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Cancel Loan Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Préstamo #${loanId}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Está seguro de cancelar este préstamo antes del desembolso?")
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Motivo de Cancelación") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cancel_reason")
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.cancelLoanBeforeDisbursement(loanId, cancelReasonInput) {
                            showCancelDialog = false
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("confirm_cancel_loan_btn")
                ) {
                    Text("Cancelar Préstamo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Volver")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            RamaTopBar(title = loan?.let { "Préstamo #${it.id} - ${it.clientName}" } ?: "Detalle del Préstamo", onBackClick = onBack)
        },
        bottomBar = {
            val l = loan
            if (l != null) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (l.status) {
                            "PENDIENTE" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSupervisorOrAdmin) {
                                        Button(
                                            onClick = {
                                                viewModel.authorizeLoan(loanId) {
                                                    onBack()
                                                }
                                            },
                                            modifier = Modifier.weight(1f).testTag("btn_authorize_loan"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                        ) {
                                            Icon(Icons.Default.PriceCheck, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Autorizar")
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.disburseLoan(loanId) {
                                                onBack()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).testTag("btn_disburse_loan")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Desembolsar")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            editCapitalInput = l.capital.toString()
                                            editPlanCode = l.planType
                                            editSkipSundays = l.skipSundays
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.weight(1f).testTag("btn_edit_loan")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar")
                                    }

                                    OutlinedButton(
                                        onClick = { showCancelDialog = true },
                                        modifier = Modifier.weight(1f).testTag("btn_cancel_loan"),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancelar")
                                    }
                                }
                            }

                            "AUTORIZADO" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.disburseLoan(loanId) {
                                                onBack()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).testTag("btn_disburse_loan")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Desembolsar Préstamo")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            editCapitalInput = l.capital.toString()
                                            editPlanCode = l.planType
                                            editSkipSundays = l.skipSundays
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.weight(1f).testTag("btn_edit_loan")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar")
                                    }
                                }
                            }

                            "ACTIVO", "VENCIDO" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onNavigate("renewal_loan/$loanId") },
                                        modifier = Modifier.weight(1f).testTag("btn_renew_from_detail")
                                    ) {
                                        Icon(Icons.Default.Autorenew, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Renovar")
                                    }

                                    Button(
                                        onClick = { onNavigate("process_payment/$loanId") },
                                        enabled = activeCash != null && activeCash?.status == "ABIERTA",
                                        modifier = Modifier.weight(1f).testTag("btn_pay_from_detail")
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Abonar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val l = loan
        if (l == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                        text = l.clientName,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text("CURP: ${l.clientCurp}", style = MaterialTheme.typography.bodySmall)
                                }
                                RamaStatusBadge(status = l.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Capital Original:")
                                Text(currencyFormat.format(l.capital), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Interés Ganado:")
                                Text(currencyFormat.format(l.interestAmount), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Acordado:")
                                Text(currencyFormat.format(l.totalAmount), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Pagado:")
                                Text(currencyFormat.format(l.paidAmount), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Saldo Restante:")
                                Text(
                                    currencyFormat.format(l.remainingBalance),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cuota Diaria:")
                                Text(currencyFormat.format(l.dailyPayment), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    com.example.ui.components.SmartPaymentScheduleComponent(
                        loan = l,
                        installments = installments,
                        lateFeeHistory = lateFeeHistory,
                        skippedLogs = skippedLogs,
                        onRegisterPayment = { _, amount, method, notes, redistributeAdvance ->
                            viewModel.registerSmartPayment(
                                loanId = loanId,
                                amount = amount,
                                method = method,
                                notes = notes,
                                redistributeAdvance = redistributeAdvance,
                                onDone = { _ ->
                                    viewModel.evaluateLateFeesForLoan(loanId)
                                }
                            )
                        },
                        onRecordSkippedPayment = { instId, reason ->
                            viewModel.recordSkippedPayment(
                                loanId = loanId,
                                installmentId = instId,
                                reason = reason,
                                onDone = {
                                    viewModel.evaluateLateFeesForLoan(loanId)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
