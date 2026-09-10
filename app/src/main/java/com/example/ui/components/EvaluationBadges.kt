package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScoreBadge(score: Int, classification: String) {
    val scoreColor = when (classification) {
        "EXCELENTE" -> Color(0xFF2E7D32)
        "MUY BUENO" -> Color(0xFF00796B)
        "BUENO" -> Color(0xFF0288D1)
        "REGULAR" -> Color(0xFFED6C02)
        "RIESGO ALTO" -> Color(0xFFD32F2F)
        else -> Color(0xFFB71C1C)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(scoreColor.copy(alpha = 0.12f))
                .border(2.dp, scoreColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }
        
        Surface(
            color = scoreColor,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = classification,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun RecommendationBadge(recommendation: String) {
    val (bgColor, textColor) = when (recommendation) {
        "RECOMENDADO" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "RECOMENDADO_CONDICIONADO", "RECOMENDADO CONDICIONADO" -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17))
        "REQUIERE_REVISION", "REQUIERE REVISIÓN" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        else -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
    }
    
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = recommendation.replace("_", " "),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
