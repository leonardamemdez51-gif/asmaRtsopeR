package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamaTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun RamaBottomNavigation(
    currentRoute: String,
    userRole: String = "",
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val allItems = listOf(
            Triple("dashboard", "Dashboard", Icons.Default.Dashboard),
            Triple("supervisor_dashboard", "Supervisión", Icons.Default.SupervisorAccount),
            Triple("daily_tasks", "Tareas Hoy", Icons.Default.TaskAlt),
            Triple("client_list", "Clientes", Icons.Default.People),
            Triple("loan_list", "Préstamos", Icons.Default.AccountBalanceWallet),
            Triple("cash_register", "Caja", Icons.Default.PointOfSale),
            Triple("collection_visits", "Cobranza", Icons.Default.Map)
        )

        val items = when (userRole) {
            "ADMINISTRADOR" -> allItems
            "SUPERVISOR" -> allItems.filter { it.first in listOf("supervisor_dashboard", "client_list", "loan_list", "collection_visits") }
            "COBRADOR" -> allItems.filter { it.first in listOf("daily_tasks", "client_list", "loan_list", "cash_register") }
            "CONSULTA" -> allItems.filter { it.first in listOf("client_list", "loan_list") }
            else -> allItems.filter { it.first == "dashboard" }
        }

        items.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("nav_item_$route")
            )
        }
    }
}

@Composable
fun RamaNavigationRail(
    currentRoute: String,
    userRole: String = "",
    onNavigate: (String) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = "RAMA ERP",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(32.dp)
            )
        }
    ) {
        val allItems = listOf(
            Triple("dashboard", "Dashboard", Icons.Default.Dashboard),
            Triple("supervisor_dashboard", "Supervisión", Icons.Default.SupervisorAccount),
            Triple("daily_tasks", "Tareas Hoy", Icons.Default.TaskAlt),
            Triple("client_list", "Clientes", Icons.Default.People),
            Triple("loan_list", "Préstamos", Icons.Default.AccountBalanceWallet),
            Triple("cash_register", "Caja", Icons.Default.PointOfSale),
            Triple("collection_visits", "Cobranza", Icons.Default.Map),
            Triple("user_management", "Usuarios", Icons.Default.ManageAccounts),
            Triple("reports", "Reportes", Icons.Default.Assessment),
            Triple("settings", "Ajustes", Icons.Default.Settings)
        )

        val items = when (userRole) {
            "ADMINISTRADOR" -> allItems
            "SUPERVISOR" -> allItems.filter { it.first in listOf("supervisor_dashboard", "client_list", "loan_list", "collection_visits", "reports") }
            "COBRADOR" -> allItems.filter { it.first in listOf("daily_tasks", "client_list", "loan_list", "cash_register") }
            "CONSULTA" -> allItems.filter { it.first in listOf("client_list", "loan_list", "reports") }
            else -> allItems.filter { it.first == "dashboard" }
        }

        items.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route
            NavigationRailItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                modifier = Modifier.testTag("rail_item_$route")
            )
        }
    }
}

@Composable
fun RamaStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RamaStatusBadge(
    status: String
) {
    val (bgColor, textColor) = when (status) {
        "ACTIVO", "PAGADO", "ABIERTA" -> RamaEmeraldContainer to RamaEmeraldSuccess
        "MOROSO", "VENCIDO", "LISTA_NEGRA" -> RamaRedContainer to RamaRedOverdue
        "PENDIENTE", "PARCIAL" -> RamaAmberContainer to RamaAmber
        "LIQUIDADO", "RENOVADO", "CERRADA" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
