package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.LoanCalculator
import com.example.domain.PlanType
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLoanScreen(
    preselectedClientId: Long? = null,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLoanCreated: (Long) -> Unit
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val customPlans by viewModel.customLoanPlans.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var selectedClientId by remember { mutableStateOf(preselectedClientId ?: (clients.firstOrNull()?.id ?: 0L)) }
    var selectedPlanCode by remember { mutableStateOf("PLAN_20_DIAS") }
    var capitalInput by remember { mutableStateOf("3000") }
    var skipSundays by remember { mutableStateOf(true) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    // Live calculations
    val capital = capitalInput.toDoubleOrNull() ?: 0.0
    val calculation = remember(capital, selectedPlanCode, customPlans) {
        val plan = customPlans.find { it.code == selectedPlanCode }
        if (plan != null) {
            LoanCalculator.calculateLoan(capital, plan.rate, plan.days)
        } else {
            val pType = if (selectedPlanCode == "PLAN_30_DIAS") PlanType.PLAN_30_DIAS else PlanType.PLAN_20_DIAS
            LoanCalculator.calculateLoan(capital, pType)
        }
    }

    val selectedClient = clients.find { it.id == selectedClientId } ?: clients.firstOrNull()

    Scaffold(
        topBar = {
            RamaTopBar(title = "Nuevo Préstamo", onBackClick = onBack)
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
            Text(
                text = "Configuración del Préstamo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Client Selector Dropdown
            ExposedDropdownMenuBox(
                expanded = clientDropdownExpanded,
                onExpandedChange = { clientDropdownExpanded = !clientDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedClient?.let { "${it.fullName} (${it.curp})" } ?: "Seleccionar Cliente",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cliente Seleccionado *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("select_client_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = clientDropdownExpanded,
                    onDismissRequest = { clientDropdownExpanded = false }
                ) {
                    clients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text("${client.fullName} - CURP: ${client.curp}") },
                            onClick = {
                                selectedClientId = client.id
                                clientDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Client Evaluation & Recommendation Support Box
            val allEvals by viewModel.allEvaluations.collectAsStateWithLifecycle()
            val latestEval = remember(allEvals, selectedClientId) {
                allEvals.filter { it.clientId == selectedClientId }
                    .maxByOrNull { it.evaluatedAt }
            }

            LaunchedEffect(selectedClientId) {
                if (selectedClientId > 0) {
                    viewModel.evaluateClientNow(selectedClientId)
                }
            }

            latestEval?.let { eval ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_loan_evaluation_summary_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Evaluación Crediticia de Apoyo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            com.example.ui.components.ScoreBadge(score = eval.score, classification = eval.classification)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.example.ui.components.RecommendationBadge(recommendation = eval.recommendation)
                            Text(
                                text = "Monto Sugerido: ${currencyFormat.format(eval.suggestedAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (eval.recommendedPlanMessage.isNotBlank()) {
                            Text(
                                text = "Nota: ${eval.recommendedPlanMessage.take(120)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Plan Selector
            Text("Plan de Crédito *", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                customPlans.forEach { plan ->
                    FilterChip(
                        selected = selectedPlanCode == plan.code,
                        onClick = { selectedPlanCode = plan.code },
                        label = { Text("${plan.name} (${(plan.rate * 100).toInt()}% / ${plan.days}d)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_plan_${plan.code.lowercase()}")
                    )
                }
            }

            // Capital Amount Input
            OutlinedTextField(
                value = capitalInput,
                onValueChange = { capitalInput = it },
                label = { Text("Monto de Capital Solicitado (MXN) *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_capital_amount"),
                singleLine = true
            )

            // Skip Sundays option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = skipSundays,
                    onCheckedChange = { skipSundays = it },
                    modifier = Modifier.testTag("checkbox_skip_sundays")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Omitir Domingos", fontWeight = FontWeight.Medium)
                    Text(
                        "Si se activa, los domingos no se generan cuotas de cobro",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Real-Time Calculation Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Resumen de Cálculo Automático",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Capital:")
                        Text(currencyFormat.format(calculation.capital), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tasa e Interés (${(calculation.interestRate * 100).toInt()}%):")
                        Text(currencyFormat.format(calculation.interestAmount), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monto Total a Pagar:")
                        Text(currencyFormat.format(calculation.totalAmount), fontWeight = FontWeight.ExtraBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pago Diario (${calculation.totalDays} Cuotas):")
                        Text(
                            currencyFormat.format(calculation.dailyPayment),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            var missingMandatoryDocs by remember { mutableStateOf<List<com.example.data.local.DocumentTypeConfigEntity>>(emptyList()) }
            var showMandatoryDialog by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    if (selectedClient == null) {
                        errorMessage = "Seleccione un cliente válido"
                    } else if (capital <= 0) {
                        errorMessage = "Ingrese un monto de capital mayor a 0"
                    } else {
                        val clientObj = selectedClient
                        coroutineScope.launch {
                            val compliance = viewModel.docRepo.checkMandatoryDocumentsCompliance(clientObj.id, isLoanRequest = true)
                            if (!compliance.isComplete) {
                                missingMandatoryDocs = compliance.missingTypes
                                showMandatoryDialog = true
                            } else {
                                viewModel.createLoan(
                                    clientId = clientObj.id,
                                    capital = capital,
                                    planType = selectedPlanCode,
                                    skipSundays = skipSundays
                                ) { loanId ->
                                    onLoanCreated(loanId)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("disburse_loan_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Aprobar y Desembolsar Préstamo", style = MaterialTheme.typography.titleMedium)
            }

            if (showMandatoryDialog && selectedClient != null) {
                com.example.ui.components.MandatoryDocsValidationDialog(
                    clientId = selectedClient.id,
                    clientName = selectedClient.fullName,
                    missingDocs = missingMandatoryDocs,
                    viewModel = viewModel,
                    onDismiss = { showMandatoryDialog = false },
                    onCapturedSuccess = {
                        // Re-check
                        val clientObj = selectedClient
                        coroutineScope.launch {
                            val comp = viewModel.docRepo.checkMandatoryDocumentsCompliance(clientObj.id, isLoanRequest = true)
                            missingMandatoryDocs = comp.missingTypes
                            if (comp.isComplete) {
                                showMandatoryDialog = false
                            }
                        }
                    }
                )
            }
        }
    }
}
