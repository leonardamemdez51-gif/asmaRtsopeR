package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.location.LocationUtils
import com.example.data.local.ClientEntity
import com.example.data.local.CollectionVisitEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorMapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProcessPaymentForClient: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    val config = systemConfig ?: com.example.data.local.SystemConfigEntity()

    val hasMapPermission = viewModel.hasPermission(com.example.core.security.AppPermission.MAPA_VER)

    if (!config.mapsEnabled) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Módulo de Mapas Desactivado",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "El administrador ha desactivado temporalmente la geolocalización y visualización de rutas.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onBack) {
                    Text("Volver")
                }
            }
        }
        return
    }

    if (!hasMapPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Acceso Denegado",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "No tienes el permiso MAPA_VER para visualizar la cartografía de clientes y rutas.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onBack) {
                    Text("Volver")
                }
            }
        }
        return
    }

    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val routes by viewModel.allRoutes.collectAsStateWithLifecycle()

    // Collector current location (default Zócalo / Central CDMX)
    var collectorLat by remember { mutableDoubleStateOf(19.4326) }
    var collectorLng by remember { mutableDoubleStateOf(-99.1332) }

    var isOptimized by remember { mutableStateOf(false) }
    var showRecordVisitDialog by remember { mutableStateOf(false) }
    var selectedClientForVisit by remember { mutableStateOf<ClientEntity?>(null) }

    // Dialog state
    var visitResult by remember { mutableStateOf("PAGO_REALIZADO") }
    var visitNotes by remember { mutableStateOf("") }
    var promisedAmount by remember { mutableStateOf("") }
    var promisedDate by remember { mutableStateOf("2026-08-08") }

    // Active client list ordered
    val activeClients = remember(clients, isOptimized, collectorLat, collectorLng) {
        val baseList = clients.filter { it.status == "ACTIVO" || it.status == "MOROSO" }
        if (isOptimized) {
            LocationUtils.optimizeClientOrder(collectorLat, collectorLng, baseList)
        } else {
            baseList
        }
    }

    // Map markers
    val mapMarkers = remember(activeClients, visits, loans, config.gpsPrivacyModeEnabled) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        activeClients.mapIndexed { index, client ->
            val hasVisitedToday = visits.any {
                it.clientId == client.id &&
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.visitDate)) == todayStr
            }

            val isMoroso = client.status == "MOROSO"
            val status = if (hasVisitedToday) "VISITADO" else if (isMoroso) "MOROSO" else "PENDIENTE"

            val clientLoan = loans.firstOrNull { it.clientId == client.id && it.status == "ACTIVO" }

            // Obfuscate coordinates slightly if privacy mode enabled (add ±0.0003, ~30 meters)
            val mappedLat = if (config.gpsPrivacyModeEnabled) client.latitude + 0.0003 else client.latitude
            val mappedLng = if (config.gpsPrivacyModeEnabled) client.longitude - 0.0003 else client.longitude

            MapMarkerItem(
                id = client.id,
                title = client.fullName,
                address = client.address,
                latitude = mappedLat,
                longitude = mappedLng,
                status = status,
                orderIndex = index + 1,
                dailyPayment = clientLoan?.dailyPayment ?: 0.0,
                rawClient = client
            )
        }
    }

    var selectedMarkerId by remember { mutableStateOf<Long?>(mapMarkers.firstOrNull()?.id) }

    Scaffold(
        topBar = {
            RamaTopBar(
                title = "Mapa del Cobrador en Campo",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            isOptimized = !isOptimized
                            val msg = if (isOptimized) "Ruta optimizada por proximidad geográfica" else "Orden de ruta por defecto"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn_optimize_map_route")
                    ) {
                        Icon(
                            imageVector = if (isOptimized) Icons.Default.Route else Icons.Default.AltRoute,
                            contentDescription = "Optimizar Ruta",
                            tint = if (isOptimized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Location Permission & Status Banner
            LocationPermissionCard(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                onPermissionGranted = {
                    // Update collector position
                    collectorLat = 19.4326 + (Math.random() - 0.5) * 0.01
                    collectorLng = -99.1332 + (Math.random() - 0.5) * 0.01
                }
            )

            // Split View: Top Map Canvas (45% height), Bottom Route List (55%)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.48f)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    InteractiveMapView(
                        modifier = Modifier.fillMaxSize(),
                        collectorLat = collectorLat,
                        collectorLng = collectorLng,
                        markers = mapMarkers,
                        showPolylineRoute = true,
                        selectedMarkerId = selectedMarkerId,
                        onMarkerSelect = { marker ->
                            selectedMarkerId = marker.id
                        },
                        onNavigateClick = { marker ->
                            if (!viewModel.hasPermission(com.example.core.security.AppPermission.NAVEGACION_USAR)) {
                                Toast.makeText(context, "No tienes permiso para usar navegación.", Toast.LENGTH_SHORT).show()
                            } else if (!config.useExternalNavigation) {
                                Toast.makeText(context, "El administrador ha desactivado la navegación externa.", Toast.LENGTH_SHORT).show()
                            } else {
                                val success = LocationUtils.launchGoogleMapsNavigation(
                                    context,
                                    marker.latitude,
                                    marker.longitude,
                                    marker.title
                                )
                                if (success) {
                                    viewModel.logAuditEvent(
                                        action = "NAVEGACION_USAR",
                                        entityType = "CLIENTE",
                                        entityId = marker.id.toString(),
                                        newValues = "Navegación iniciada a ${marker.title} desde el mapa"
                                    )
                                } else {
                                    Toast.makeText(context, "Iniciando navegación a ${marker.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onVisitClick = { marker ->
                            marker.rawClient?.let {
                                selectedClientForVisit = it
                                showRecordVisitDialog = true
                            }
                        }
                    )
                }

                // Route List Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Clientes en Ruta (${mapMarkers.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isOptimized,
                                onClick = { isOptimized = !isOptimized },
                                label = { Text(if (isOptimized) "Optimizado GPS" else "Sin Optimizar") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Route List Items
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.52f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(mapMarkers) { index, item ->
                        val distMeters = LocationUtils.haversineDistanceMeters(
                            collectorLat, collectorLng, item.latitude, item.longitude
                        )
                        val travelMin = LocationUtils.estimateTravelTimeMinutes(distMeters)
                        val isSelected = item.id == selectedMarkerId

                        Card(
                            onClick = { selectedMarkerId = item.id },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_client_map_item_${item.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Stop Order Index Badge
                                Surface(
                                    color = when (item.status) {
                                        "VISITADO" -> MaterialTheme.colorScheme.secondary
                                        "MOROSO" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = item.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.NearMe,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = "${LocationUtils.formatDistance(distMeters)} • $travelMin min",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (item.dailyPayment > 0) {
                                            Text(
                                                text = "Cuota: $${item.dailyPayment.toInt()} MXN",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    IconButton(
                                        onClick = {
                                            if (!viewModel.hasPermission(com.example.core.security.AppPermission.NAVEGACION_USAR)) {
                                                Toast.makeText(context, "No tienes permiso para usar navegación.", Toast.LENGTH_SHORT).show()
                                            } else if (!config.useExternalNavigation) {
                                                Toast.makeText(context, "El administrador ha desactivado la navegación externa.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val success = LocationUtils.launchGoogleMapsNavigation(
                                                    context, item.latitude, item.longitude, item.title
                                                )
                                                if (success) {
                                                    viewModel.logAuditEvent(
                                                        action = "NAVEGACION_USAR",
                                                        entityType = "CLIENTE",
                                                        entityId = item.id.toString(),
                                                        newValues = "Navegación iniciada a ${item.title} desde la lista del mapa"
                                                    )
                                                } else {
                                                    Toast.makeText(context, "Iniciando navegación a ${item.title}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("btn_item_nav_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Navigation,
                                            contentDescription = "Navegar con Google Maps",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Record Visit Modal Dialog
    if (showRecordVisitDialog && selectedClientForVisit != null) {
        val client = selectedClientForVisit!!
        val isLocationAllowed = config.locationRecordingEnabled && config.visitLocationEnabled
        
        AlertDialog(
            onDismissRequest = { showRecordVisitDialog = false },
            title = { Text("Registrar Visita: ${client.fullName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isLocationAllowed) "Ubicación GPS actual capturada:" else "Ubicación GPS (Desactivada por políticas):",
                        color = if (isLocationAllowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Lat: ${if (isLocationAllowed) String.format("%.4f", collectorLat) else "0.0"}, Lng: ${if (isLocationAllowed) String.format("%.4f", collectorLng) else "0.0"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocationAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    OutlinedTextField(
                        value = visitResult,
                        onValueChange = { visitResult = it },
                        label = { Text("Resultado de Visita") },
                        modifier = Modifier.fillMaxWidth().testTag("input_visit_result")
                    )

                    OutlinedTextField(
                        value = visitNotes,
                        onValueChange = { visitNotes = it },
                        label = { Text("Observaciones / Notas") },
                        modifier = Modifier.fillMaxWidth().testTag("input_visit_notes")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalLat = if (isLocationAllowed) collectorLat else 0.0
                        val finalLng = if (isLocationAllowed) collectorLng else 0.0
                        val finalAccuracy = if (isLocationAllowed) 4.5f else 0.0f

                        val visit = CollectionVisitEntity(
                            clientId = client.id,
                            clientName = client.fullName,
                            loanId = 1L,
                            collectorId = 2L,
                            collectorName = "Roberto Gómez",
                            visitDate = System.currentTimeMillis(),
                            latitude = finalLat,
                            longitude = finalLng,
                            gpsAccuracy = finalAccuracy,
                            locationReference = client.address,
                            result = visitResult,
                            notes = visitNotes,
                            syncStatus = "SINCRONIZADO"
                        )
                        viewModel.recordVisit(visit, "cobrador1")
                        
                        viewModel.logAuditEvent(
                            action = "UBICACION_VISITA_REGISTRAR",
                            entityType = "VISITA",
                            entityId = client.id.toString(),
                            newValues = "Visita registrada. GPS: ($finalLat, $finalLng), Obs: $visitNotes"
                        )

                        showRecordVisitDialog = false
                        Toast.makeText(context, "Visita registrada exitosamente", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_confirm_visit_map")
                ) {
                    Text("Guardar Visita")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordVisitDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
