package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.security.AppPermission
import com.example.core.security.BruteForceManager
import com.example.data.local.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import java.text.SimpleDateFormat
import java.util.*

val SUCURSALES_LIST = listOf("Todas", "Sucursal Central", "Sucursal Norte", "Sucursal Sur", "Sucursal Este", "Sucursal Oeste")
val RUTAS_LIST = listOf("Ruta 01 - Centro", "Ruta 02 - Mercado", "Ruta 03 - Industrial", "Ruta 04 - Residencial", "Ruta 05 - Periferia")
val FONDOS_LIST = listOf("Fondo Operativo General", "Fondo de Reserva", "Fondo Microcrédito", "Fondo Caja Chica")
val ROLES_LIST = listOf("ADMINISTRADOR", "SUPERVISOR", "COBRADOR", "CONSULTA")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    val isAdmin = sessionState.role.equals("ADMINISTRADOR", ignoreCase = true) ||
            viewModel.hasPermission(AppPermission.USER_MANAGE) ||
            viewModel.hasPermission(AppPermission.USER_VIEW)

    // Filters & Pagination State
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("Todos") } // Todos, Activos, Inactivos, Bloqueados
    var selectedRoleFilter by remember { mutableStateOf("Todos") } // Todos, ADMINISTRADOR, SUPERVISOR, COBRADOR, CONSULTA
    var selectedBranchFilter by remember { mutableStateOf("Todas") }
    var sortBy by remember { mutableStateOf("Nombre") } // Nombre, Usuario, Rol, Último Acceso, Registro
    var currentPage by remember { mutableStateOf(1) }
    var itemsPerPage by remember { mutableStateOf(10) }

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var viewingUser by remember { mutableStateOf<UserEntity?>(null) }
    var permissionUser by remember { mutableStateOf<UserEntity?>(null) }
    var resetPassUser by remember { mutableStateOf<UserEntity?>(null) }

    // Confirmation dialog state
    var pendingConfirmationAction by remember { mutableStateOf<ConfirmActionType?>(null) }
    var confirmationTargetUser by remember { mutableStateOf<UserEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            RamaTopBar(
                title = "Administración de Usuarios",
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_user")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo Usuario")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nuevo Usuario", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (!isAdmin) {
            // Access Denied Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Acceso Restringido",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "El módulo de Administración de Usuarios es de uso exclusivo para el Administrador del Sistema.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        } else {
            // Admin Authorized Interface
            val filteredUsers = remember(users, searchQuery, selectedStatusFilter, selectedRoleFilter, selectedBranchFilter, sortBy) {
                users.filter { user ->
                    val matchesSearch = searchQuery.isBlank() ||
                            user.fullName.contains(searchQuery, ignoreCase = true) ||
                            user.username.contains(searchQuery, ignoreCase = true) ||
                            user.email.contains(searchQuery, ignoreCase = true) ||
                            user.phone.contains(searchQuery, ignoreCase = true) ||
                            user.branch.contains(searchQuery, ignoreCase = true) ||
                            user.route.contains(searchQuery, ignoreCase = true)

                    val isLocked = BruteForceManager.isUserLocked(user.failedAttempts, user.lockedUntilMs)
                    val matchesStatus = when (selectedStatusFilter) {
                        "Activos" -> user.active && !isLocked
                        "Inactivos" -> !user.active
                        "Bloqueados" -> isLocked
                        else -> true
                    }

                    val matchesRole = if (selectedRoleFilter == "Todos") true else user.role.equals(selectedRoleFilter, ignoreCase = true)
                    val matchesBranch = if (selectedBranchFilter == "Todas") true else user.branch.equals(selectedBranchFilter, ignoreCase = true)

                    matchesSearch && matchesStatus && matchesRole && matchesBranch
                }.sortedWith { u1, u2 ->
                    when (sortBy) {
                        "Usuario" -> u1.username.compareTo(u2.username, ignoreCase = true)
                        "Rol" -> u1.role.compareTo(u2.role, ignoreCase = true)
                        "Último Acceso" -> u2.lastLoginMs.compareTo(u1.lastLoginMs)
                        "Registro" -> u2.createdAtMs.compareTo(u1.createdAtMs)
                        else -> u1.fullName.compareTo(u2.fullName, ignoreCase = true)
                    }
                }
            }

            val totalUsersCount = filteredUsers.size
            val totalPages = (totalUsersCount + itemsPerPage - 1).coerceAtLeast(1) / itemsPerPage
            val actualPage = currentPage.coerceIn(1, totalPages.coerceAtLeast(1))
            val pagedUsers = filteredUsers.drop((actualPage - 1) * itemsPerPage).take(itemsPerPage)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header & Search controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            currentPage = 1
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_users"),
                        placeholder = { Text("Buscar por nombre, usuario, correo, teléfono, sucursal...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filters Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == "Todos",
                                onClick = { selectedStatusFilter = "Todos"; currentPage = 1 },
                                label = { Text("Todos (${users.size})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == "Activos",
                                onClick = { selectedStatusFilter = "Activos"; currentPage = 1 },
                                label = { Text("Activos") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == "Inactivos",
                                onClick = { selectedStatusFilter = "Inactivos"; currentPage = 1 },
                                label = { Text("Inactivos") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Gray)
                                    )
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == "Bloqueados",
                                onClick = { selectedStatusFilter = "Bloqueados"; currentPage = 1 },
                                label = { Text("Bloqueados") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary Filters (Roles & Sucursal & Sorting)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var roleDropdownExpanded by remember { mutableStateOf(false) }
                        var sortDropdownExpanded by remember { mutableStateOf(false) }

                        // Role Filter Dropdown
                        Box {
                            AssistChip(
                                onClick = { roleDropdownExpanded = true },
                                label = { Text("Rol: $selectedRoleFilter", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenu(
                                expanded = roleDropdownExpanded,
                                onDismissRequest = { roleDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos los Roles") },
                                    onClick = { selectedRoleFilter = "Todos"; roleDropdownExpanded = false; currentPage = 1 }
                                )
                                ROLES_LIST.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r) },
                                        onClick = { selectedRoleFilter = r; roleDropdownExpanded = false; currentPage = 1 }
                                    )
                                }
                            }
                        }

                        // Sort Dropdown
                        Box {
                            AssistChip(
                                onClick = { sortDropdownExpanded = true },
                                label = { Text("Orden: $sortBy", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenu(
                                expanded = sortDropdownExpanded,
                                onDismissRequest = { sortDropdownExpanded = false }
                            ) {
                                listOf("Nombre", "Usuario", "Rol", "Último Acceso", "Registro").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = { sortBy = option; sortDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                Divider()

                // User Cards List
                if (pagedUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.GroupOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No se encontraron usuarios con los criterios seleccionados",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(pagedUsers, key = { it.id }) { user ->
                            UserItemCard(
                                user = user,
                                onProfileClick = { viewingUser = user },
                                onEditClick = { editingUser = user },
                                onPermissionsClick = { permissionUser = user },
                                onResetPasswordClick = { resetPassUser = user },
                                onToggleStatusClick = {
                                    confirmationTargetUser = user
                                    pendingConfirmationAction = if (user.active) ConfirmActionType.DEACTIVATE else ConfirmActionType.ACTIVATE
                                },
                                onToggleLockClick = {
                                    confirmationTargetUser = user
                                    val isLocked = BruteForceManager.isUserLocked(user.failedAttempts, user.lockedUntilMs)
                                    pendingConfirmationAction = if (isLocked) ConfirmActionType.UNBLOCK else ConfirmActionType.BLOCK
                                },
                                onDeleteClick = {
                                    confirmationTargetUser = user
                                    pendingConfirmationAction = ConfirmActionType.DELETE
                                }
                            )
                        }
                    }
                }

                // Pagination Footer Bar
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Mostrando ${pagedUsers.size} de $totalUsersCount | Pág $actualPage de $totalPages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (actualPage > 1) currentPage-- },
                                enabled = actualPage > 1
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Página anterior")
                            }

                            Text(
                                "$actualPage / $totalPages",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { if (actualPage < totalPages) currentPage++ },
                                enabled = actualPage < totalPages
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Página siguiente")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Create User Dialog
    if (showCreateDialog) {
        UserFormDialog(
            title = "Crear Nuevo Usuario",
            userToEdit = null,
            availableSupervisors = users.filter { it.role.equals("SUPERVISOR", ignoreCase = true) || it.role.equals("ADMINISTRADOR", ignoreCase = true) },
            onDismiss = { showCreateDialog = false },
            onSave = { newUser ->
                viewModel.adminCreateUser(newUser) { result ->
                    if (result is com.example.core.error.Result.Success) {
                        showCreateDialog = false
                    }
                }
            }
        )
    }

    // 2. Edit User Dialog
    editingUser?.let { target ->
        UserFormDialog(
            title = "Editar Usuario: ${target.username}",
            userToEdit = target,
            availableSupervisors = users.filter { it.role.equals("SUPERVISOR", ignoreCase = true) || it.role.equals("ADMINISTRADOR", ignoreCase = true) },
            onDismiss = { editingUser = null },
            onSave = { updated ->
                viewModel.adminUpdateUser(updated) { result ->
                    if (result is com.example.core.error.Result.Success) {
                        editingUser = null
                    }
                }
            }
        )
    }

    // 3. User Profile / Detail View Dialog
    viewingUser?.let { target ->
        UserProfileDetailDialog(
            user = target,
            onDismiss = { viewingUser = null }
        )
    }

    // 4. Permissions Assignment Dialog
    permissionUser?.let { target ->
        UserPermissionsDialog(
            user = target,
            onDismiss = { permissionUser = null },
            onSavePermissions = { perms ->
                viewModel.adminUpdateUserPermissions(target.id, perms) { result ->
                    if (result is com.example.core.error.Result.Success) {
                        permissionUser = null
                    }
                }
            }
        )
    }

    // 5. Password Reset Dialog
    resetPassUser?.let { target ->
        AdminPasswordResetDialog(
            user = target,
            onDismiss = { resetPassUser = null },
            onResetPassword = { newPass ->
                viewModel.adminResetUserPassword(target.id, newPass) { result ->
                    if (result is com.example.core.error.Result.Success) {
                        resetPassUser = null
                    }
                }
            }
        )
    }

    // 6. Action Confirmation Dialogs
    pendingConfirmationAction?.let { action ->
        confirmationTargetUser?.let { target ->
            val (title, text, confirmBtnText, isDanger) = when (action) {
                ConfirmActionType.DEACTIVATE -> Quadruple(
                    "Desactivar Usuario",
                    "¿Está seguro de desactivar al usuario '${target.username}' (${target.fullName})? No podrá iniciar sesión en la aplicación hasta que sea reactivado.",
                    "Desactivar",
                    true
                )
                ConfirmActionType.ACTIVATE -> Quadruple(
                    "Reactivar Usuario",
                    "¿Desea reactivar el acceso para el usuario '${target.username}'?",
                    "Reactivar",
                    false
                )
                ConfirmActionType.BLOCK -> Quadruple(
                    "Bloquear Usuario",
                    "¿Desea bloquear el acceso del usuario '${target.username}' por motivos de seguridad?",
                    "Bloquear",
                    true
                )
                ConfirmActionType.UNBLOCK -> Quadruple(
                    "Desbloquear Usuario",
                    "¿Desea desbloquear la cuenta del usuario '${target.username}' y restablecer sus intentos de acceso?",
                    "Desbloquear",
                    false
                )
                ConfirmActionType.DELETE -> Quadruple(
                    "Eliminar Usuario",
                    "¡ATENCIÓN! ¿Está seguro de eliminar permanentemente al usuario '${target.username}'? Esta acción no se puede deshacer y se registrará en la auditoría.",
                    "Eliminar Permanentemente",
                    true
                )
            }

            AlertDialog(
                onDismissRequest = {
                    pendingConfirmationAction = null
                    confirmationTargetUser = null
                },
                title = { Text(title, fontWeight = FontWeight.Bold) },
                text = { Text(text) },
                confirmButton = {
                    Button(
                        colors = if (isDanger) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        else ButtonDefaults.buttonColors(),
                        onClick = {
                            when (action) {
                                ConfirmActionType.DEACTIVATE -> viewModel.adminSetUserStatus(target.id, false) {}
                                ConfirmActionType.ACTIVATE -> viewModel.adminSetUserStatus(target.id, true) {}
                                ConfirmActionType.BLOCK -> viewModel.adminSetUserLock(target.id, true) {}
                                ConfirmActionType.UNBLOCK -> viewModel.adminSetUserLock(target.id, false) {}
                                ConfirmActionType.DELETE -> viewModel.adminDeleteUser(target.id) {}
                            }
                            pendingConfirmationAction = null
                            confirmationTargetUser = null
                        }
                    ) {
                        Text(confirmBtnText)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            pendingConfirmationAction = null
                            confirmationTargetUser = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private enum class ConfirmActionType {
    DEACTIVATE, ACTIVATE, BLOCK, UNBLOCK, DELETE
}

@Composable
private fun UserItemCard(
    user: UserEntity,
    onProfileClick: () -> Unit,
    onEditClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onToggleStatusClick: () -> Unit,
    onToggleLockClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isLocked = BruteForceManager.isUserLocked(user.failedAttempts, user.lockedUntilMs)

    val statusBg = when {
        isLocked -> MaterialTheme.colorScheme.errorContainer
        !user.active -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val statusText = when {
        isLocked -> "BLOQUEADO"
        !user.active -> "INACTIVO"
        else -> "ACTIVO"
    }

    val statusColor = when {
        isLocked -> MaterialTheme.colorScheme.error
        !user.active -> Color.Gray
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_card_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top User Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Status Indicator Badge Dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(statusColor)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "@${user.username} | ID: #${user.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Organization metadata grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rol: ${user.role}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("Sucursal: ${user.branch.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ruta: ${user.route.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Fondo: ${user.fund.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (user.supervisorName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Supervisor: ${user.supervisorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onProfileClick, modifier = Modifier.testTag("btn_view_profile_${user.id}")) {
                    Icon(Icons.Default.Visibility, contentDescription = "Ver Perfil", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEditClick, modifier = Modifier.testTag("btn_edit_user_${user.id}")) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar Usuario", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onPermissionsClick, modifier = Modifier.testTag("btn_permissions_${user.id}")) {
                    Icon(Icons.Default.VpnKey, contentDescription = "Asignar Permisos", tint = MaterialTheme.colorScheme.tertiary)
                }
                IconButton(onClick = onResetPasswordClick, modifier = Modifier.testTag("btn_reset_pass_${user.id}")) {
                    Icon(Icons.Default.LockReset, contentDescription = "Restablecer Contraseña", tint = Color(0xFFE65100))
                }
                IconButton(onClick = onToggleLockClick, modifier = Modifier.testTag("btn_toggle_lock_${user.id}")) {
                    Icon(
                        if (isLocked) Icons.Default.LockOpen else Icons.Default.Block,
                        contentDescription = if (isLocked) "Desbloquear" else "Bloquear",
                        tint = if (isLocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = onToggleStatusClick, modifier = Modifier.testTag("btn_toggle_status_${user.id}")) {
                    Icon(
                        if (user.active) Icons.Default.PowerSettingsNew else Icons.Default.CheckCircle,
                        contentDescription = if (user.active) "Desactivar" else "Reactivar",
                        tint = if (user.active) Color.Gray else Color(0xFF2E7D32)
                    )
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.testTag("btn_delete_user_${user.id}")) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar Usuario", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormDialog(
    title: String,
    userToEdit: UserEntity?,
    availableSupervisors: List<UserEntity>,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    var username by remember { mutableStateOf(userToEdit?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf(userToEdit?.fullName ?: "") }
    var lastName by remember { mutableStateOf(userToEdit?.lastName ?: "") }
    var email by remember { mutableStateOf(userToEdit?.email ?: "") }
    var phone by remember { mutableStateOf(userToEdit?.phone ?: "") }
    var role by remember { mutableStateOf(userToEdit?.role ?: "COBRADOR") }
    var branch by remember { mutableStateOf(userToEdit?.branch ?: SUCURSALES_LIST.getOrElse(1) { "Sucursal Central" }) }
    var route by remember { mutableStateOf(userToEdit?.route ?: RUTAS_LIST.first()) }
    var fund by remember { mutableStateOf(userToEdit?.fund ?: FONDOS_LIST.first()) }
    var selectedSupervisorName by remember { mutableStateOf(userToEdit?.supervisorName ?: "") }
    var notes by remember { mutableStateOf(userToEdit?.notes ?: "") }

    var roleExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var routeExpanded by remember { mutableStateOf(false) }
    var fundExpanded by remember { mutableStateOf(false) }
    var supervisorExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank() || fullName.isBlank()) {
                        errorMessage = "Nombre de usuario y nombre completo son obligatorios."
                        return@Button
                    }
                    if (userToEdit == null && password.isBlank()) {
                        errorMessage = "La contraseña es requerida para nuevos usuarios."
                        return@Button
                    }

                    val selectedSup = availableSupervisors.find { it.fullName == selectedSupervisorName }

                    val entity = (userToEdit ?: UserEntity(username = username, passwordHash = password, role = role, fullName = fullName)).copy(
                        username = username.trim(),
                        fullName = fullName.trim(),
                        lastName = lastName.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        role = role,
                        branch = branch,
                        route = route,
                        fund = fund,
                        supervisorId = selectedSup?.id,
                        supervisorName = selectedSupervisorName,
                        notes = notes.trim(),
                        passwordHash = if (password.isNotBlank()) password else (userToEdit?.passwordHash ?: "")
                    )
                    onSave(entity)
                }
            ) {
                Text("Guardar Usuario")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Username & Password
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de Usuario (@username)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userToEdit == null,
                    singleLine = true
                )

                if (userToEdit == null) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña Inicial") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Names
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre(s)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Apellidos") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Contact Info
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Asignaciones Organizacionales", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                // Role Dropdown
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol de Sistema") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        ROLES_LIST.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = { role = r; roleExpanded = false }
                            )
                        }
                    }
                }

                // Branch / Sucursal Dropdown
                ExposedDropdownMenuBox(
                    expanded = branchExpanded,
                    onExpandedChange = { branchExpanded = !branchExpanded }
                ) {
                    OutlinedTextField(
                        value = branch,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sucursal Asignada") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = branchExpanded,
                        onDismissRequest = { branchExpanded = false }
                    ) {
                        SUCURSALES_LIST.filter { it != "Todas" }.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = { branch = s; branchExpanded = false }
                            )
                        }
                    }
                }

                // Route / Ruta Dropdown
                ExposedDropdownMenuBox(
                    expanded = routeExpanded,
                    onExpandedChange = { routeExpanded = !routeExpanded }
                ) {
                    OutlinedTextField(
                        value = route,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ruta Asignada") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = routeExpanded,
                        onDismissRequest = { routeExpanded = false }
                    ) {
                        RUTAS_LIST.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = { route = r; routeExpanded = false }
                            )
                        }
                    }
                }

                // Fund / Fondo Dropdown
                ExposedDropdownMenuBox(
                    expanded = fundExpanded,
                    onExpandedChange = { fundExpanded = !fundExpanded }
                ) {
                    OutlinedTextField(
                        value = fund,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fondo Asignado") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fundExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fundExpanded,
                        onDismissRequest = { fundExpanded = false }
                    ) {
                        FONDOS_LIST.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f) },
                                onClick = { fund = f; fundExpanded = false }
                            )
                        }
                    }
                }

                // Supervisor Dropdown
                ExposedDropdownMenuBox(
                    expanded = supervisorExpanded,
                    onExpandedChange = { supervisorExpanded = !supervisorExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSupervisorName.ifBlank { "Sin Supervisor" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supervisor Directo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supervisorExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = supervisorExpanded,
                        onDismissRequest = { supervisorExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin Supervisor") },
                            onClick = { selectedSupervisorName = ""; supervisorExpanded = false }
                        )
                        availableSupervisors.forEach { sup ->
                            DropdownMenuItem(
                                text = { Text("${sup.fullName} (@${sup.username})") },
                                onClick = { selectedSupervisorName = sup.fullName; supervisorExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones / Notas de Administración") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }
    )
}

@Composable
private fun UserProfileDetailDialog(
    user: UserEntity,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val isLocked = BruteForceManager.isUserLocked(user.failedAttempts, user.lockedUntilMs)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar Vista") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Perfil Detallado de Usuario", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Identity Card Header
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                user.fullName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@${user.username} | ID: #${user.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = if (user.active && !isLocked) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isLocked -> "CUENTA BLOQUEADA"
                                        !user.active -> "CUENTA INACTIVA"
                                        else -> "CUENTA ACTIVA"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (user.active && !isLocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Grid Info
                DetailRow("Rol de Sistema:", user.role)
                DetailRow("Sucursal:", user.branch)
                DetailRow("Ruta:", user.route)
                DetailRow("Fondo Operativo:", user.fund)
                DetailRow("Supervisor Directo:", user.supervisorName.ifBlank { "Sin asignar" })
                DetailRow("Correo Electrónico:", user.email.ifBlank { "No registrado" })
                DetailRow("Teléfono de Contacto:", user.phone.ifBlank { "No registrado" })
                DetailRow("Fecha de Alta:", if (user.createdAtMs > 0) dateFormat.format(Date(user.createdAtMs)) else "Inicial")
                DetailRow("Último Acceso:", if (user.lastLoginMs > 0) dateFormat.format(Date(user.lastLoginMs)) else "Sin accesos registrados")

                if (user.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Observaciones:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(user.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Divider()
                Text("Permisos Personalizados Activos:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                val customList = user.customPermissions.split(",").filter { it.isNotBlank() }
                if (customList.isEmpty()) {
                    Text("Heredando permisos predeterminados del rol (${user.role})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        customList.forEach { code ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(code, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UserPermissionsDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSavePermissions: (Set<AppPermission>) -> Unit
) {
    val roleDef = com.example.core.security.RoleRegistry.getRole(user.role)
    val defaultPerms = roleDef.defaultPermissions

    val effectivePermissions = remember(user) {
        val permissions = mutableSetOf<AppPermission>()
        permissions.addAll(defaultPerms)

        if (user.customPermissions.isNotBlank()) {
            user.customPermissions.split(",").forEach { pCodeRaw ->
                val pCode = pCodeRaw.trim()
                if (pCode.startsWith("-")) {
                    val perm = AppPermission.fromCode(pCode.drop(1))
                    if (perm != null) permissions.remove(perm)
                } else if (pCode.startsWith("+")) {
                    val perm = AppPermission.fromCode(pCode.drop(1))
                    if (perm != null) permissions.add(perm)
                } else {
                    val perm = AppPermission.fromCode(pCode)
                    if (perm != null) permissions.add(perm)
                }
            }
        }
        permissions
    }
    
    val selectedPermissions = remember {
        mutableStateListOf<AppPermission>().apply { addAll(effectivePermissions) }
    }

    // Permission groupings
    val categories = remember {
        listOf(
            "Usuarios" to listOf(
                AppPermission.USER_VIEW, AppPermission.USER_CREATE, AppPermission.USER_EDIT, AppPermission.USER_DISABLE,
                AppPermission.USER_RESET_PASSWORD, AppPermission.USER_ASSIGN_ROLE, AppPermission.USER_ASSIGN_ROUTE,
                AppPermission.USER_ASSIGN_FUND, AppPermission.USER_MANAGE_PERMISSIONS
            ),
            "Clientes" to listOf(AppPermission.CLIENT_VIEW, AppPermission.CLIENT_CREATE, AppPermission.CLIENT_EDIT, AppPermission.CLIENT_DELETE),
            "Préstamos" to listOf(AppPermission.LOAN_VIEW, AppPermission.LOAN_CREATE, AppPermission.LOAN_EDIT, AppPermission.LOAN_APPROVE, AppPermission.LOAN_RENEW),
            "Caja y Pagos" to listOf(AppPermission.PAYMENT_VIEW, AppPermission.PAYMENT_CREATE, AppPermission.PAYMENT_EDIT, AppPermission.PAYMENT_CANCEL, AppPermission.CASH_VIEW, AppPermission.CASH_OPEN, AppPermission.CASH_CLOSE),
            "Administración" to listOf(AppPermission.SETTINGS_VIEW, AppPermission.SETTINGS_EDIT, AppPermission.CONFIG_MANAGE),
            "Reportes" to listOf(AppPermission.REPORTS_VIEW, AppPermission.REPORT_EXPORT, AppPermission.DASHBOARD_VIEW),
            "Autorizaciones y Auditoría" to listOf(AppPermission.AUTHORIZATION_VIEW, AppPermission.AUTHORIZATION_APPROVE, AppPermission.AUDIT_VIEW),
            "Evaluación" to listOf(AppPermission.EVALUATE_CLIENT, AppPermission.SCORE_MANUAL_ADJUST),
            "Documentos" to listOf(
                AppPermission.DOCUMENTOS_VER, AppPermission.DOCUMENTOS_CARGAR, AppPermission.DOCUMENTOS_EDITAR,
                AppPermission.DOCUMENTOS_VALIDAR, AppPermission.DOCUMENTOS_RECHAZAR, AppPermission.DOCUMENTOS_ELIMINAR,
                AppPermission.CONFIG_DOCUMENTOS
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Asignar Permisos: ${user.username}", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSavePermissions(selectedPermissions.toSet()) }
            ) {
                Text("Guardar Permisos")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Seleccione los permisos específicos otorgados a este usuario. Estos prevalecen sobre el rol.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                categories.forEach { (categoryName, perms) ->
                    Text(categoryName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    perms.forEach { perm ->
                        val isChecked = selectedPermissions.contains(perm)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedPermissions.remove(perm)
                                    else selectedPermissions.add(perm)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedPermissions.add(perm)
                                    else selectedPermissions.remove(perm)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = buildString {
                                        append(perm.title)
                                        if (defaultPerms.contains(perm)) {
                                            if (isChecked) append(" (Heredado)") else append(" (Denegado)")
                                        } else {
                                            if (isChecked) append(" (Individual)")
                                        }
                                    },
                                    fontWeight = FontWeight.SemiBold, 
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (defaultPerms.contains(perm) && !isChecked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(perm.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    )
}

@Composable
private fun AdminPasswordResetDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onResetPassword: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFFE65100))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restablecer Contraseña", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                onClick = {
                    if (newPassword.length < 6) {
                        errorMsg = "La contraseña debe tener al menos 6 caracteres."
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMsg = "Las contraseñas no coinciden."
                        return@Button
                    }
                    onResetPassword(newPassword)
                }
            ) {
                Text("Restablecer Contraseña")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Ingrese la nueva contraseña para el usuario '@${user.username}' (${user.fullName}):",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Nueva Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    )
}
