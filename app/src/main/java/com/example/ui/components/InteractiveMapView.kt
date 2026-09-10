package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientEntity
import com.example.data.local.CollectionVisitEntity
import kotlin.math.*

data class MapMarkerItem(
    val id: Long,
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: String, // "PENDIENTE", "PAGADO", "MOROSO", "VISITADO", "PRIORIDAD"
    val orderIndex: Int = 0,
    val dailyPayment: Double = 0.0,
    val rawClient: ClientEntity? = null
)

@Composable
fun InteractiveMapView(
    modifier: Modifier = Modifier,
    collectorLat: Double = 19.4326,
    collectorLng: Double = -99.1332,
    collectorName: String = "Cobrador Principal",
    markers: List<MapMarkerItem> = emptyList(),
    showPolylineRoute: Boolean = true,
    selectedMarkerId: Long? = null,
    onMarkerSelect: (MapMarkerItem) -> Unit = {},
    onNavigateClick: (MapMarkerItem) -> Unit = {},
    onVisitClick: (MapMarkerItem) -> Unit = {}
) {
    // Zoom & Pan state
    var zoomLevel by remember { mutableStateOf(14.0f) }
    var centerLat by remember { mutableStateOf(collectorLat) }
    var centerLng by remember { mutableStateOf(collectorLng) }

    // Keep center synced if collector position changes significantly
    LaunchedEffect(collectorLat, collectorLng) {
        if (abs(centerLat - collectorLat) > 0.05 || abs(centerLng - collectorLng) > 0.05) {
            centerLat = collectorLat
            centerLng = collectorLng
        }
    }

    var activeSelectedMarker by remember {
        mutableStateOf(markers.firstOrNull { it.id == selectedMarkerId } ?: markers.firstOrNull())
    }

    LaunchedEffect(selectedMarkerId, markers) {
        markers.firstOrNull { it.id == selectedMarkerId }?.let {
            activeSelectedMarker = it
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .background(Color(0xFFEFEBE7)) // Light map base canvas
    ) {
        // Map Canvas Drawing
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_canvas")
                .pointerInput(centerLat, centerLng, zoomLevel, markers) {
                    detectTapGestures { tapOffset ->
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        val scale = (1 shl zoomLevel.toInt()) * 12.0f

                        // Find closest marker to tap
                        var closest: MapMarkerItem? = null
                        var minDistanceSq = 2500f // 50px radius

                        for (marker in markers) {
                            val x = width / 2f + ((marker.longitude - centerLng) * scale).toFloat()
                            val y = height / 2f - ((marker.latitude - centerLat) * scale).toFloat()
                            val dx = tapOffset.x - x
                            val dy = tapOffset.y - y
                            val distSq = dx * dx + dy * dy
                            if (distSq < minDistanceSq) {
                                minDistanceSq = distSq
                                closest = marker
                            }
                        }

                        if (closest != null) {
                            activeSelectedMarker = closest
                            onMarkerSelect(closest)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val scale = (1 shl zoomLevel.toInt()) * 12.0f

            // 1. Draw Map Grid / Blocks
            val gridStep = 80f
            for (x in 0..(width / gridStep).toInt()) {
                drawLine(
                    color = Color(0xFFE2DCD5),
                    start = Offset(x * gridStep, 0f),
                    end = Offset(x * gridStep, height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(height / gridStep).toInt()) {
                drawLine(
                    color = Color(0xFFE2DCD5),
                    start = Offset(0f, y * gridStep),
                    end = Offset(width, y * gridStep),
                    strokeWidth = 1f
                )
            }

            // Draw Simulated Main Avenues & Green Zones
            drawRect(
                color = Color(0xFFD4E8D4),
                topLeft = Offset(width * 0.1f, height * 0.15f),
                size = Size(width * 0.25f, height * 0.2f)
            )

            drawLine(
                color = Color(0xFFFFFFFF),
                start = Offset(0f, height * 0.45f),
                end = Offset(width, height * 0.45f),
                strokeWidth = 12f
            )
            drawLine(
                color = Color(0xFFFFFFFF),
                start = Offset(width * 0.5f, 0f),
                end = Offset(width * 0.5f, height),
                strokeWidth = 12f
            )

            // 2. Draw Polyline Route between ordered markers
            if (showPolylineRoute && markers.isNotEmpty()) {
                val sortedRoute = markers.sortedBy { if (it.orderIndex > 0) it.orderIndex else Int.MAX_VALUE }
                val path = Path()

                // Start from collector position
                val startX = width / 2f + ((collectorLng - centerLng) * scale).toFloat()
                val startY = height / 2f - ((collectorLat - centerLat) * scale).toFloat()
                path.moveTo(startX, startY)

                sortedRoute.forEach { marker ->
                    val mx = width / 2f + ((marker.longitude - centerLng) * scale).toFloat()
                    val my = height / 2f - ((marker.latitude - centerLat) * scale).toFloat()
                    path.lineTo(mx, my)
                }

                drawPath(
                    path = path,
                    color = Color(0xFF006874),
                    style = Stroke(
                        width = 6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                )
            }

            // 3. Draw Collector Position Marker (Blue pulse circle)
            val collX = width / 2f + ((collectorLng - centerLng) * scale).toFloat()
            val collY = height / 2f - ((collectorLat - centerLat) * scale).toFloat()

            // Accuracy halo
            drawCircle(
                color = Color(0x33006874),
                radius = 32f,
                center = Offset(collX, collY)
            )
            drawCircle(
                color = Color(0xFF006874),
                radius = 12f,
                center = Offset(collX, collY)
            )
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(collX, collY)
            )

            // 4. Draw Client Markers
            markers.forEach { marker ->
                val mx = width / 2f + ((marker.longitude - centerLng) * scale).toFloat()
                val my = height / 2f - ((marker.latitude - centerLat) * scale).toFloat()

                val markerColor = when (marker.status) {
                    "PAGADO", "VISITADO" -> Color(0xFF2E7D32) // Green
                    "MOROSO" -> Color(0xFFC62828) // Red
                    "PRIORIDAD" -> Color(0xFF6A1B9A) // Purple
                    else -> Color(0xFFEF6C00) // Orange (Pending)
                }

                val isSelected = activeSelectedMarker?.id == marker.id
                val radius = if (isSelected) 22f else 16f

                // Outer pin ring
                drawCircle(
                    color = if (isSelected) Color.Black else Color.White,
                    radius = radius + 3f,
                    center = Offset(mx, my)
                )
                drawCircle(
                    color = markerColor,
                    radius = radius,
                    center = Offset(mx, my)
                )
            }
        }

        // Map Controls Overlay (Top Right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { zoomLevel = (zoomLevel + 0.5f).coerceAtMost(18.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("btn_zoom_in")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom +")
            }
            SmallFloatingActionButton(
                onClick = { zoomLevel = (zoomLevel - 0.5f).coerceAtLeast(10.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("btn_zoom_out")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom -")
            }
            SmallFloatingActionButton(
                onClick = {
                    centerLat = collectorLat
                    centerLng = collectorLng
                },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("btn_center_collector")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Mi Ubicación", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Top Status Header Banner
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Mapa Interactivo Google GPS • ${markers.size} Clientes",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Legend Overlay (Bottom Left)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = Color(0xFFEF6C00), label = "Pendiente")
                LegendDot(color = Color(0xFFC62828), label = "Moroso")
                LegendDot(color = Color(0xFF2E7D32), label = "Visitado")
            }
        }

        // Selected Client Quick Details Bottom Sheet Banner
        activeSelectedMarker?.let { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (item.orderIndex > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "#${item.orderIndex}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = item.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.dailyPayment > 0) {
                            Text(
                                text = "Cuota diaria: $${String.format("%.2f", item.dailyPayment)} MXN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onNavigateClick(item) },
                            modifier = Modifier.testTag("btn_navigate_client_map")
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Navegar", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onVisitClick(item) },
                            modifier = Modifier.testTag("btn_visit_client_map")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Visita", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
    }
}
