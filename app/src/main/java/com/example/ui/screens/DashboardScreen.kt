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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.ClientEntity
import com.example.data.local.LoanEntity
import com.example.ui.DailyTaskItem
import com.example.ui.MainViewModel
import com.example.ui.components.RamaStatCard
import com.example.ui.components.RamaStatusBadge
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val activeCashRegister by viewModel.activeCashRegister.collectAsStateWithLifecycle()
    val notifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val executiveState by viewModel.executiveDashboardState.collectAsStateWithLifecycle()
    val activeLoans by viewModel.activeLoans.collectAsStateWithLifecycle()

    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedAlertTab by remember { mutableStateOf(0) } // 0: Críticos, 1: Vencidos, 2: Cobros Hoy, 3: Renovaciones

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val user = session.user
    val kpis = executiveState.kpis
    val charts = executiveState.charts
    val alerts = executiveState.alerts
    val filter = executiveState.filter

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
                                    text = "Dashboard Ejecutivo",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                            Text(
                                text = "En tiempo real • ${user?.fullName ?: "Admin"} (${user?.role ?: "ADMINISTRADOR"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.testTag("action_filter_dashboard")
                    ) {
                        BadgedBox(
                            badge = {
                                if (filter.branch != "TODAS" || filter.route != "TODAS" || filter.collectorName != "TODOS" || filter.dateRange != "MES") {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtros Ejecutivos")
                        }
                    }
                    IconButton(
                        onClick = { onNavigate("notification_center") },
                        modifier = Modifier.testTag("action_notifications")
                    ) {
                        BadgedBox(
                            badge = {
                                if (notifications.isNotEmpty()) {
                                    Badge { Text(notifications.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Centro de Notificaciones")
                        }
                    }
                    IconButton(
                        onClick = { onNavigate("reports") },
                        modifier = Modifier.testTag("action_reports")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = "Reportes")
                    }
                    IconButton(
                        onClick = { onNavigate("settings") },
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("action_logout")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión")
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
            // Live Status Banner & Active Cash State
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeCashRegister != null)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (activeCashRegister != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activeCashRegister != null) Icons.Default.PointOfSale else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (activeCashRegister != null) "Caja Operativa ABIERTA" else "Caja CERRADA",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (activeCashRegister != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (activeCashRegister != null)
                                        "Efectivo en caja: ${currencyFormat.format(activeCashRegister!!.expectedCash)}"
                                    else
                                        "Abra la caja para autorizar desembolsos y cobros",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeCashRegister != null) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Button(
                            onClick = { onNavigate("cash_register") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeCashRegister != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("caja_action_button")
                        ) {
                            Text(if (activeCashRegister != null) "Ver Caja" else "Abrir Caja")
                        }
                    }
                }
            }

            // Executive Filter Summary Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Filtros Aplicados:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            TextButton(onClick = { showFilterDialog = true }) {
                                Text("Cambiar")
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = { showFilterDialog = true },
                                label = { Text("Periodo: ${filter.dateRange}") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            FilterChip(
                                selected = filter.branch != "TODAS",
                                onClick = { showFilterDialog = true },
                                label = { Text("Sucursal: ${filter.branch}") },
                                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            FilterChip(
                                selected = filter.route != "TODAS",
                                onClick = { showFilterDialog = true },
                                label = { Text("Ruta: ${filter.route}") },
                                leadingIcon = { Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            if (filter.collectorName != "TODOS") {
                                FilterChip(
                                    selected = true,
                                    onClick = { showFilterDialog = true },
                                    label = { Text("Cobrador: ${filter.collectorName}") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            // Motor de Evaluación & Score Executive Overview
            item {
                com.example.ui.components.EvaluationDashboardCard(
                    viewModel = viewModel,
                    onNavigateToClients = { onNavigate("clients") }
                )
            }

            // Quick Operations Horizontal Shortcuts
            item {
                Text(
                    text = "Acciones Rápidas del Administrador",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickActionButton(
                            label = "Mapa GPS",
                            icon = Icons.Default.Map,
                            onClick = { onNavigate("collector_map") },
                            testTag = "btn_collector_map"
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Gestión Rutas",
                            icon = Icons.Default.Route,
                            onClick = { onNavigate("route_management") },
                            testTag = "btn_route_management"
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Supervisión",
                            icon = Icons.Default.SupervisorAccount,
                            onClick = { onNavigate("supervisor_dashboard") },
                            testTag = "btn_supervisor_dashboard"
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Nuevo Préstamo",
                            icon = Icons.Default.AddCard,
                            onClick = { onNavigate("new_loan") },
                            testTag = "btn_new_loan"
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Registrar Pago",
                            icon = Icons.Default.Payments,
                            onClick = { onNavigate("loan_list") },
                            testTag = "btn_pay"
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Nuevo Cliente",
                            icon = Icons.Default.PersonAdd,
                            onClick = { onNavigate("new_client") },
                            testTag = "btn_new_client"
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Configuración",
                            icon = Icons.Default.Settings,
                            onClick = { onNavigate("settings") },
                            testTag = "btn_settings"
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Auditoría",
                            icon = Icons.Default.Security,
                            onClick = { onNavigate("audit_logs") },
                            testTag = "btn_audit"
                        )
                    }
                    item {
                        QuickActionButton(
                            label = "Reportes",
                            icon = Icons.Default.BarChart,
                            onClick = { onNavigate("reports") },
                            testTag = "btn_reports"
                        )
                    }
                }
            }

            // 17 EXECUTIVE KPI CARDS SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Indicadores Clave (KPIs Ejecutivos)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "17 Métricas",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Row 1: Clientes Activos, Clientes Nuevos, Clientes Bloqueados
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveKpiCard(
                        title = "Clientes Activos",
                        value = kpis.activeClientsCount.toString(),
                        subtitle = "Cartera vigente",
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Clientes Nuevos",
                        value = kpis.newClientsCount.toString(),
                        subtitle = "Últimos 30 días",
                        icon = Icons.Default.PersonAdd,
                        iconColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Bloqueados",
                        value = kpis.blockedClientsCount.toString(),
                        subtitle = "Mora / Lista negra",
                        icon = Icons.Default.Block,
                        iconColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 2: Préstamos Activos, Liquidados, Vencidos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveKpiCard(
                        title = "Préstamos Activos",
                        value = kpis.activeLoansCount.toString(),
                        subtitle = "En amortización",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Liquidados",
                        value = kpis.liquidatedLoansCount.toString(),
                        subtitle = "100% Pagados",
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Vencidos",
                        value = kpis.overdueLoansCount.toString(),
                        subtitle = "Requiere gestión",
                        icon = Icons.Default.Warning,
                        iconColor = Color(0xFFEA580C),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 3: Capital Colocado, Capital Recuperado, Intereses Cobrados
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveKpiCard(
                        title = "Capital Colocado",
                        value = currencyFormat.format(kpis.placedCapital),
                        subtitle = "Monto prestado",
                        icon = Icons.Default.MonetizationOn,
                        iconColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Capital Recuperado",
                        value = currencyFormat.format(kpis.recoveredCapital),
                        subtitle = "Cobrado a capital",
                        icon = Icons.Default.TrendingUp,
                        iconColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Intereses Cobrados",
                        value = currencyFormat.format(kpis.interestCollected),
                        subtitle = "Ganancia realizada",
                        icon = Icons.Default.Savings,
                        iconColor = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 4: Cobranza Esperada, Realizada, Pendiente
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveKpiCard(
                        title = "Cobranza Esperada",
                        value = currencyFormat.format(kpis.expectedCollection),
                        subtitle = "Meta acumulada",
                        icon = Icons.Default.PriceCheck,
                        iconColor = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Cobranza Realizada",
                        value = currencyFormat.format(kpis.realizedCollection),
                        subtitle = "Ingresado a caja",
                        icon = Icons.Default.Payments,
                        iconColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Cobranza Pendiente",
                        value = currencyFormat.format(kpis.pendingCollection),
                        subtitle = "Por recaudar",
                        icon = Icons.Default.HourglassEmpty,
                        iconColor = Color(0xFFD97706),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 5: Mora, Renovaciones, Pagos del Día
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveKpiCard(
                        title = "Recargo por Mora",
                        value = currencyFormat.format(kpis.lateFeeAmount),
                        subtitle = "Multas aplicadas",
                        icon = Icons.Default.ReportProblem,
                        iconColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Renovaciones",
                        value = kpis.renewalsCount.toString(),
                        subtitle = "Créditos renovados",
                        icon = Icons.Default.Autorenew,
                        iconColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Pagos de Hoy",
                        value = currencyFormat.format(kpis.todayPaymentsAmount),
                        subtitle = "Recaudado hoy",
                        icon = Icons.Default.Today,
                        iconColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 6: Pagos Atrasados & Score Promedio
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveKpiCard(
                        title = "Pagos Atrasados",
                        value = kpis.overdueInstallmentsCount.toString(),
                        subtitle = "Cuotas vencidas",
                        icon = Icons.Default.Schedule,
                        iconColor = Color(0xFFEA580C),
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveKpiCard(
                        title = "Score Promedio",
                        value = String.format(Locale.getDefault(), "%.1f pts", kpis.averageClientScore),
                        subtitle = "Calificación general",
                        icon = Icons.Default.Stars,
                        iconColor = Color(0xFFCA8A04),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // REAL-TIME CHARTS SECTION
            item {
                Text(
                    text = "Análisis Gráfico Financiero",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Chart 1: Cobranza Mensual
            item {
                BarChartCard(
                    title = "Cobranza Mensual (MXN)",
                    subtitle = "Comportamiento de recuperaciones por mes",
                    data = charts.monthlyCollection,
                    currencyFormat = currencyFormat,
                    barColor = Color(0xFF2563EB)
                )
            }

            // Chart 2: Cobranza Semanal
            item {
                BarChartCard(
                    title = "Cobranza Semanal de la Ruta",
                    subtitle = "Recaudación diaria de lunes a domingo",
                    data = charts.weeklyCollection,
                    currencyFormat = currencyFormat,
                    barColor = Color(0xFF059669)
                )
            }

            // Chart 3: Crecimiento de Cartera
            item {
                LineChartCard(
                    title = "Crecimiento de Cartera de Préstamos",
                    subtitle = "Evolución del capital colado acumulado",
                    data = charts.portfolioGrowth,
                    currencyFormat = currencyFormat,
                    lineColor = Color(0xFF7C3AED)
                )
            }

            // Chart 4 & 5: Distribución por Zonas & Distribución por Cobrador
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDistributionCard(
                        title = "Distribución por Zonas",
                        subtitle = "Monto de cartera colocada por zona geográfica",
                        items = charts.distributionByZone,
                        currencyFormat = currencyFormat,
                        accentColor = Color(0xFF0284C7)
                    )

                    HorizontalDistributionCard(
                        title = "Distribución por Cobrador",
                        subtitle = "Recaudación por agente de cobro asignado",
                        items = charts.distributionByCollector,
                        currencyFormat = currencyFormat,
                        accentColor = Color(0xFF0D9488)
                    )
                }
            }

            // Chart 6 & 7: Préstamos por Plan & Clientes por Estado
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDistributionCard(
                        title = "Préstamos por Plan de Crédito",
                        subtitle = "Volumen colocado en Plan 20 Días vs Plan 30 Días",
                        items = charts.loansByPlan,
                        currencyFormat = currencyFormat,
                        accentColor = Color(0xFF16A34A)
                    )

                    HorizontalDistributionCard(
                        title = "Clientes por Estado de Cartera",
                        subtitle = "Distribución de clientes activos, morosos y lista negra",
                        items = charts.clientsByStatus,
                        currencyFormat = currencyFormat,
                        isCurrency = false,
                        accentColor = Color(0xFFDC2626)
                    )
                }
            }

            // EXECUTIVE ALERTS SECTION
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alertas Ejecutivas del Sistema",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ScrollableTabRow(
                            selectedTabIndex = selectedAlertTab,
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent
                        ) {
                            Tab(
                                selected = selectedAlertTab == 0,
                                onClick = { selectedAlertTab = 0 },
                                text = { Text("Clientes Críticos (${alerts.criticalClients.size})") }
                            )
                            Tab(
                                selected = selectedAlertTab == 1,
                                onClick = { selectedAlertTab = 1 },
                                text = { Text("Préstamos Vencidos (${alerts.overdueLoans.size})") }
                            )
                            Tab(
                                selected = selectedAlertTab == 2,
                                onClick = { selectedAlertTab = 2 },
                                text = { Text("Cobros de Hoy (${alerts.topDailyDueInstallments.size})") }
                            )
                            Tab(
                                selected = selectedAlertTab == 3,
                                onClick = { selectedAlertTab = 3 },
                                text = { Text("Próximas Renovaciones (${alerts.eligibleRenewalLoans.size})") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (selectedAlertTab) {
                            0 -> { // Clientes Críticos
                                if (alerts.criticalClients.isEmpty()) {
                                    Text(
                                        text = "No hay clientes críticos o en riesgo actualmente.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        alerts.criticalClients.take(5).forEach { client ->
                                            AlertClientItem(client = client, onNavigate = onNavigate)
                                        }
                                    }
                                }
                            }
                            1 -> { // Préstamos Vencidos
                                if (alerts.overdueLoans.isEmpty()) {
                                    Text(
                                        text = "Excelente. No existen préstamos con mora crítica.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        alerts.overdueLoans.take(5).forEach { loan ->
                                            AlertLoanItem(loan = loan, currencyFormat = currencyFormat, onNavigate = onNavigate)
                                        }
                                    }
                                }
                            }
                            2 -> { // Cobros de Hoy
                                if (alerts.topDailyDueInstallments.isEmpty()) {
                                    Text(
                                        text = "Todos los cobros del día han sido liquidados o no hay cuotas para hoy.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        alerts.topDailyDueInstallments.take(5).forEach { task ->
                                            AlertTaskItem(task = task, currencyFormat = currencyFormat, onNavigate = onNavigate)
                                        }
                                    }
                                }
                            }
                            3 -> { // Próximas Renovaciones
                                if (alerts.eligibleRenewalLoans.isEmpty()) {
                                    Text(
                                        text = "No hay préstamos listos para renovación (>=75% pagado).",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        alerts.eligibleRenewalLoans.take(5).forEach { loan ->
                                            AlertRenewalItem(loan = loan, currencyFormat = currencyFormat, onNavigate = onNavigate)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RECENT ACTIVE LOANS LIST
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Últimos Préstamos Colocados",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { onNavigate("loan_list") }) {
                        Text("Ver Todos")
                    }
                }
            }

            if (activeLoans.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay préstamos registrados en el sistema")
                        }
                    }
                }
            } else {
                items(activeLoans.take(5)) { loan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("loan_detail/${loan.id}") },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Préstamo #${loan.id} • ${loan.planType.replace("_", " ")} • ${loan.collectorName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RamaStatusBadge(status = loan.status)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total: ${currencyFormat.format(loan.totalAmount)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Saldo: ${currencyFormat.format(loan.remainingBalance)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
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
        var tempDateRange by remember { mutableStateOf(filter.dateRange) }
        var tempBranch by remember { mutableStateOf(filter.branch) }
        var tempRoute by remember { mutableStateOf(filter.route) }
        var tempCollector by remember { mutableStateOf(filter.collectorName) }

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Filtros del Dashboard Ejecutivo")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Filtrar la información del sistema para análisis focalizado:", style = MaterialTheme.typography.bodySmall)

                    // Period Filter
                    Column {
                        Text("Periodo de Fecha", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("HOY", "SEMANA", "MES", "ANIO", "TODO").forEach { range ->
                                FilterChip(
                                    selected = tempDateRange == range,
                                    onClick = { tempDateRange = range },
                                    label = { Text(range) }
                                )
                            }
                        }
                    }

                    // Branch Filter
                    Column {
                        Text("Sucursal", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            executiveState.availableBranches.forEach { br ->
                                FilterChip(
                                    selected = tempBranch == br,
                                    onClick = { tempBranch = br },
                                    label = { Text(br) }
                                )
                            }
                        }
                    }

                    // Route Filter
                    Column {
                        Text("Ruta Operativa", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            executiveState.availableRoutes.forEach { rt ->
                                FilterChip(
                                    selected = tempRoute == rt,
                                    onClick = { tempRoute = rt },
                                    label = { Text(rt) }
                                )
                            }
                        }
                    }

                    // Collector Filter
                    Column {
                        Text("Cobrador Asignado", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            executiveState.availableCollectors.take(5).forEach { col ->
                                FilterChip(
                                    selected = tempCollector == col,
                                    onClick = { tempCollector = col },
                                    label = { Text(col) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDashboardFilter(
                            dateRange = tempDateRange,
                            branch = tempBranch,
                            route = tempRoute,
                            collectorName = tempCollector
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
                        viewModel.updateDashboardFilter(
                            dateRange = "MES",
                            branch = "TODAS",
                            route = "TODAS",
                            collectorName = "TODOS"
                        )
                        showFilterDialog = false
                    }
                ) {
                    Text("Restablecer")
                }
            }
        )
    }

    // NOTIFICATIONS DIALOG
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notificaciones y Alertas")
                }
            },
            text = {
                if (notifications.isEmpty()) {
                    Text("Sin notificaciones pendientes en el sistema.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        notifications.forEach { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (notif.type) {
                                        "ALERT" -> MaterialTheme.colorScheme.errorContainer
                                        "WARNING" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(notif.title, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(notif.message, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNotificationsDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
fun ExecutiveKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BarChartCard(
    title: String,
    subtitle: String,
    data: List<ChartDataPoint>,
    currencyFormat: NumberFormat,
    barColor: Color
) {
    val maxValue = (data.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { item ->
                    val fraction = (item.value / maxValue).toFloat().coerceIn(0.08f, 1.0f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (item.value >= 1000) "${(item.value / 1000).toInt()}k" else "${item.value.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LineChartCard(
    title: String,
    subtitle: String,
    data: List<ChartDataPoint>,
    currencyFormat: NumberFormat,
    lineColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = (data.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val pointsCount = data.size

                    if (pointsCount > 1) {
                        val stepX = width / (pointsCount - 1)
                        val path = Path()

                        data.forEachIndexed { i, point ->
                            val x = i * stepX
                            val y = height - ((point.value / maxVal) * (height - 20)).toFloat() - 10f

                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }

                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach {
                    Text(
                        text = it.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalDistributionCard(
    title: String,
    subtitle: String,
    items: List<ChartDataPoint>,
    currencyFormat: NumberFormat,
    isCurrency: Boolean = true,
    accentColor: Color
) {
    val totalVal = items.sumOf { it.value }.coerceAtLeast(1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    val fraction = (item.value / totalVal).toFloat().coerceIn(0.05f, 1.0f)
                    val formattedVal = if (isCurrency) currencyFormat.format(item.value) else "${item.value.toInt()} reg."

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = formattedVal,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = accentColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertClientItem(client: ClientEntity, onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("client_detail/${client.id}") },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(client.fullName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("CURP: ${client.curp} • Zone: ${client.zone}", style = MaterialTheme.typography.bodySmall)
            }
            Surface(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${client.punctualityScore} pts",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun AlertLoanItem(loan: LoanEntity, currencyFormat: NumberFormat, onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("loan_detail/${loan.id}") },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Préstamo #${loan.id} • ${loan.clientName}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("Cobrador: ${loan.collectorName}", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(currencyFormat.format(loan.remainingBalance), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                Text("Saldo en Mora", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AlertTaskItem(task: DailyTaskItem, currencyFormat: NumberFormat, onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("loan_detail/${task.loan.id}") },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(task.client.fullName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("Cuota #${task.installment.installmentNumber} • Tel: ${task.client.phone}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                currencyFormat.format(task.installment.targetAmount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun AlertRenewalItem(loan: LoanEntity, currencyFormat: NumberFormat, onNavigate: (String) -> Unit) {
    val progressPercent = ((loan.paidAmount / loan.totalAmount) * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("new_loan") },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(loan.clientName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("Pagado $progressPercent% (${currencyFormat.format(loan.paidAmount)})", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onNavigate("new_loan") },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Renovar", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

