package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClientEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewClientScreen(
    viewModel: MainViewModel,
    editingClientId: Long? = null,
    onBack: () -> Unit
) {
    val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
    val config = systemConfig ?: com.example.data.local.SystemConfigEntity()

    var loadedClient by remember { mutableStateOf<ClientEntity?>(null) }
    var isEditing by remember { mutableStateOf(editingClientId != null) }

    var fullName by remember { mutableStateOf("") }
    var curp by remember { mutableStateOf("") }
    var ine by remember { mutableStateOf("") }
    var rfc by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("1990-05-10") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("Zona Centro") }
    var latitude by remember { mutableStateOf("19.4326") }
    var longitude by remember { mutableStateOf("-99.1332") }
    var maritalStatus by remember { mutableStateOf("Soltero(a)") }
    var occupation by remember { mutableStateOf("Comerciante") }
    var monthlyIncome by remember { mutableStateOf("12000") }

    // References
    var refName1 by remember { mutableStateOf("") }
    var refPhone1 by remember { mutableStateOf("") }
    var refRel1 by remember { mutableStateOf("") }

    var refName2 by remember { mutableStateOf("") }
    var refPhone2 by remember { mutableStateOf("") }
    var refRel2 by remember { mutableStateOf("") }

    // Document URIs
    var photoUri by remember { mutableStateOf<String?>(null) }
    var ineFrontUri by remember { mutableStateOf<String?>(null) }
    var ineBackUri by remember { mutableStateOf<String?>(null) }
    var proofOfAddressUri by remember { mutableStateOf<String?>(null) }
    var housePhotoUri by remember { mutableStateOf<String?>(null) }
    var signatureUri by remember { mutableStateOf<String?>(null) }

    var status by remember { mutableStateOf("ACTIVO") }
    var punctualityScore by remember { mutableIntStateOf(100) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val zones = listOf("Zona Centro", "Zona Norte", "Zona Sur", "Zona Mercado", "Zona Industrial")
    val maritalOptions = listOf("Soltero(a)", "Casado(a)", "Unión Libre", "Divorciado(a)", "Viudo(a)")
    val occupations = listOf("Comerciante", "Empleado Privado", "Servidor Público", "Trabajador Independiente", "Hogar", "Jubilado/Pensionado")
    val statusOptions = listOf("ACTIVO", "MOROSO", "INACTIVO", "LISTA_NEGRA")

    LaunchedEffect(editingClientId) {
        if (editingClientId != null) {
            val c = viewModel.clientRepo.getClientById(editingClientId)
            if (c != null) {
                loadedClient = c
                fullName = c.fullName
                curp = c.curp
                ine = c.ine
                rfc = c.rfc
                birthDate = c.birthDate
                phone = c.phone
                email = c.email
                address = c.address
                zone = c.zone
                latitude = c.latitude.toString()
                longitude = c.longitude.toString()
                maritalStatus = c.maritalStatus
                occupation = c.occupation
                monthlyIncome = c.monthlyIncome.toString()
                refName1 = c.referenceName1
                refPhone1 = c.referencePhone1
                refRel1 = c.referenceRelation1
                refName2 = c.referenceName2
                refPhone2 = c.referencePhone2
                refRel2 = c.referenceRelation2
                photoUri = c.photoUri
                ineFrontUri = c.ineFrontUri
                ineBackUri = c.ineBackUri
                proofOfAddressUri = c.proofOfAddressUri
                housePhotoUri = c.housePhotoUri
                signatureUri = c.signatureUri
                status = c.status
                punctualityScore = c.punctualityScore
            }
        }
    }

    // Pickers for document photos
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }
    val ineFrontPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) ineFrontUri = uri.toString()
    }
    val ineBackPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) ineBackUri = uri.toString()
    }
    val proofPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) proofOfAddressUri = uri.toString()
    }
    val housePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) housePhotoUri = uri.toString()
    }
    val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) signatureUri = uri.toString()
    }

    Scaffold(
        topBar = {
            RamaTopBar(
                title = if (isEditing) "Editar Expediente de Cliente" else "Registro de Nuevo Cliente",
                subtitle = "Expediente Digital • Datos & Documentos",
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }

            // SECTION 1: Personal Data
            SectionCard(title = "1. Datos Personales & Identificación", icon = Icons.Default.Badge) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre Completo del Cliente *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_client_name"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = curp,
                        onValueChange = { curp = it.uppercase() },
                        label = { Text("CURP *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_client_curp"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ine,
                        onValueChange = { ine = it },
                        label = { Text("Clave INE *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_client_ine"),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = rfc,
                        onValueChange = { rfc = it.uppercase() },
                        label = { Text("RFC (Opcional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text("Fecha Nac. (AAAA-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Dropdowns for Marital & Occupation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SimpleDropdown(
                        label = "Estado Civil",
                        options = maritalOptions,
                        selected = maritalStatus,
                        onSelect = { maritalStatus = it },
                        modifier = Modifier.weight(1f)
                    )
                    SimpleDropdown(
                        label = "Ocupación",
                        options = occupations,
                        selected = occupation,
                        onSelect = { occupation = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = monthlyIncome,
                    onValueChange = { monthlyIncome = it },
                    label = { Text("Ingresos Mensuales Estimados ($) *") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // SECTION 2: Contact & Address
            SectionCard(title = "2. Contacto & Domicilio GPS", icon = Icons.Default.HomeWork) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono Móvil *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_client_phone"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección Completa (Calle, No, Col) *") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_client_address"),
                    minLines = 2
                )

                SimpleDropdown(
                    label = "Zona Asignada",
                    options = zones,
                    selected = zone,
                    onSelect = { zone = it },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!config.mapsEnabled) {
                    Text("Coordenadas GPS (Desactivado administrativamente)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Coordenadas GPS de Ubicación", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    
                    if (config.clientLocationEnabled) {
                        Button(
                            onClick = {
                                // Simulate high precision GPS read centered in Mexico City
                                latitude = String.format(java.util.Locale.US, "%.6f", 19.4326 + (Math.random() - 0.5) * 0.01)
                                longitude = String.format(java.util.Locale.US, "%.6f", -99.1332 + (Math.random() - 0.5) * 0.01)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("btn_autodetect_client_coords"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detectar Coordenadas GPS del Dispositivo")
                        }
                    } else {
                        Text("El registro de ubicación GPS para clientes nuevos está desactivado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = { Text("Latitud") },
                            modifier = Modifier.weight(1f).testTag("input_new_client_latitude"),
                            singleLine = true,
                            enabled = config.clientLocationEnabled
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = { Text("Longitud") },
                            modifier = Modifier.weight(1f).testTag("input_new_client_longitude"),
                            singleLine = true,
                            enabled = config.clientLocationEnabled
                        )
                    }
                }
            }

            // SECTION 3: References
            SectionCard(title = "3. Referencias Personales y Familiares", icon = Icons.Default.Group) {
                Text("Referencia 1 (Personal)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = refName1,
                    onValueChange = { refName1 = it },
                    label = { Text("Nombre Completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = refPhone1,
                        onValueChange = { refPhone1 = it },
                        label = { Text("Teléfono") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = refRel1,
                        onValueChange = { refRel1 = it },
                        label = { Text("Parentesco / Relación") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Referencia 2 (Familiar)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = refName2,
                    onValueChange = { refName2 = it },
                    label = { Text("Nombre Completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = refPhone2,
                        onValueChange = { refPhone2 = it },
                        label = { Text("Teléfono") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = refRel2,
                        onValueChange = { refRel2 = it },
                        label = { Text("Parentesco / Relación") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // SECTION 4: Digital Expediente Documents
            SectionCard(title = "4. Documentación Digital & Captura", icon = Icons.Default.CameraAlt) {
                Text(
                    text = "Seleccione o tome fotografía para adjuntar los expedientes digitales obligatorios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DocumentUploadRow(
                    title = "Fotografía de Perfil",
                    subtitle = if (photoUri != null) "Cargado ✓" else "Sin adjuntar",
                    uri = photoUri,
                    onUpload = { photoPicker.launch("image/*") },
                    onSimulateCamera = { photoUri = "content://simulated_camera_photo_${System.currentTimeMillis()}.jpg" }
                )

                DocumentUploadRow(
                    title = "INE Frente",
                    subtitle = if (ineFrontUri != null) "Cargado ✓" else "Sin adjuntar",
                    uri = ineFrontUri,
                    onUpload = { ineFrontPicker.launch("image/*") },
                    onSimulateCamera = { ineFrontUri = "content://simulated_ine_front_${System.currentTimeMillis()}.jpg" }
                )

                DocumentUploadRow(
                    title = "INE Reverso",
                    subtitle = if (ineBackUri != null) "Cargado ✓" else "Sin adjuntar",
                    uri = ineBackUri,
                    onUpload = { ineBackPicker.launch("image/*") },
                    onSimulateCamera = { ineBackUri = "content://simulated_ine_back_${System.currentTimeMillis()}.jpg" }
                )

                DocumentUploadRow(
                    title = "Comprobante de Domicilio",
                    subtitle = if (proofOfAddressUri != null) "Cargado ✓" else "Sin adjuntar",
                    uri = proofOfAddressUri,
                    onUpload = { proofPicker.launch("image/*") },
                    onSimulateCamera = { proofOfAddressUri = "content://simulated_proof_${System.currentTimeMillis()}.jpg" }
                )

                DocumentUploadRow(
                    title = "Fotografía del Domicilio",
                    subtitle = if (housePhotoUri != null) "Cargado ✓" else "Sin adjuntar",
                    uri = housePhotoUri,
                    onUpload = { housePicker.launch("image/*") },
                    onSimulateCamera = { housePhotoUri = "content://simulated_house_${System.currentTimeMillis()}.jpg" }
                )

                DocumentUploadRow(
                    title = "Firma Digital del Cliente",
                    subtitle = if (signatureUri != null) "Cargado ✓" else "Sin adjuntar",
                    uri = signatureUri,
                    onUpload = { signaturePicker.launch("image/*") },
                    onSimulateCamera = { signatureUri = "content://simulated_signature_${System.currentTimeMillis()}.png" }
                )
            }

            if (isEditing) {
                SectionCard(title = "5. Estatus & Scoring", icon = Icons.Default.Shield) {
                    SimpleDropdown(
                        label = "Estatus del Cliente",
                        options = statusOptions,
                        selected = status,
                        onSelect = { status = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Puntualidad (0 a 100 pts): $punctualityScore", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = punctualityScore.toFloat(),
                        onValueChange = { punctualityScore = it.toInt() },
                        valueRange = 0f..100f
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "Por favor ingrese el Nombre Completo."
                        return@Button
                    }
                    if (curp.isBlank() || ine.isBlank()) {
                        errorMessage = "Por favor ingrese CURP e INE obligatorios."
                        return@Button
                    }
                    if (phone.isBlank() || address.isBlank()) {
                        errorMessage = "Por favor ingrese Teléfono y Dirección."
                        return@Button
                    }

                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()

                    if (config.mapsEnabled) {
                        if (lat == null || lat < -90.0 || lat > 90.0 || lat == 0.0) {
                            errorMessage = "Latitud GPS inválida. Debe ser un número decimal entre -90 y 90 (no puede ser vacía o 0.0)."
                            return@Button
                        }
                        if (lng == null || lng < -180.0 || lng > 180.0 || lng == 0.0) {
                            errorMessage = "Longitud GPS inválida. Debe ser un número decimal entre -180 y 180 (no puede ser vacía o 0.0)."
                            return@Button
                        }
                    }

                    val finalLat = lat ?: 19.4326
                    val finalLng = lng ?: -99.1332
                    val inc = monthlyIncome.toDoubleOrNull() ?: 12000.0

                    val clientToSave = ClientEntity(
                        id = loadedClient?.id ?: 0L,
                        fullName = fullName.trim(),
                        curp = curp.trim(),
                        ine = ine.trim(),
                        rfc = rfc.trim(),
                        birthDate = birthDate.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        zone = zone,
                        latitude = finalLat,
                        longitude = finalLng,
                        gpsAccuracy = loadedClient?.gpsAccuracy ?: 5.0f,
                        locationCapturedAt = loadedClient?.locationCapturedAt ?: System.currentTimeMillis(),
                        locationCapturedBy = loadedClient?.locationCapturedBy ?: (viewModel.userSession.value.user?.username ?: "admin"),
                        locationSource = loadedClient?.locationSource ?: "Registro Inicial",
                        locationReference = loadedClient?.locationReference ?: address.trim(),
                        maritalStatus = maritalStatus,
                        occupation = occupation,
                        monthlyIncome = inc,
                        referenceName1 = refName1.trim(),
                        referencePhone1 = refPhone1.trim(),
                        referenceRelation1 = refRel1.trim(),
                        referenceName2 = refName2.trim(),
                        referencePhone2 = refPhone2.trim(),
                        referenceRelation2 = refRel2.trim(),
                        photoUri = photoUri,
                        ineFrontUri = ineFrontUri,
                        ineBackUri = ineBackUri,
                        proofOfAddressUri = proofOfAddressUri,
                        housePhotoUri = housePhotoUri,
                        signatureUri = signatureUri,
                        status = status,
                        punctualityScore = punctualityScore,
                        createdAt = loadedClient?.createdAt ?: System.currentTimeMillis()
                    )

                    if (isEditing) {
                        val prevLat = loadedClient?.latitude ?: 0.0
                        val prevLng = loadedClient?.longitude ?: 0.0
                        viewModel.updateClient(clientToSave) {
                            viewModel.logAuditEvent(
                                action = "CLIENTE_EDICION",
                                entityType = "CLIENTE",
                                entityId = clientToSave.id.toString(),
                                prevValues = "Lat: $prevLat, Lng: $prevLng",
                                newValues = "Lat: $finalLat, Lng: $finalLng"
                            )
                            onBack()
                        }
                    } else {
                        viewModel.createClient(clientToSave) {
                            viewModel.logAuditEvent(
                                action = "CLIENTE_CREACION",
                                entityType = "CLIENTE",
                                entityId = "NUEVO",
                                prevValues = null,
                                newValues = "Nombre: ${clientToSave.fullName}, Lat: $finalLat, Lng: $finalLng"
                            )
                            onBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_client"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Guardar Cambios en Expediente" else "Registrar Cliente y Crear Expediente",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun DocumentUploadRow(
    title: String,
    subtitle: String,
    uri: String?,
    onUpload: () -> Unit,
    onSimulateCamera: () -> Unit
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (uri != null) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uri != null) Icons.Default.CheckCircle else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (uri != null) Color(0xFF065F46) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (uri != null) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSimulateCamera, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camara", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onUpload, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Galeria", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
