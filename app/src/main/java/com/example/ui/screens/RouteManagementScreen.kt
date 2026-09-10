package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.CollectionRouteEntity
import com.example.data.local.RouteClientAssignmentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val routes by viewModel.allRoutes.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    var showCreateRouteDialog by remember { mutableStateOf(false) }
    var selectedRouteForEdit by remember { mutableStateOf<CollectionRouteEntity?>(null) }
    var selectedRouteForAssignments by remember { mutableStateOf<CollectionRouteEntity?>(null) }

    // Create / Edit Route state
    var routeCodeInput by remember { mutableStateOf("") }
    var routeNameInput by remember { mutableStateOf("") }
    var branchInput by remember { mutableStateOf("Sucursal Central") }
    var supervisorInput by remember { mutableStateOf("Lucía Ramírez") }
    var collectorInput by remember { mutableStateOf("Roberto Gómez") }
    var zoneInput by remember { mutableStateOf("Zona Centro") }
    var notesInput by remember { mutableStateOf("") }
    var isActiveInput by remember { mutableStateOf(true) }

    // Assignments state
    var routeAssignments by remember { mutableStateOf<List<RouteClientAssignmentEntity>>(emptyList()) }
    var showAddClientDialog by remember { mutableStateOf(false) }

    val collectorsList = remember(users) {
        users.filter { it.role == "COBRADOR" || it.role == "SUPERVISOR" || it.role == "ADMINISTRADOR" }
    }

    Scaffold(
        topBar = {
            RamaTopBar(
                title = "Gestión de Rutas y Cobertura Geográfica",
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedRouteForEdit = null
                    routeCodeInput = "RUT-0${routes.size + 1}"
                    routeNameInput = "Ruta 0${routes.size + 1} - Nueva Zona"
                    branchInput = "Sucursal Central"
                    supervisorInput = "Lucía Ramírez"
                    collectorInput = "Roberto Gómez"
                    zoneInput = "Zona Centro"
                    notesInput = ""
                    isActiveInput = true
                    showCreateRouteDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_route")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Ruta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header stats card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
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
                            text = "Rutas Operativas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${routes.size} Rutas • ${clients.size} Clientes en Sistema",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (routes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay rutas registradas. Toca '+' para agregar la primera.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(routes) { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_route_${route.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = route.code,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = route.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Surface(
                                        color = if (route.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (route.isActive) "ACTIVA" else "INACTIVA",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column {
                                        Text("Cobrador", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(route.collectorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column {
                                        Text("Zona", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(route.zone, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column {
                                        Text("Sucursal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(route.branchName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Divider(Modifier.padding(vertical = 12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${route.totalClientsCount} clientes asignados",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedRouteForAssignments = route
                                            },
                                            modifier = Modifier.testTag("btn_assign_clients_${route.id}")
                                        ) {
                                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Clientes")
                                        }

                                        IconButton(
                                            onClick = {
                                                selectedRouteForEdit = route
                                                routeCodeInput = route.code
                                                routeNameInput = route.name
                                                branchInput = route.branchName
                                                supervisorInput = route.supervisorName
                                                collectorInput = route.collectorName
                                                zoneInput = route.zone
                                                notesInput = route.notes
                                                isActiveInput = route.isActive
                                                showCreateRouteDialog = true
                                            },
                                            modifier = Modifier.testTag("btn_edit_route_${route.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar Ruta")
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

    // Create / Edit Route Dialog
    if (showCreateRouteDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRouteDialog = false },
            title = {
                Text(
                    if (selectedRouteForEdit == null) "Nueva Ruta de Cobranza" else "Editar Ruta: ${selectedRouteForEdit?.code}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = routeCodeInput,
                            onValueChange = { routeCodeInput = it },
                            label = { Text("Código") },
                            modifier = Modifier.weight(0.4f).testTag("input_route_code")
                        )
                        OutlinedTextField(
                            value = routeNameInput,
                            onValueChange = { routeNameInput = it },
                            label = { Text("Nombre de la Ruta") },
                            modifier = Modifier.weight(0.6f).testTag("input_route_name")
                        )
                    }

                    OutlinedTextField(
                        value = collectorInput,
                        onValueChange = { collectorInput = it },
                        label = { Text("Cobrador Asignado") },
                        modifier = Modifier.fillMaxWidth().testTag("input_route_collector")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = zoneInput,
                            onValueChange = { zoneInput = it },
                            label = { Text("Zona") },
                            modifier = Modifier.weight(0.5f).testTag("input_route_zone")
                        )
                        OutlinedTextField(
                            value = branchInput,
                            onValueChange = { branchInput = it },
                            label = { Text("Sucursal") },
                            modifier = Modifier.weight(0.5f).testTag("input_route_branch")
                        )
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notas / Observaciones") },
                        modifier = Modifier.fillMaxWidth().testTag("input_route_notes")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ruta Activa")
                        Switch(
                            checked = isActiveInput,
                            onCheckedChange = { isActiveInput = it },
                            modifier = Modifier.testTag("switch_route_active")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val routeToSave = CollectionRouteEntity(
                            id = selectedRouteForEdit?.id ?: 0L,
                            code = routeCodeInput.trim(),
                            name = routeNameInput.trim(),
                            branchName = branchInput.trim(),
                            supervisorName = supervisorInput.trim(),
                            collectorId = 2L,
                            collectorName = collectorInput.trim(),
                            zone = zoneInput.trim(),
                            isActive = isActiveInput,
                            notes = notesInput.trim(),
                            totalClientsCount = selectedRouteForEdit?.totalClientsCount ?: 0
                        )
                        viewModel.saveRoute(routeToSave, "admin")
                        showCreateRouteDialog = false
                        Toast.makeText(context, "Ruta guardada exitosamente", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_save_route_confirm")
                ) {
                    Text("Guardar Ruta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRouteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Client Route Assignments Sheet Modal
    selectedRouteForAssignments?.let { route ->
        val assignmentsFlow = viewModel.getAssignmentsForRoute(route.id).collectAsStateWithLifecycle(emptyList())
        val assignments = assignmentsFlow.value

        AlertDialog(
            onDismissRequest = { selectedRouteForAssignments = null },
            title = {
                Text("Asignación de Clientes: ${route.name}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${assignments.size} Clientes en Secuencia")
                        Button(
                            onClick = { showAddClientDialog = true },
                            modifier = Modifier.testTag("btn_add_client_to_route")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar Cliente")
                        }
                    }

                    if (assignments.isEmpty()) {
                        Text(
                            "No hay clientes asignados a esta ruta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(assignments) { idx, assignment ->
                                val client = clients.firstOrNull { it.id == assignment.clientId }
                                if (client != null) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "#${idx + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(client.fullName, fontWeight = FontWeight.Bold)
                                                    Text(client.address, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.removeClientFromRoute(route.id, client.id)
                                                },
                                                modifier = Modifier.testTag("btn_remove_client_${client.id}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar de ruta", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedRouteForAssignments = null }) {
                    Text("Cerrar")
                }
            }
        )

        // Add Client Selection Dialog
        if (showAddClientDialog) {
            AlertDialog(
                onDismissRequest = { showAddClientDialog = false },
                title = { Text("Seleccionar Cliente para Asignar", fontWeight = FontWeight.Bold) },
                text = {
                    val availableClients = clients.filter { client ->
                        assignments.none { it.clientId == client.id }
                    }

                    if (availableClients.isEmpty()) {
                        Text("Todos los clientes disponibles ya están en esta ruta.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(availableClients) { client ->
                                Card(
                                    onClick = {
                                        viewModel.assignClientToRoute(route.id, client.id, assignments.size + 1)
                                        showAddClientDialog = false
                                        Toast.makeText(context, "${client.fullName} asignado a la ruta", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("card_select_assign_client_${client.id}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(client.fullName, fontWeight = FontWeight.Bold)
                                        Text(client.address, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddClientDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
