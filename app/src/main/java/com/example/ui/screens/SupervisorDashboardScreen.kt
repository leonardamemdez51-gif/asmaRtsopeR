package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.components.RamaStatCard
import com.example.ui.components.RamaStatusBadge
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SupervisorDashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val state by viewModel.supervisorDashboardState.collectAsStateWithLifecycle()
    val user = session.user

    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Resumen & KPIs, 1: Productividad & Ranking, 2: Ruta del Día, 3: Gráficos, 4: Alertas

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("es").setRegion("MX").build()) }
    val kpis = state.kpis
    val filter = state.filter

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.rama_app_icon_1785869635876),
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Dashboard Supervisión",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF2563EB),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "SUPERVISOR",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Equipo a cargo • ${state.supervisorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigate("collector_map") },
                        modifier = Modifier.testTag("action_collector_map_supervisor")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Mapa del Cobrador", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { onNavigate("route_management") },
                        modifier = Modifier.testTag("action_route_mgmt_supervisor")
                    ) {
                        Icon(Icons.Default.Route, contentDescription = "Gestión de Rutas", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("action_export_supervisor")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar Reporte", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.testTag("action_filter_supervisor")
                    ) {
                        BadgedBox(
                            badge = {
                                if (filter.route != "TODAS" || filter.collectorName != "TODOS" || filter.zone != "TODAS" || filter.dateRange != "HOY") {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtros Supervisión")
                        }
                    }
                    IconButton(
                        onClick = { onNavigate("reports") },
                        modifier = Modifier.testTag("action_reports_supervisor")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = "Reportes Avanzados")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner: Efficiency Gauge & Export Quick Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Efectividad Global de Cobranza",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", kpis.efficiencyPercentage),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Meta del periodo: 85.0% • Recaudado: ${currencyFormat.format(kpis.realizedCollection)} de ${currencyFormat.format(kpis.expectedCollection)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Button(
                                onClick = { showExportDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("btn_export_report_top")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar for collection efficiency
                        LinearProgressIndicator(
                            progress = { (kpis.efficiencyPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (kpis.efficiencyPercentage >= 80) Color(0xFF16A34A) else Color(0xFFEA580C),
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Filter Summary Bar
            item {
                com.example.ui.components.EvaluationDashboardCard(
                    viewModel = viewModel,
                    onNavigateToClients = { onNavigate("clients") }
                )
            }

            // Filter Summary Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Filtros Activos de Supervisión", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            TextButton(onClick = { showFilterDialog = true }) {
                                Text("Ajustar")
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = { showFilterDialog = true },
                                label = { Text("Periodo: ${filter.dateRange}") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = filter.route != "TODAS",
                                onClick = { showFilterDialog = true },
                                label = { Text("Ruta: ${filter.route}") },
                                leadingIcon = { Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = filter.collectorName != "TODOS",
                                onClick = { showFilterDialog = true },
                                label = { Text("Cobrador: ${filter.collectorName}") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = filter.zone != "TODAS",
                                onClick = { showFilterDialog = true },
                                label = { Text("Zona: ${filter.zone}") },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
            }

            // Tab Navigation for Supervisor Sections
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Resumen KPIs") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Ranking Cobradores") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Ruta del Día") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Gráficos") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Alertas (${state.alerts.size})") }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: RESUMEN Y KPIS DE SUPERVISIÓN
                    item {
                        Text("Cobranza & Recuperación del Equipo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SupervisorKpiCard(
                                title = "Cobranza Esperada",
                                value = currencyFormat.format(kpis.expectedCollection),
                                subtitle = "Meta programada",
                                icon = Icons.Default.PriceCheck,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                            SupervisorKpiCard(
                                title = "Cobranza Realizada",
                                value = currencyFormat.format(kpis.realizedCollection),
                                subtitle = "Ingresado por cobradores",
                                icon = Icons.Default.Payments,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SupervisorKpiCard(
                                title = "Efectivo",
                                value = currencyFormat.format(kpis.cashCollection),
                                subtitle = "Cobrado en efectivo",
                                icon = Icons.Default.Money,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                            SupervisorKpiCard(
                                title = "Transferencias",
                                value = currencyFormat.format(kpis.transferCollection),
                                subtitle = "Cobrado vía electrónica",
                                icon = Icons.Default.AccountBalance,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SupervisorKpiCard(
                                title = "Cobranza Pendiente",
                                value = currencyFormat.format(kpis.pendingCollection),
                                subtitle = "Por recaudar hoy",
                                icon = Icons.Default.HourglassEmpty,
                                color = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                            SupervisorKpiCard(
                                title = "Mora Vigente",
                                value = currencyFormat.format(kpis.moraAmount),
                                subtitle = "Mora generada",
                                icon = Icons.Default.Gavel,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Text("Cartera y Préstamos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SupervisorKpiCard(
                                title = "Cartera Asignada",
                                value = currencyFormat.format(kpis.totalAssignedPortfolio),
                                subtitle = "Monto pendiente total",
                                icon = Icons.Default.AccountBalanceWallet,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.weight(1f)
                            )
                            SupervisorKpiCard(
                                title = "Cartera Vencida",
                                value = currencyFormat.format(kpis.overdueAmount),
                                subtitle = "${kpis.overdueLoansCount} préstamos vencidos",
                                icon = Icons.Default.Warning,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Text("Desempeño & Clientes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SupervisorKpiCard(
                                title = "Clientes",
                                value = kpis.totalClientsCount.toString(),
                                subtitle = "Bajo supervisión",
                                icon = Icons.Default.People,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                            SupervisorKpiCard(
                                title = "Préstamos Activos",
                                value = kpis.totalActiveLoans.toString(),
                                subtitle = "Operaciones en curso",
                                icon = Icons.Default.Assignment,
                                color = Color(0xFFCA8A04),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SupervisorKpiCard(
                                title = "Clientes en Mora",
                                value = kpis.overdueClientsCount.toString(),
                                subtitle = "En supervisión activa",
                                icon = Icons.Default.PersonOff,
                                color = Color(0xFFEA580C),
                                modifier = Modifier.weight(1f)
                            )
                            SupervisorKpiCard(
                                title = "Renovaciones",
                                value = kpis.renewalsCount.toString(),
                                subtitle = "Créditos gestionados",
                                icon = Icons.Default.Autorenew,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: PRODUCTIVIDAD Y RANKING DE COBRADORES
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ranking de Productividad por Cobrador", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "${state.ranking.size} Cobradores",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(state.ranking) { collector ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = when (collector.rankingPosition) {
                                                1 -> Color(0xFFEAB308) // Gold
                                                2 -> Color(0xFF94A3B8) // Silver
                                                3 -> Color(0xFFD97706) // Bronze
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${collector.rankingPosition}",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (collector.rankingPosition <= 3) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = collector.collectorName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${collector.route} • ${collector.zone}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (collector.efficiencyPercentage >= 80) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f%% Eficacia", collector.efficiencyPercentage),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (collector.efficiencyPercentage >= 80) Color(0xFF166534) else Color(0xFF92400E)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Esperado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(currencyFormat.format(collector.expectedAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column {
                                        Text("Cobrado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(currencyFormat.format(collector.realizedAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)))
                                    }
                                    Column {
                                        Text("Pendiente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(currencyFormat.format(collector.pendingAmount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD97706)))
                                    }
                                    Column {
                                        Text("Préstamos/Mora", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${collector.activeLoansCount} / ${collector.overdueLoansCount}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { (collector.efficiencyPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (collector.efficiencyPercentage >= 80) Color(0xFF16A34A) else Color(0xFFD97706)
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: RUTA DEL DÍA Y AVANCE DE VISITAS
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Avance General de Rutas de Hoy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Clientes Programados", style = MaterialTheme.typography.labelSmall)
                                        Text("${kpis.totalRouteClients} Clientes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column {
                                        Text("Visitados & Cobrados", style = MaterialTheme.typography.labelSmall)
                                        Text("${kpis.visitedClientsCount} (${if (kpis.totalRouteClients > 0) String.format(Locale.getDefault(), "%.1f", (kpis.visitedClientsCount.toDouble()/kpis.totalRouteClients)*100) else "0"}%)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)))
                                    }
                                    Column {
                                        Text("Pendientes de Visita", style = MaterialTheme.typography.labelSmall)
                                        Text("${kpis.pendingRouteClientsCount} Clientes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Detalle de Avance por Cobrador", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    items(state.collectorsProductivity) { col ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(col.collectorName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${col.route} • ${col.visitedCount} visitados de ${col.visitedCount + col.pendingVisitsCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${col.visitedCount}/${col.visitedCount + col.pendingVisitsCount}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    IconButton(onClick = { onNavigate("collection_visits") }) {
                                        Icon(Icons.Default.Map, contentDescription = "Ver Mapa de Ruta")
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: GRÁFICOS DINÁMICOS
                    item {
                        Text("Gráficos Dinámicos de Supervisión", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    item {
                        SupervisorGroupedBarChartCard(
                            title = "Cobranza Esperada vs Realizada por Cobrador",
                            subtitle = "Comparativa en MXN por agente",
                            data = state.chartCollectionByCollector,
                            currencyFormat = currencyFormat
                        )
                    }

                    item {
                        BarChartCard(
                            title = "Efectividad Promedio por Ruta (%)",
                            subtitle = "Porcentaje de recuperación en cada ruta geográfica",
                            data = state.chartEfficiencyByRoute,
                            currencyFormat = currencyFormat,
                            barColor = Color(0xFF0D9488)
                        )
                    }

                    item {
                        LineChartCard(
                            title = "Evolución Diaria de Recaudación",
                            subtitle = "Cobro acumulado durante la semana",
                            data = state.chartDailyProgress,
                            currencyFormat = currencyFormat,
                            lineColor = Color(0xFF2563EB)
                        )
                    }
                }

                4 -> {
                    // TAB 4: ALERTAS DE SUPERVISIÓN
                    item {
                        Text("Alertas y Desviaciones Operativas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    items(state.alerts) { alert ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (alert.severity) {
                                    "CRITICAL" -> Color(0xFFFEF2F2)
                                    "WARNING" -> Color(0xFFFFFBEB)
                                    else -> Color(0xFFF0FDF4)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (alert.severity) {
                                        "CRITICAL" -> Icons.Default.Error
                                        "WARNING" -> Icons.Default.Warning
                                        else -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = when (alert.severity) {
                                        "CRITICAL" -> Color(0xFFDC2626)
                                        "WARNING" -> Color(0xFFD97706)
                                        else -> Color(0xFF16A34A)
                                    },
                                    modifier = Modifier.size(28.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when (alert.severity) {
                                                "CRITICAL" -> Color(0xFF991B1B)
                                                "WARNING" -> Color(0xFF92400E)
                                                else -> Color(0xFF166534)
                                            }
                                        )
                                    )
                                    Text(
                                        text = alert.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // FILTER DIALOG
    if (showFilterDialog) {
        var tempRange by remember { mutableStateOf(filter.dateRange) }
        var tempRoute by remember { mutableStateOf(filter.route) }
        var tempCollector by remember { mutableStateOf(filter.collectorName) }
        var tempZone by remember { mutableStateOf(filter.zone) }

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Filtros de Supervisión")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Filtrar datos por cobrador a cargo, ruta o zona:", style = MaterialTheme.typography.bodySmall)

                    Column {
                        Text("Periodo", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("HOY", "SEMANA", "MES", "ANIO", "TODO").forEach { range ->
                                FilterChip(
                                    selected = tempRange == range,
                                    onClick = { tempRange = range },
                                    label = { Text(range) }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Cobrador", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.availableCollectors.forEach { col ->
                                FilterChip(
                                    selected = tempCollector == col,
                                    onClick = { tempCollector = col },
                                    label = { Text(col) }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Ruta Operativa", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.availableRoutes.forEach { r ->
                                FilterChip(
                                    selected = tempRoute == r,
                                    onClick = { tempRoute = r },
                                    label = { Text(r) }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Zona Geográfica", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.availableZones.forEach { z ->
                                FilterChip(
                                    selected = tempZone == z,
                                    onClick = { tempZone = z },
                                    label = { Text(z) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSupervisorFilter(
                            dateRange = tempRange,
                            route = tempRoute,
                            collectorName = tempCollector,
                            zone = tempZone
                        )
                        showFilterDialog = false
                    }
                ) {
                    Text("Aplicar Filtros")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSupervisorFilter("HOY", "TODAS", "TODOS", "TODAS")
                        showFilterDialog = false
                    }
                ) {
                    Text("Restablecer")
                }
            }
        )
    }

    // EXPORT REPORT DIALOG
    if (showExportDialog) {
        var selectedFormat by remember { mutableStateOf("PDF") }
        var selectedScope by remember { mutableStateOf("Resumen Ejecutivo") }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Reporte de Supervisión")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Generar archivo oficial con la información de cobranza y productividad del equipo:", style = MaterialTheme.typography.bodySmall)

                    Column {
                        Text("Formato de Exportación", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("PDF", "EXCEL (CSV)", "IMPRIMIR").forEach { fmt ->
                                FilterChip(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt },
                                    label = { Text(fmt) }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Contenido del Reporte", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Resumen Ejecutivo", "Productividad por Cobrador", "Reporte de Rutas & Visitas", "Auditoría de Mora Vencida").forEach { scope ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedScope = scope }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = selectedScope == scope, onClick = { selectedScope = scope })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(scope, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.exportSupervisorReport(selectedFormat, selectedScope)
                        showExportDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_export_report")
                ) {
                    Text("Generar y Exportar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SupervisorKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun SupervisorGroupedBarChartCard(
    title: String,
    subtitle: String,
    data: List<ChartDataPoint>,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = data.maxOfOrNull { maxOf(it.value, it.secondaryValue ?: 0.0) }?.takeIf { it > 0 } ?: 100.0

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val width = size.width
                val height = size.height
                val barWidth = (width / (data.size * 3f)).coerceAtMost(30f)
                val spacing = width / data.size

                data.forEachIndexed { i, point ->
                    val x = i * spacing + spacing / 4

                    // Expected bar
                    val expH = (point.secondaryValue ?: 0.0) / maxVal * (height - 30f)
                    drawRect(
                        color = Color(0xFF94A3B8),
                        topLeft = Offset(x.toFloat(), (height - 30f - expH).toFloat()),
                        size = Size(barWidth, expH.toFloat())
                    )

                    // Realized bar
                    val realH = point.value / maxVal * (height - 30f)
                    drawRect(
                        color = Color(0xFF16A34A),
                        topLeft = Offset((x + barWidth + 4f).toFloat(), (height - 30f - realH).toFloat()),
                        size = Size(barWidth, realH.toFloat())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(10.dp).background(Color(0xFF94A3B8)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Esperado", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(10.dp).background(Color(0xFF16A34A)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Realizado", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
