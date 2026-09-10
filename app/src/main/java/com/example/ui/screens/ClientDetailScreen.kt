package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.ClientEvaluationView
import com.example.ui.components.RamaStatusBadge
import com.example.ui.components.RamaTopBar
import com.example.ui.components.WhatsAppMessageDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var client by remember { mutableStateOf<ClientEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var previewDocumentUri by remember { mutableStateOf<Pair<String, String>?>(null) } // (Title, Uri)

    // Location dialog state
    var showCaptureLocationDialog by remember { mutableStateOf(false) }
    var capturedLat by remember { mutableStateOf("") }
    var capturedLng by remember { mutableStateOf("") }
    var capturedAccuracy by remember { mutableFloatStateOf(5.0f) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf("") }

    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    val config = systemConfig ?: com.example.data.local.SystemConfigEntity()

    val loans by viewModel.loanRepo.getLoansByClient(clientId).collectAsStateWithLifecycle(initialValue = emptyList())
    val visits by viewModel.collectionRepo.getVisitsByClient(clientId).collectAsStateWithLifecycle(initialValue = emptyList())
    val payments by viewModel.loanRepo.allPayments.collectAsStateWithLifecycle(initialValue = emptyList())

    val clientPayments = remember(payments, loans) {
        val loanIds = loans.map { it.id }.toSet()
        payments.filter { loanIds.contains(it.loanId) }.sortedByDescending { it.paymentDate }
    }

    val latestEvaluation by viewModel.evalRepo.getLatestEvaluationForClient(clientId).collectAsStateWithLifecycle(initialValue = null)
    val evaluationsHistory by viewModel.evalRepo.getEvaluationsForClient(clientId).collectAsStateWithLifecycle(initialValue = emptyList())
    val manualAdjustments by viewModel.evalRepo.getManualAdjustmentsForClient(clientId).collectAsStateWithLifecycle(initialValue = emptyList())
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()

    val canAdjustManually = remember(userSession) {
        val role = userSession.user?.role ?: ""
        role == "ADMINISTRADOR" || role == "SUPERVISOR"
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    LaunchedEffect(clientId) {
        client = viewModel.clientRepo.getClientById(clientId)
        viewModel.evaluateClientNow(clientId)
    }

    val tabs = listOf(
        "Evaluación & Score",
        "Datos Personales",
        "Documentación",
        "Referencias",
        "Préstamos (${loans.size})",
        "Pagos & Gestiones"
    )

    Scaffold(
        topBar = {
            RamaTopBar(
                title = client?.fullName ?: "Expediente Digital",
                subtitle = "ID #${client?.id ?: clientId} • ${client?.zone ?: ""}",
                onBackClick = onBack,
                actions = {
                    if (client != null) {
                        IconButton(
                            onClick = { onNavigate("edit_client/${client!!.id}") },
                            modifier = Modifier.testTag("action_edit_client")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Expediente", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("action_delete_client")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Cliente", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        val currentClient = client
        if (currentClient == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val rec = remember(currentClient, loans, clientPayments, visits) {
                calculateClientRecommendation(currentClient, loans, clientPayments, visits)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 1. HEADER CARD (Main Digital Profile)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentClient.fullName.take(1).uppercase(),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentClient.fullName,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "ID: #${currentClient.id} • ${currentClient.occupation}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                RamaStatusBadge(status = currentClient.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CURP: ${currentClient.curp}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text("INE: ${currentClient.ine} | RFC: ${currentClient.rfc.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Surface(
                                    color = when {
                                        currentClient.punctualityScore >= 85 -> Color(0xFFD1FAE5)
                                        currentClient.punctualityScore >= 70 -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFFEE2E2)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Score: ${currentClient.punctualityScore}/100",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive Quick Actions Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentClient.phone}"))
                                        try { context.startActivity(intent) } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Llamar", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        showWhatsAppDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val uri = "geo:${currentClient.latitude},${currentClient.longitude}?q=${Uri.encode(currentClient.address)}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                        try { context.startActivity(intent) } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Maps", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 2. AUTOMATIC CREDIT RECOMMENDATION ENGINE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = rec.riskLevel.badgeColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = rec.riskLevel.textColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Evaluación & Recomendación Crediticia",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = rec.riskLevel.textColor
                                    )
                                }

                                Surface(
                                    color = rec.riskLevel.textColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = rec.riskLevel.label,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = rec.decisionTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = rec.riskLevel.textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rec.decisionRationale,
                                style = MaterialTheme.typography.bodySmall,
                                color = rec.riskLevel.textColor.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = rec.riskLevel.textColor.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricColumn(
                                    title = "Prob. de Pago",
                                    value = rec.paymentProbabilityLabel,
                                    textColor = rec.riskLevel.textColor
                                )
                                MetricColumn(
                                    title = "Monto Recomendado",
                                    value = if (rec.recommendedAmount > 0) currencyFormat.format(rec.recommendedAmount) else "N/A",
                                    textColor = rec.riskLevel.textColor
                                )
                                MetricColumn(
                                    title = "Plazo Sugerido",
                                    value = rec.recommendedTerm,
                                    textColor = rec.riskLevel.textColor
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onNavigate("new_loan?clientId=${currentClient.id}") },
                                colors = ButtonDefaults.buttonColors(containerColor = rec.riskLevel.textColor, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_new_loan_from_detail"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Otorgar Préstamo Basado en Recomendación", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. EXPEDIENTE TAB BAR
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                // TAB CONTENT
                when (selectedTab) {
                    0 -> {
                        item {
                            ClientEvaluationView(
                                latestEvaluation = latestEvaluation,
                                evaluationsHistory = evaluationsHistory,
                                manualAdjustments = manualAdjustments,
                                canAdjustManually = canAdjustManually,
                                onRecalculateScore = {
                                    viewModel.evaluateClientNow(clientId)
                                },
                                onManualAdjustmentRequested = { newScore, reason, observation ->
                                    viewModel.adjustScoreManually(clientId, newScore, reason, observation)
                                }
                            )
                        }
                    }

                    1 -> {
                        // TAB 1: DATOS PERSONALES & DOMICILIO
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Expediente de Datos Personales", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    HorizontalDivider()

                                    DataRowItem(label = "Nombre Completo", value = currentClient.fullName)
                                    DataRowItem(label = "CURP", value = currentClient.curp)
                                    DataRowItem(label = "INE", value = currentClient.ine)
                                    DataRowItem(label = "RFC", value = currentClient.rfc.ifBlank { "Sin registrar" })
                                    DataRowItem(label = "Fecha Nacimiento", value = currentClient.birthDate.ifBlank { "Sin registrar" })
                                    DataRowItem(label = "Estado Civil", value = currentClient.maritalStatus)
                                    DataRowItem(label = "Ocupación", value = currentClient.occupation)
                                    DataRowItem(label = "Ingresos Mensuales", value = currencyFormat.format(currentClient.monthlyIncome))

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Contacto & Ubicación Domiciliaria", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    HorizontalDivider()

                                    DataRowItem(label = "Teléfono Móvil", value = currentClient.phone)
                                    DataRowItem(label = "Correo Electrónico", value = currentClient.email.ifBlank { "Sin registrar" })
                                    DataRowItem(label = "Dirección", value = currentClient.address)
                                    DataRowItem(label = "Zona Asignada", value = currentClient.zone)

                                    if (!config.mapsEnabled) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                Text(
                                                    text = "Mapas y geolocalización desactivados por el Administrador.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                     color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    } else {
                                        val canViewLocation = viewModel.hasPermission(com.example.core.security.AppPermission.UBICACION_CLIENTE_VER)
                                        val canEditLocation = viewModel.hasPermission(com.example.core.security.AppPermission.UBICACION_CLIENTE_EDITAR)
                                        val canUseNavigation = viewModel.hasPermission(com.example.core.security.AppPermission.NAVEGACION_USAR)

                                        if (canViewLocation) {
                                            DataRowItem(
                                                label = "Coordenadas GPS",
                                                value = "Lat: ${currentClient.latitude}, Lng: ${currentClient.longitude} (Precisión: ${currentClient.gpsAccuracy}m)"
                                            )
                                            if (currentClient.locationCapturedBy.isNotEmpty()) {
                                                Text(
                                                    text = "Actualizado por: ${currentClient.locationCapturedBy} el ${dateFormat.format(Date(currentClient.locationCapturedAt))}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            DataRowItem(label = "Coordenadas GPS", value = "[Protegido - Sin Permiso]")
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (canEditLocation && config.clientLocationEnabled) {
                                            Button(
                                                onClick = {
                                                    capturedLat = currentClient.latitude.toString()
                                                    capturedLng = currentClient.longitude.toString()
                                                    capturedAccuracy = currentClient.gpsAccuracy
                                                    showCaptureLocationDialog = true
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("btn_register_client_location"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.MyLocation, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Registrar / Actualizar Ubicación GPS")
                                            }
                                        }

                                        if (canUseNavigation) {
                                            OutlinedButton(
                                                onClick = {
                                                    if (!config.useExternalNavigation) {
                                                        android.widget.Toast.makeText(context, "El administrador ha desactivado el uso de navegación externa.", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        if (currentClient.latitude == 19.4326 && currentClient.longitude == -99.1332) {
                                                            android.widget.Toast.makeText(context, "El cliente no tiene coordenadas geográficas registradas.", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val success = com.example.core.location.LocationUtils.launchGoogleMapsNavigation(
                                                                context, currentClient.latitude, currentClient.longitude, currentClient.fullName
                                                            )
                                                            if (success) {
                                                                viewModel.logAuditEvent(
                                                                    action = "NAVEGACION_USAR",
                                                                    entityType = "CLIENTE",
                                                                    entityId = currentClient.id.toString(),
                                                                    newValues = "Navegación iniciada a ${currentClient.fullName}"
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("btn_navigate_external_maps"),
                                                shape = RoundedCornerShape(10.dp)
                                             ) {
                                                Icon(Icons.Default.Navigation, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Navegar con Google Maps GPS")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: EXPEDIENTE DIGITAL Y DOCUMENTOS
                        item {
                            val session by viewModel.userSession.collectAsState()
                            val role = session.user?.role ?: "ADMINISTRADOR"
                            val uname = session.user?.username ?: "admin"
                            val uid = session.user?.id ?: 1L

                            com.example.ui.components.DocumentExpedienteSection(
                                client = currentClient,
                                viewModel = viewModel,
                                userRole = role,
                                currentUsername = uname,
                                currentUserId = uid,
                                onNavigate = onNavigate
                            )
                        }
                    }

                    3 -> {
                        // TAB 3: REFERENCIAS
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Referencias Personales y Familiares", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    HorizontalDivider()

                                    if (currentClient.referenceName1.isNotBlank()) {
                                        ReferenceItemCard(
                                            typeLabel = "Referencia Personal 1",
                                            name = currentClient.referenceName1,
                                            phone = currentClient.referencePhone1,
                                            relation = currentClient.referenceRelation1,
                                            context = context
                                        )
                                    } else {
                                        Text("Sin Referencia Personal 1 registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    if (currentClient.referenceName2.isNotBlank()) {
                                        ReferenceItemCard(
                                            typeLabel = "Referencia Familiar 2",
                                            name = currentClient.referenceName2,
                                            phone = currentClient.referencePhone2,
                                            relation = currentClient.referenceRelation2,
                                            context = context
                                        )
                                    } else {
                                        Text("Sin Referencia Familiar 2 registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // TAB 4: HISTORIAL DE PRÉSTAMOS
                        if (loans.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("El cliente no tiene préstamos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(loans) { loan ->
                                Card(
                                    onClick = { onNavigate("loan_detail/${loan.id}") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.RequestQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Préstamo #${loan.id} (${loan.planType.replace("_", " ")})", fontWeight = FontWeight.Bold)
                                            }
                                            RamaStatusBadge(status = loan.status)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Capital: ${currencyFormat.format(loan.capital)}", style = MaterialTheme.typography.bodyMedium)
                                                Text("Total a Pagar: ${currencyFormat.format(loan.totalAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Saldo Restante", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                Text(currencyFormat.format(loan.remainingBalance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    5 -> {
                        // TAB 5: HISTORIAL DE PAGOS & GESTIONES DE COBRANZA
                        item {
                            Text("A. Historial de Pagos Recibidos (${clientPayments.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        if (clientPayments.isEmpty()) {
                            item { Text("No hay pagos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                        } else {
                            items(clientPayments.take(15)) { pay ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Recibo #${pay.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Fecha: ${dateFormat.format(Date(pay.paymentDate))} • Préstamo #${pay.loanId}", fontSize = 11.sp, color = Color.Gray)
                                            Text("Método: ${pay.method}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(currencyFormat.format(pay.amount), fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("B. Visitas & Acuerdos de Cobranza (${visits.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        if (visits.isEmpty()) {
                            item { Text("No hay visitas de cobranza registradas.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                        } else {
                            items(visits) { visit ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Resultado: ${visit.result}", fontWeight = FontWeight.Bold)
                                            Text("Fecha: ${dateFormat.format(Date(visit.visitDate))}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (visit.notes.isNotBlank()) {
                                            Text("Notas: ${visit.notes}", fontSize = 12.sp)
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

    // DOCUMENT PREVIEW DIALOG
    if (previewDocumentUri != null) {
        val (title, uri) = previewDocumentUri!!
        AlertDialog(
            onDismissRequest = { previewDocumentUri = null },
            title = { Text("Documento Digital: $title") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Documento Digital Encriptado", fontWeight = FontWeight.Bold)
                            Text(uri, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { previewDocumentUri = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (showDeleteDialog && client != null) {
        val targetClient = client!!
        val activeLoansCount = loans.count { it.status == "ACTIVO" || it.status == "MOROSO" || it.status == "VENCIDO" }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar expediente de ${targetClient.fullName}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Esta acción eliminará de forma permanente al cliente y sus registros de expediente.")
                    if (activeLoansCount > 0) {
                        Text(
                            "Advertencia: El cliente tiene $activeLoansCount préstamo(s) activo(s) o con saldo pendiente.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClient(targetClient) {
                            showDeleteDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_client_btn")
                ) {
                    Text("Eliminar Definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // WHATSAPP DIALOG
    if (showWhatsAppDialog && client != null) {
        val targetClient = client!!
        val activeLoan = loans.firstOrNull { it.status == "ACTIVO" || it.status == "MOROSO" }
        WhatsAppMessageDialog(
            viewModel = viewModel,
            clientId = targetClient.id,
            clientName = targetClient.fullName,
            clientPhone = targetClient.phone,
            monto = activeLoan?.dailyPayment?.let { currencyFormat.format(it) } ?: "$0.00",
            cuota = activeLoan?.dailyPayment?.let { currencyFormat.format(it) } ?: "$0.00",
            fechaPago = "Hoy",
            diasAtraso = 0,
            saldo = activeLoan?.remainingBalance?.let { currencyFormat.format(it) } ?: "$0.00",
            numeroPrestamo = activeLoan?.let { "PRST-${it.id}" } ?: "PRST-0000",
            defaultTemplateCode = if (activeLoan?.status == "MOROSO") "MORA" else "PAGO_PROXIMO",
            onDismiss = { showWhatsAppDialog = false }
        )
    }

    if (showCaptureLocationDialog && client != null) {
        val currentClient = client!!
        AlertDialog(
            onDismissRequest = { showCaptureLocationDialog = false },
            title = { Text("Registrar Ubicación GPS del Cliente", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Capture las coordenadas geográficas precisas para el expediente de ${currentClient.fullName}.", style = MaterialTheme.typography.bodySmall)
                    
                    Button(
                        onClick = {
                            isCapturing = true
                            captureError = ""
                            // Simulate GPS read with realistic Mexico City centered coordinates
                            capturedLat = String.format(Locale.US, "%.6f", 19.4326 + (Math.random() - 0.5) * 0.01)
                            capturedLng = String.format(Locale.US, "%.6f", -99.1332 + (Math.random() - 0.5) * 0.01)
                            capturedAccuracy = 3.5f + (Math.random() * 3.0).toFloat()
                            isCapturing = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_get_device_gps_coords")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isCapturing) "Obteniendo señal GPS..." else "Obtener Ubicación Actual")
                    }

                    OutlinedTextField(
                        value = capturedLat,
                        onValueChange = { capturedLat = it },
                        label = { Text("Latitud (-90 a 90)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_captured_latitude"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = capturedLng,
                        onValueChange = { capturedLng = it },
                        label = { Text("Longitud (-180 a 180)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_captured_longitude"),
                        singleLine = true
                    )

                    if (capturedLat.isNotEmpty() && capturedLng.isNotEmpty()) {
                        Text(
                            text = "Precisión: ${String.format(Locale.US, "%.1f", capturedAccuracy)} metros • Fuente: Dispositivo GPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (captureError.isNotEmpty()) {
                        Text(
                            text = captureError,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val latVal = capturedLat.toDoubleOrNull()
                        val lngVal = capturedLng.toDoubleOrNull()

                        if (latVal == null || latVal < -90.0 || latVal > 90.0 || latVal == 0.0) {
                            captureError = "Error: Latitud inválida. Debe estar entre -90 y 90 (no puede ser vacía o 0.0)."
                            return@Button
                        }
                        if (lngVal == null || lngVal < -180.0 || lngVal > 180.0 || lngVal == 0.0) {
                            captureError = "Error: Longitud inválida. Debe estar entre -180 y 180 (no puede ser vacía o 0.0)."
                            return@Button
                        }

                        val updated = currentClient.copy(
                            latitude = latVal,
                            longitude = lngVal,
                            gpsAccuracy = capturedAccuracy,
                            locationCapturedAt = System.currentTimeMillis(),
                            locationCapturedBy = userSession.user?.username ?: "admin",
                            locationSource = "Dispositivo GPS",
                            locationReference = currentClient.address
                        )

                        viewModel.updateClient(updated) {
                            client = updated
                            viewModel.logAuditEvent(
                                action = "UBICACION_CLIENTE_EDITAR",
                                entityType = "CLIENTE",
                                entityId = currentClient.id.toString(),
                                prevValues = "Lat: ${currentClient.latitude}, Lng: ${currentClient.longitude}",
                                newValues = "Lat: $latVal, Lng: $lngVal (Precisión: $capturedAccuracy m)"
                            )
                            showCaptureLocationDialog = false
                            android.widget.Toast.makeText(context, "Ubicación del cliente actualizada correctamente", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_captured_location")
                ) {
                    Text("Confirmar y Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaptureLocationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun MetricColumn(title: String, value: String, textColor: Color) {
    Column {
        Text(title, fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
private fun DataRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DocumentCardItem(
    title: String,
    uri: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onView: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (uri != null) MaterialTheme.colorScheme.primary else Color.Gray)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (uri != null) "Digitalizado ✓" else "Sin adjuntar",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uri != null) Color(0xFF16A34A) else Color.Gray
                    )
                }
            }

            OutlinedButton(
                onClick = onView,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(if (uri != null) "Ver Digital" else "Adjuntar", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ReferenceItemCard(
    typeLabel: String,
    name: String,
    phone: String,
    relation: String,
    context: android.content.Context
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(typeLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Parentesco: $relation • Tel: $phone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }
            ) {
                Icon(Icons.Default.Call, contentDescription = "Llamar Referencia", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
