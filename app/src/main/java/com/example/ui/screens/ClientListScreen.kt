package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClientEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaStatusBadge
import com.example.ui.components.RamaTopBar

@Composable
fun ClientListScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val searchQuery by viewModel.clientSearchQuery.collectAsStateWithLifecycle()
    var selectedStatusFilter by remember { mutableStateOf("TODOS") }

    val statusFilters = listOf("TODOS", "ACTIVO", "MOROSO", "INACTIVO", "LISTA_NEGRA")

    val filteredClients = remember(clients, searchQuery, selectedStatusFilter) {
        clients.filter { client ->
            val matchesQuery = searchQuery.isBlank() ||
                    client.fullName.contains(searchQuery, ignoreCase = true) ||
                    client.curp.contains(searchQuery, ignoreCase = true) ||
                    client.ine.contains(searchQuery, ignoreCase = true) ||
                    client.rfc.contains(searchQuery, ignoreCase = true) ||
                    client.phone.contains(searchQuery, ignoreCase = true) ||
                    client.address.contains(searchQuery, ignoreCase = true) ||
                    client.id.toString().contains(searchQuery)

            val matchesStatus = if (selectedStatusFilter == "TODOS") true else client.status.equals(selectedStatusFilter, ignoreCase = true)

            matchesQuery && matchesStatus
        }
    }

    val activeCount = remember(clients) { clients.count { it.status == "ACTIVO" } }
    val morosoCount = remember(clients) { clients.count { it.status == "MOROSO" || it.status == "LISTA_NEGRA" } }
    val avgScore = remember(clients) { if (clients.isNotEmpty()) clients.map { it.punctualityScore }.average().toInt() else 100 }

    Scaffold(
        topBar = {
            RamaTopBar(title = "Gestión de Clientes", subtitle = "Expedientes Digitales • ${clients.size} Registrados")
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate("new_client") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Nuevo Cliente", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_client")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // KPI Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiChip(
                    title = "Total",
                    value = "${clients.size}",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                KpiChip(
                    title = "Activos",
                    value = "$activeCount",
                    color = Color(0xFFD1FAE5),
                    contentColor = Color(0xFF065F46),
                    modifier = Modifier.weight(1f)
                )
                KpiChip(
                    title = "Morosos",
                    value = "$morosoCount",
                    color = Color(0xFFFEE2E2),
                    contentColor = Color(0xFF991B1B),
                    modifier = Modifier.weight(1f)
                )
                KpiChip(
                    title = "Score Prom",
                    value = "$avgScore pts",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1.1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setClientSearchQuery(it) },
                placeholder = { Text("Buscar por Nombre, CURP, INE, Teléfono, ID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setClientSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_client_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(statusFilters) { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = {
                            Text(
                                text = when (status) {
                                    "TODOS" -> "Todos (${clients.size})"
                                    "ACTIVO" -> "Activos ($activeCount)"
                                    "MOROSO" -> "Morosos"
                                    "INACTIVO" -> "Inactivos"
                                    "LISTA_NEGRA" -> "Lista Negra"
                                    else -> status
                                },
                                fontWeight = if (selectedStatusFilter == status) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se encontraron clientes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Pruebe con otros términos de búsqueda o ajuste los filtros.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        ClientExpedienteCard(
                            client = client,
                            onOpenDetail = { onNavigate("client_detail/${client.id}") },
                            onNewLoan = { onNavigate("new_loan?clientId=${client.id}") },
                            onOpenGps = {
                                val uri = "geo:${client.latitude},${client.longitude}?q=${Uri.encode(client.address)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            onWhatsApp = {
                                val cleanPhone = client.phone.replace("[^0-9]".toRegex(), "")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=52$cleanPhone"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiChip(
    title: String,
    value: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = contentColor.copy(alpha = 0.8f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
private fun ClientExpedienteCard(
    client: ClientEntity,
    onOpenDetail: () -> Unit,
    onNewLoan: () -> Unit,
    onOpenGps: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
            .testTag("client_card_${client.id}"),
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = client.fullName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = client.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "ID: #${client.id} • CURP: ${client.curp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RamaStatusBadge(status = client.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Information Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(client.phone, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${client.address} (${client.zone})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }

                // Score Chip
                Surface(
                    color = when {
                        client.punctualityScore >= 85 -> Color(0xFFD1FAE5)
                        client.punctualityScore >= 70 -> Color(0xFFFEF3C7)
                        else -> Color(0xFFFEE2E2)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Score", fontSize = 9.sp, color = Color.Gray)
                        Text("${client.punctualityScore} pts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phone Call
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Llamar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }

                // WhatsApp
                IconButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFDCFCE7), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(18.dp), tint = Color(0xFF16A34A))
                }

                // GPS Map
                IconButton(
                    onClick = onOpenGps,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Google Maps", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Ver Expediente
                OutlinedButton(
                    onClick = onOpenDetail,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Expediente", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                // Nuevo Préstamo
                Button(
                    onClick = onNewLoan,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("btn_loan_for_client_${client.id}")
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Préstamo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
