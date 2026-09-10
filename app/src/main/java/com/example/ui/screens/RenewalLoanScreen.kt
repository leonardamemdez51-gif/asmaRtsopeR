package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.LoanEntity
import com.example.domain.LoanCalculator
import com.example.domain.PlanType
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RenewalLoanScreen(
    loanId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLoanRenewed: (Long) -> Unit
) {
    val customPlans by viewModel.customLoanPlans.collectAsStateWithLifecycle()

    var previousLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var selectedPlanCode by remember { mutableStateOf("PLAN_20_DIAS") }
    var capitalInput by remember { mutableStateOf("5000") }
    var extraCapitalInput by remember { mutableStateOf("0") }
    var skipSundays by remember { mutableStateOf(true) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    LaunchedEffect(loanId) {
        previousLoan = viewModel.loanRepo.getLoanById(loanId)
    }

    val prev = previousLoan
    val baseCapital = capitalInput.toDoubleOrNull() ?: 0.0
    val extraCapital = extraCapitalInput.toDoubleOrNull() ?: 0.0
    val totalCapital = baseCapital + extraCapital

    val calculation = remember(totalCapital, selectedPlanCode, customPlans) {
        val plan = customPlans.find { it.code == selectedPlanCode }
        if (plan != null) {
            LoanCalculator.calculateLoan(totalCapital, plan.rate, plan.days)
        } else {
            val pType = if (selectedPlanCode == "PLAN_30_DIAS") PlanType.PLAN_30_DIAS else PlanType.PLAN_20_DIAS
            LoanCalculator.calculateLoan(totalCapital, pType)
        }
    }

    val netToDisburse = (totalCapital - (prev?.remainingBalance ?: 0.0)).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            RamaTopBar(title = "Renovación de Préstamo", onBackClick = onBack)
        }
    ) { padding ->
        if (prev == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Préstamo Anterior #${prev.id}", fontWeight = FontWeight.Bold)
                        Text("Cliente: ${prev.clientName}")
                        Text("Saldo Restante Pendiente: ${currencyFormat.format(prev.remainingBalance)}")
                    }
                }

                // Client Evaluation & Recommendation Support Box
                val allEvals by viewModel.allEvaluations.collectAsStateWithLifecycle()
                val latestEval = remember(allEvals, prev.clientId) {
                    allEvals.filter { it.clientId == prev.clientId }
                        .maxByOrNull { it.evaluatedAt }
                }

                LaunchedEffect(prev.clientId) {
                    if (prev.clientId > 0) {
                        viewModel.evaluateClientNow(prev.clientId)
                    }
                }

                latestEval?.let { eval ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("renewal_loan_evaluation_summary_card"),
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
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Evaluación para Renovación",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                com.example.ui.components.ScoreBadge(score = eval.score, classification = eval.classification)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                com.example.ui.components.RecommendationBadge(recommendation = eval.recommendation)
                                Text(
                                    text = "Capacidad Máx: ${currencyFormat.format(eval.maxRecommendedAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text("Plan de Renovación", fontWeight = FontWeight.Bold)

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
                                .testTag("chip_renew_plan_${plan.code.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = capitalInput,
                    onValueChange = { capitalInput = it },
                    label = { Text("Monto de Capital Base (MXN)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_renew_capital"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = extraCapitalInput,
                    onValueChange = { extraCapitalInput = it },
                    label = { Text("Monto Adicional / Nuevo Préstamo Extra (MXN)") },
                    supportingText = { Text("Opcional: Si el cliente solicita capital adicional en la misma renovación") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_renew_extra_capital"),
                    singleLine = true
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumen de Renovación + Crédito", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Capital Total Nuevo:")
                            Text(currencyFormat.format(calculation.capital), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Menos Saldo Anterior Liquidador:")
                            Text("- ${currencyFormat.format(prev.remainingBalance)}", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Efectivo Neto a Entregar:")
                            Text(currencyFormat.format(netToDisburse), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nuevo Pago Diario:")
                            Text(currencyFormat.format(calculation.dailyPayment), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.renewLoan(
                            clientId = prev.clientId,
                            previousLoanId = prev.id,
                            capital = totalCapital,
                            extraCapital = extraCapital,
                            planType = selectedPlanCode,
                            skipSundays = skipSundays
                        ) { newLoanId ->
                            onLoanRenewed(newLoanId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_renew_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirmar y Desembolsar Renovación", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
