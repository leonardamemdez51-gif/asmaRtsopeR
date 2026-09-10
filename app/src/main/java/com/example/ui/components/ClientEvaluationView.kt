package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.security.AppPermission
import com.example.data.local.ClientEvaluationEntity
import com.example.data.local.ManualScoreAdjustmentEntity
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientEvaluationView(
    latestEvaluation: ClientEvaluationEntity?,
    evaluationsHistory: List<ClientEvaluationEntity>,
    manualAdjustments: List<ManualScoreAdjustmentEntity>,
    canAdjustManually: Boolean,
    onRecalculateScore: () -> Unit,
    onManualAdjustmentRequested: (newScore: Int, reason: String, observation: String) -> Unit
) {
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val currentScore = latestEvaluation?.score ?: 80
    val classification = latestEvaluation?.classification ?: "MUY BUENO"
    val trend = latestEvaluation?.trend ?: "ESTABLE"
    val recommendation = latestEvaluation?.recommendation ?: "RECOMENDADO"

    val scoreColor = when (classification) {
        "EXCELENTE" -> Color(0xFF2E7D32)
        "MUY BUENO" -> Color(0xFF00796B)
        "BUENO" -> Color(0xFF0288D1)
        "REGULAR" -> Color(0xFFED6C02)
        "RIESGO ALTO" -> Color(0xFFD32F2F)
        else -> Color(0xFFB71C1C)
    }

    val recommendationBgColor = when (recommendation) {
        "RECOMENDADO" -> Color(0xFFE8F5E9)
        "RECOMENDADO_CONDICIONADO", "RECOMENDADO CONDICIONADO" -> Color(0xFFFFF8E1)
        "REQUIERE_REVISION", "REQUIERE REVISIÓN" -> Color(0xFFFFF3E0)
        else -> Color(0xFFFFEBEE)
    }

    val recommendationTextColor = when (recommendation) {
        "RECOMENDADO" -> Color(0xFF2E7D32)
        "RECOMENDADO_CONDICIONADO", "RECOMENDADO CONDICIONADO" -> Color(0xFFF57F17)
        "REQUIERE_REVISION", "REQUIERE REVISIÓN" -> Color(0xFFE65100)
        else -> Color(0xFFC62828)
    }

    val positiveFactors = remember(latestEvaluation) {
        try {
            val jsonArray = JSONArray(latestEvaluation?.positiveFactorsJson ?: "[]")
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) list.add(jsonArray.getString(i))
            if (list.isEmpty()) listOf("+ Historial sin mora activa") else list
        } catch (_: Exception) {
            listOf("+ Historial general positivo")
        }
    }

    val negativeFactors = remember(latestEvaluation) {
        try {
            val jsonArray = JSONArray(latestEvaluation?.negativeFactorsJson ?: "[]")
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) list.add(jsonArray.getString(i))
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    val reasons = remember(latestEvaluation) {
        try {
            val jsonArray = JSONArray(latestEvaluation?.reasonsJson ?: "[]")
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) list.add(jsonArray.getString(i))
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    val alerts = remember(latestEvaluation) {
        try {
            val jsonArray = JSONArray(latestEvaluation?.alertsJson ?: "[]")
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) list.add(jsonArray.getString(i))
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP SCORE BANNER CARD
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("score_summary_card"),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Score & Evaluación Crediticia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onRecalculateScore,
                            modifier = Modifier.testTag("btn_recalculate_score")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recalcular Score", tint = MaterialTheme.colorScheme.primary)
                        }
                        if (canAdjustManually) {
                            FilledTonalButton(
                                onClick = { showAdjustmentDialog = true },
                                modifier = Modifier.testTag("btn_manual_adjust_score"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ajustar", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Score Circle Badge
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.12f))
                        .border(4.dp, scoreColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$currentScore",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = scoreColor
                        )
                        Text(
                            text = "de 100",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = scoreColor,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = classification,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // Trend Indicator
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val trendIcon = when (trend) {
                                "ASCENDENTE" -> Icons.Default.TrendingUp
                                "DESCENDENTE" -> Icons.Default.TrendingDown
                                else -> Icons.Default.TrendingFlat
                            }
                            val trendColor = when (trend) {
                                "ASCENDENTE" -> Color(0xFF2E7D32)
                                "DESCENDENTE" -> Color(0xFFD32F2F)
                                else -> Color.Gray
                            }
                            Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(16.dp))
                            Text(trend, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = trendColor)
                        }
                    }
                }

                if (latestEvaluation?.isManualAdjustment == true) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Ajustado manualmente: ${latestEvaluation.manualAdjustmentReason}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // RECOMMENDATION & SUGGESTED AMOUNTS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recommendation_card"),
            colors = CardDefaults.cardColors(containerColor = recommendationBgColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = recommendationTextColor
                    )
                    Text(
                        text = "RECOMENDACIÓN: ${recommendation.replace("_", " ")}",
                        fontWeight = FontWeight.ExtraBold,
                        color = recommendationTextColor,
                        fontSize = 15.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monto Sugerido:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = currencyFormat.format(latestEvaluation?.suggestedAmount ?: 5000.0),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rango Recomendado:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${currencyFormat.format(latestEvaluation?.minRecommendedAmount ?: 1000.0)} - ${currencyFormat.format(latestEvaluation?.maxRecommendedAmount ?: 10000.0)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                HorizontalDivider(color = recommendationTextColor.copy(alpha = 0.2f))

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = recommendationTextColor)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = latestEvaluation?.recommendedPlanMessage.takeIf { !it.isNullOrBlank() }
                            ?: "Plan Recomendado: ${latestEvaluation?.recommendedPlanCode ?: "PLAN_20_DIAS"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Nota: La recomendación es una herramienta de apoyo y no aprueba ni ejecuta el préstamo automáticamente.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // POSITIVE & NEGATIVE FACTORS (TRANSPARENCIA DEL SCORE)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Transparencia de Factores Evaluados",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                // Positive Factors
                Text(
                    text = "Factores Positivos (+)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                Spacer(Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    positiveFactors.forEach { factor ->
                        AssistChip(
                            onClick = {},
                            label = { Text(factor, fontSize = 11.sp, color = Color(0xFF1B5E20)) },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFE8F5E9))
                        )
                    }
                }

                if (negativeFactors.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Factores Negativos (-)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )

                    Spacer(Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        negativeFactors.forEach { factor ->
                            AssistChip(
                                onClick = {},
                                label = { Text(factor, fontSize = 11.sp, color = Color(0xFFB71C1C)) },
                                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp)) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFEBEE))
                            )
                        }
                    }
                }
            }
        }

        // ALERTS & RISKS (IF ANY)
        if (alerts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFE65100))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Alertas Detectadas (${alerts.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    alerts.forEach { alert ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(8.dp), tint = Color(0xFFE65100))
                            Spacer(Modifier.width(6.dp))
                            Text(alert, fontSize = 12.sp, color = Color(0xFF424242))
                        }
                    }
                }
            }
        }

        // EXPLICABILITY DETAILS
        if (reasons.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Explicabilidad y Criterios del Algoritmo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // HISTORIAL GRÁFICO / TIMELINE DE EVALUACIONES
        if (evaluationsHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Historial y Tendencia de Score (${evaluationsHistory.size} registros)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    // Simple Custom Canvas Line Chart for Score History
                    val points = remember(evaluationsHistory) {
                        evaluationsHistory.take(10).reversed()
                    }

                    if (points.size >= 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val maxScore = 100f
                                val minScore = 0f

                                val stepX = width / (points.size - 1)
                                val path = Path()

                                points.forEachIndexed { index, item ->
                                    val x = index * stepX
                                    val normalizedY = (item.score - minScore) / (maxScore - minScore)
                                    val y = height - (normalizedY * height)

                                    if (index == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }

                                    drawCircle(
                                        color = if (item.score >= 80) Color(0xFF2E7D32) else if (item.score >= 60) Color(0xFFED6C02) else Color(0xFFD32F2F),
                                        radius = 5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = Color(0xFF006874),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // History List
                    evaluationsHistory.take(5).forEach { eval ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Score: ${eval.score} (${eval.classification})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${dateFormat.format(Date(eval.evaluatedAt))} • Por: ${eval.evaluatedByUsername}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (eval.isManualAdjustment) {
                                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text("Ajuste Manual", fontSize = 10.sp)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // MANUAL ADJUSTMENT DIALOG
    if (showAdjustmentDialog) {
        var newScoreText by remember { mutableStateOf(currentScore.toString()) }
        var reasonText by remember { mutableStateOf("") }
        var observationText by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAdjustmentDialog = false },
            title = { Text("Ajuste Manual de Score") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Solo administradores o supervisores autorizados pueden realizar ajustes manuales. Este evento se registrará en la auditoría del sistema.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newScoreText,
                        onValueChange = { newScoreText = it },
                        label = { Text("Nuevo Score (0 - 100)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_manual_score"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Motivo Obligatorio *") },
                        placeholder = { Text("Ej. Verificación presencial de garantía o acuerdo especial") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_manual_reason"),
                        isError = errorMessage != null
                    )

                    OutlinedTextField(
                        value = observationText,
                        onValueChange = { observationText = it },
                        label = { Text("Observaciones adicionales") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val scoreVal = newScoreText.toIntOrNull()
                        if (scoreVal == null || scoreVal !in 0..100) {
                            errorMessage = "Ingrese un número válido entre 0 y 100"
                            return@Button
                        }
                        if (reasonText.isBlank()) {
                            errorMessage = "El motivo del ajuste es estrictamente obligatorio"
                            return@Button
                        }
                        onManualAdjustmentRequested(scoreVal, reasonText.trim(), observationText.trim())
                        showAdjustmentDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_manual_score")
                ) {
                    Text("Guardar Ajuste")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAdjustmentDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
