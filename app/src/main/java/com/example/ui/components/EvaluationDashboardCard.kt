package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel

@Composable
fun EvaluationDashboardCard(
    viewModel: MainViewModel,
    onNavigateToClients: () -> Unit
) {
    val allEvals by viewModel.allEvaluations.collectAsStateWithLifecycle()

    if (allEvals.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("evaluation_dashboard_empty_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Motor de Scoring y Riesgo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Realice la primera evaluación en el expediente de un cliente",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    // Deduplicate to latest evaluation per client
    val latestPerClient = remember(allEvals) {
        allEvals.groupBy { it.clientId }
            .mapValues { entry -> entry.value.maxByOrNull { it.evaluatedAt }!! }
            .values.toList()
    }

    val totalClients = latestPerClient.size
    val avgScore = if (totalClients > 0) latestPerClient.map { it.score }.average().toInt() else 0

    val excelenteCount = latestPerClient.count { it.classification == "EXCELENTE" }
    val muyBuenoCount = latestPerClient.count { it.classification == "MUY BUENO" }
    val buenoCount = latestPerClient.count { it.classification == "BUENO" }
    val regularCount = latestPerClient.count { it.classification == "REGULAR" }
    val riesgoAltoCount = latestPerClient.count { it.classification == "RIESGO ALTO" || it.classification == "RIESGO CRÍTICO" }

    val riesgoPct = if (totalClients > 0) (riesgoAltoCount.toDouble() / totalClients * 100).toInt() else 0
    val excelentePct = if (totalClients > 0) ((excelenteCount + muyBuenoCount).toDouble() / totalClients * 100).toInt() else 0

    val ascendenteCount = latestPerClient.count { it.trend == "ASCENDENTE" }
    val descendenteCount = latestPerClient.count { it.trend == "DESCENDENTE" }
    val ascendentePct = if (totalClients > 0) (ascendenteCount.toDouble() / totalClients * 100).toInt() else 0
    val descendentePct = if (totalClients > 0) (descendenteCount.toDouble() / totalClients * 100).toInt() else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("evaluation_dashboard_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Calificación y Riesgo de Cartera",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalClients cliente(s) evaluado(s) • Score Promedio: $avgScore / 100",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = onNavigateToClients) {
                    Text("Ver Clientes", fontSize = 12.sp)
                }
            }

            HorizontalDivider()

            // 3 KPI HIGHLIGHT BOXES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // High Risk %
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("En Riesgo Alto", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("$riesgoPct%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFD32F2F))
                        Text("$riesgoAltoCount clientes", fontSize = 10.sp, color = Color(0xFFB71C1C))
                    }
                }

                // Low Risk / Excellent %
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Score Excelente", fontSize = 10.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("$excelentePct%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                        Text("${excelenteCount + muyBuenoCount} clientes", fontSize = 10.sp, color = Color(0xFF1B5E20))
                    }
                }

                // Trend Ascending %
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0F7FA),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Tendencia Ascendente", fontSize = 10.sp, color = Color(0xFF006064), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("$ascendentePct%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF00838F))
                        Text("$ascendenteCount mejorando", fontSize = 10.sp, color = Color(0xFF006064))
                    }
                }
            }

            // SCORE DISTRIBUTION VISUAL BAR
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Distribución de Cartera por Clasificación", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (descendenteCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("$descendentePct% cayendo", fontSize = 10.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Progress segmented bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (excelenteCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(excelenteCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF2E7D32))
                        )
                    }
                    if (muyBuenoCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(muyBuenoCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF00796B))
                        )
                    }
                    if (buenoCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(buenoCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF0288D1))
                        )
                    }
                    if (regularCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(regularCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFFED6C02))
                        )
                    }
                    if (riesgoAltoCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(riesgoAltoCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFFD32F2F))
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem(label = "Exc/MB ($excelentePct%)", color = Color(0xFF2E7D32))
                    LegendItem(label = "Bueno", color = Color(0xFF0288D1))
                    LegendItem(label = "Regular", color = Color(0xFFED6C02))
                    LegendItem(label = "Alto Riesgo ($riesgoPct%)", color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
