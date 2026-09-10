package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.RamaStatusBadge
import com.example.ui.components.RamaTopBar
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LoanListScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val activeCash by viewModel.activeCashRegister.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("TODOS") }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    val filterOptions = listOf("TODOS", "PENDIENTES", "AUTORIZADOS", "ACTIVOS", "LIQUIDADOS", "RENOVADOS", "CANCELADOS")

    val filteredLoans = remember(loans, selectedFilter) {
        when (selectedFilter) {
            "PENDIENTES" -> loans.filter { it.status == "PENDIENTE" }
            "AUTORIZADOS" -> loans.filter { it.status == "AUTORIZADO" }
            "ACTIVOS" -> loans.filter { it.status == "ACTIVO" }
            "LIQUIDADOS" -> loans.filter { it.status == "LIQUIDADO" }
            "RENOVADOS" -> loans.filter { it.status == "RENOVADO" }
            "CANCELADOS" -> loans.filter { it.status == "CANCELADO" }
            else -> loans
        }
    }

    Scaffold(
        topBar = {
            RamaTopBar(title = "Préstamos", subtitle = "${filteredLoans.size} Préstamos Encontrados")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate("new_loan") },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_loan")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Préstamo")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = filterOptions.indexOf(selectedFilter).coerceAtLeast(0),
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                filterOptions.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = { Text(filter) },
                        modifier = Modifier.testTag("tab_loan_$filter")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredLoans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay préstamos en esta categoría.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredLoans) { loan ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("loan_detail/${loan.id}") }
                                .testTag("loan_card_${loan.id}"),
                            shape = RoundedCornerShape(12.dp),
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
                                            text = loan.clientName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Préstamo #${loan.id} • ${if (loan.planType == "PLAN_20_DIAS") "20% en 20 Días" else "30% en 30 Días"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RamaStatusBadge(status = loan.status)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Capital", style = MaterialTheme.typography.labelSmall)
                                        Text(currencyFormat.format(loan.capital), fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Pago Diario", style = MaterialTheme.typography.labelSmall)
                                        Text(currencyFormat.format(loan.dailyPayment), fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Saldo Restante", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            currencyFormat.format(loan.remainingBalance),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (loan.status == "ACTIVO") {
                                        OutlinedButton(
                                            onClick = { onNavigate("renewal_loan/${loan.id}") },
                                            modifier = Modifier.padding(end = 8.dp).testTag("btn_renew_${loan.id}")
                                        ) {
                                            Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Renovar", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { onNavigate("process_payment/${loan.id}") },
                                            enabled = activeCash != null && activeCash?.status == "ABIERTA",
                                            modifier = Modifier.testTag("btn_pay_${loan.id}")
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Abonar", fontSize = 12.sp)
                                        }
                                    } else {
                                        TextButton(onClick = { onNavigate("loan_detail/${loan.id}") }) {
                                            Text("Ver Calendario")
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
