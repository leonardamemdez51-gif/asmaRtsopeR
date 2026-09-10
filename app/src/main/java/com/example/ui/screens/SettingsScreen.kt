package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SystemConfigEntity
import com.example.ui.MainViewModel
import com.example.ui.components.RamaTopBar
import java.text.SimpleDateFormat
import java.util.*

enum class ConfigCategory(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    EMPRESA("Empresa", Icons.Default.Business, "Datos corporativos, moneda y formatos"),
    PARAMETROS_GENERALES("Parámetros Generales", Icons.Default.Settings, "Configuración global del sistema"),
    PLANES_PRESTAMO("Planes de Préstamo", Icons.Default.Assignment, "Planes 20 y 30 días, reglas de cobro"),
    INTERESES_MORA("Intereses y Mora", Icons.Default.TrendingUp, "Cálculo de moratorios y recargos"),
    USUARIOS_SEGURIDAD("Usuarios y Seguridad", Icons.Default.Security, "Políticas de contraseñas y sesiones"),
    RUTAS_SUCURSALES("Rutas y Sucursales", Icons.Default.AltRoute, "Asignación geográfica y sucursales"),
    FONDOS("Fondos", Icons.Default.AccountBalance, "Bolsas de capital y cajas chicas"),
    NOTIFICACIONES("Notificaciones", Icons.Default.Notifications, "Alertas push, SMS y correo"),
    WHATSAPP("WhatsApp", Icons.Default.Phone, "Integración API y plantillas de recibo"),
    GOOGLE_MAPS("Google Maps", Icons.Default.LocationOn, "Rastreo GPS de cobradores y mapa"),
    RESPALDOS("Respaldos", Icons.Default.Backup, "Copias de seguridad y restauración"),
    AUDITORIA("Auditoría", Icons.Default.FactCheck, "Histórico de acciones y retención de logs"),
    NUMERACIONES("Numeraciones Automáticas", Icons.Default.Numbers, "Folios de préstamos, recibos y clientes"),
    APARIENCIA("Apariencia", Icons.Default.Palette, "Modo oscuro y temas visuales"),
    EVALUACION_SCORE("Motor de Evaluación & Score", Icons.Default.QueryStats, "Ponderaciones, rangos, alertas y ajustes de score")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToUserManagement: () -> Unit
) {
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val currentConfigState by viewModel.systemConfig.collectAsStateWithLifecycle()

    // Verification of Administrator role
    val userRole = (userSession.user?.role ?: sessionState.role).uppercase()
    val isAdmin = userRole == "ADMINISTRADOR"

    var selectedCategory by remember { mutableStateOf(ConfigCategory.EMPRESA) }

    val config = currentConfigState ?: SystemConfigEntity()

    Scaffold(
        topBar = {
            RamaTopBar(
                title = "Configuración del Sistema",
                subtitle = if (isAdmin) "Módulo Administrador | RAMA Microfinanzas" else "Acceso Restringido",
                onBackClick = onBack
            )
        }
    ) { padding ->
        if (!isAdmin) {
            // Access Denied Screen for non-administrators
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Acceso Restringido",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Acceso Restringido",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "El módulo de Configuración del Sistema está reservado exclusivamente para usuarios con rol ADMINISTRADOR.\n\nTu rol actual es: $userRole",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp)
                        .testTag("btn_back_unauthorized"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regresar")
                }
            }
        } else {
            // Administrator Settings Layout
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val isWideScreen = maxWidth >= 600.dp

                if (isWideScreen) {
                    // Two-panel layout for Wide screens / Tablets
                    Row(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "CATEGORÍAS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )

                                ConfigCategory.values().forEach { category ->
                                    val isSelected = category == selectedCategory
                                    NavigationDrawerItem(
                                        icon = {
                                            Icon(
                                                imageVector = category.icon,
                                                contentDescription = category.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = category.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        },
                                        selected = isSelected,
                                        onClick = { selectedCategory = category },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("nav_item_${category.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(20.dp)
                        ) {
                            CategoryContentPanel(
                                category = selectedCategory,
                                config = config,
                                viewModel = viewModel,
                                onNavigateToUserManagement = onNavigateToUserManagement
                            )
                        }
                    }
                } else {
                    // Compact Single Panel with top Category Horizontal Chips / Scrollable Tabs
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConfigCategory.values().forEach { category ->
                                val isSelected = category == selectedCategory
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category.title) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("chip_${category.name.lowercase()}")
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            CategoryContentPanel(
                                category = selectedCategory,
                                config = config,
                                viewModel = viewModel,
                                onNavigateToUserManagement = onNavigateToUserManagement
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryContentPanel(
    category: ConfigCategory,
    config: SystemConfigEntity,
    viewModel: MainViewModel,
    onNavigateToUserManagement: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Dynamic Form depending on category
        when (category) {
            ConfigCategory.EMPRESA -> CategoryEmpresaForm(config, viewModel)
            ConfigCategory.PARAMETROS_GENERALES -> CategoryParametrosGeneralesForm(config, viewModel)
            ConfigCategory.PLANES_PRESTAMO -> CategoryPlanesPrestamoForm(config, viewModel)
            ConfigCategory.INTERESES_MORA -> CategoryInteresesMoraForm(config, viewModel)
            ConfigCategory.USUARIOS_SEGURIDAD -> CategoryUsuariosSeguridadForm(config, viewModel, onNavigateToUserManagement)
            ConfigCategory.RUTAS_SUCURSALES -> CategoryRutasSucursalesForm(config, viewModel)
            ConfigCategory.FONDOS -> CategoryFondosForm(config, viewModel)
            ConfigCategory.NOTIFICACIONES -> CategoryNotificacionesForm(config, viewModel)
            ConfigCategory.WHATSAPP -> CategoryWhatsAppForm(config, viewModel)
            ConfigCategory.GOOGLE_MAPS -> CategoryGoogleMapsForm(config, viewModel)
            ConfigCategory.RESPALDOS -> CategoryRespaldosForm(config, viewModel)
            ConfigCategory.AUDITORIA -> CategoryAuditoriaForm(config, viewModel)
            ConfigCategory.NUMERACIONES -> CategoryNumeracionesForm(config, viewModel)
            ConfigCategory.APARIENCIA -> CategoryAparienciaForm(config, viewModel)
            ConfigCategory.EVALUACION_SCORE -> CategoryEvaluacionScoreForm(viewModel)
        }
    }
}

// -----------------------------------------------------------------------------
// 1. EMPRESA
// -----------------------------------------------------------------------------
@Composable
private fun CategoryEmpresaForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var companyName by remember(config) { mutableStateOf(config.companyName) }
    var businessName by remember(config) { mutableStateOf(config.businessName) }
    var rfc by remember(config) { mutableStateOf(config.rfc) }
    var address by remember(config) { mutableStateOf(config.address) }
    var phone by remember(config) { mutableStateOf(config.phone) }
    var email by remember(config) { mutableStateOf(config.email) }
    var logoUrl by remember(config) { mutableStateOf(config.logoUrl) }
    var currency by remember(config) { mutableStateOf(config.currency) }
    var timeZone by remember(config) { mutableStateOf(config.timeZone) }
    var dateFormat by remember(config) { mutableStateOf(config.dateFormat) }
    var timeFormat by remember(config) { mutableStateOf(config.timeFormat) }

    OutlinedTextField(
        value = companyName,
        onValueChange = { companyName = it },
        label = { Text("Nombre Comercial de la Empresa") },
        modifier = Modifier.fillMaxWidth().testTag("input_company_name"),
        singleLine = true
    )

    OutlinedTextField(
        value = businessName,
        onValueChange = { businessName = it },
        label = { Text("Razón Social (Fiscal)") },
        modifier = Modifier.fillMaxWidth().testTag("input_business_name"),
        singleLine = true
    )

    OutlinedTextField(
        value = rfc,
        onValueChange = { rfc = it },
        label = { Text("RFC / Identificación Fiscal") },
        modifier = Modifier.fillMaxWidth().testTag("input_rfc"),
        singleLine = true
    )

    OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        label = { Text("Dirección de la Casa Matriz") },
        modifier = Modifier.fillMaxWidth().testTag("input_address")
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Teléfono de Atención") },
            modifier = Modifier.weight(1f).testTag("input_phone"),
            singleLine = true
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.weight(1f).testTag("input_email"),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = logoUrl,
        onValueChange = { logoUrl = it },
        label = { Text("URL / Ruta del Logo Oficial") },
        modifier = Modifier.fillMaxWidth().testTag("input_logo_url"),
        singleLine = true
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it },
            label = { Text("Moneda Principal") },
            modifier = Modifier.weight(1f).testTag("input_currency"),
            singleLine = true
        )
        OutlinedTextField(
            value = timeZone,
            onValueChange = { timeZone = it },
            label = { Text("Zona Horaria") },
            modifier = Modifier.weight(1f).testTag("input_timezone"),
            singleLine = true
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = dateFormat,
            onValueChange = { dateFormat = it },
            label = { Text("Formato de Fecha") },
            modifier = Modifier.weight(1f).testTag("input_date_format"),
            singleLine = true
        )
        OutlinedTextField(
            value = timeFormat,
            onValueChange = { timeFormat = it },
            label = { Text("Formato de Hora") },
            modifier = Modifier.weight(1f).testTag("input_time_format"),
            singleLine = true
        )
    }

    SaveButton(
        testTag = "save_empresa_button",
        onSave = {
            val updated = config.copy(
                companyName = companyName,
                businessName = businessName,
                rfc = rfc,
                address = address,
                phone = phone,
                email = email,
                logoUrl = logoUrl,
                currency = currency,
                timeZone = timeZone,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            )
            viewModel.updateSystemConfig(updated, "Empresa")
        }
    )
}

// -----------------------------------------------------------------------------
// 2. PARÁMETROS GENERALES
// -----------------------------------------------------------------------------
@Composable
private fun CategoryParametrosGeneralesForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var defaultZone by remember(config) { mutableStateOf(config.defaultZone) }
    var systemLanguage by remember(config) { mutableStateOf(config.systemLanguage) }
    var strictMode by remember(config) { mutableStateOf(config.strictMode) }
    var autoSyncOffline by remember(config) { mutableStateOf(config.autoSyncOffline) }
    var allowNegativeBalance by remember(config) { mutableStateOf(config.allowNegativeBalance) }

    OutlinedTextField(
        value = defaultZone,
        onValueChange = { defaultZone = it },
        label = { Text("Zona Geográfica Predeterminada") },
        modifier = Modifier.fillMaxWidth().testTag("input_default_zone"),
        singleLine = true
    )

    OutlinedTextField(
        value = systemLanguage,
        onValueChange = { systemLanguage = it },
        label = { Text("Idioma Predeterminado del Sistema") },
        modifier = Modifier.fillMaxWidth().testTag("input_language"),
        singleLine = true
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Modo Estricto de Validación",
                subtitle = "Verifica geolocalización e imágenes obligatorias al crear o cobrar",
                checked = strictMode,
                onCheckedChange = { strictMode = it },
                testTag = "switch_strict_mode"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Sincronización Automática Offline",
                subtitle = "Envía operaciones registradas localmente en cuanto se detecte conexión",
                checked = autoSyncOffline,
                onCheckedChange = { autoSyncOffline = it },
                testTag = "switch_auto_sync"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Permitir Saldo Negativo en Caja",
                subtitle = "Permite registrar egresos aunque superen el efectivo acumulado en turno",
                checked = allowNegativeBalance,
                onCheckedChange = { allowNegativeBalance = it },
                testTag = "switch_negative_balance"
            )
        }
    }

    SaveButton(
        testTag = "save_general_button",
        onSave = {
            val updated = config.copy(
                defaultZone = defaultZone,
                systemLanguage = systemLanguage,
                strictMode = strictMode,
                autoSyncOffline = autoSyncOffline,
                allowNegativeBalance = allowNegativeBalance
            )
            viewModel.updateSystemConfig(updated, "Parámetros Generales")
        }
    )
}

// -----------------------------------------------------------------------------
// 3. PLANES DE PRÉSTAMO
// -----------------------------------------------------------------------------
@Composable
private fun CategoryPlanesPrestamoForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var plan20Rate by remember(config) { mutableStateOf((config.plan20InterestRate * 100).toString()) }
    var plan30Rate by remember(config) { mutableStateOf((config.plan30InterestRate * 100).toString()) }
    var paymentFrequency by remember(config) { mutableStateOf(config.paymentFrequency) }
    var firstPaymentNextDay by remember(config) { mutableStateOf(config.firstPaymentNextDay) }
    var allowSkipSundays by remember(config) { mutableStateOf(config.allowSkipSundays) }
    var enableLateFees by remember(config) { mutableStateOf(config.enableLateFees) }
    var lateFeePercentage by remember(config) { mutableStateOf((config.lateFeePercentage * 100).toString()) }
    var maxOverdueDays by remember(config) { mutableStateOf(config.maxOverdueDays.toString()) }
    var allowRenewals by remember(config) { mutableStateOf(config.allowRenewals) }
    var allowRenewalPlusLoan by remember(config) { mutableStateOf(config.allowRenewalPlusLoan) }
    var allowMultipleLoans by remember(config) { mutableStateOf(config.allowMultipleLoans) }

    var customPlans by remember(config) {
        mutableStateOf(
            listOf(
                "Plan 20 días" to "${config.plan20InterestRate * 100}%",
                "Plan 30 días" to "${config.plan30InterestRate * 100}%"
            )
        )
    }

    var showNewPlanDialog by remember { mutableStateOf(false) }
    var newPlanName by remember { mutableStateOf("") }
    var newPlanDays by remember { mutableStateOf("15") }
    var newPlanRate by remember { mutableStateOf("15") }

    Text("Tasas Base de Planes Preconfigurados", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = plan20Rate,
            onValueChange = { plan20Rate = it },
            label = { Text("Interés Plan 20 Días (%)") },
            modifier = Modifier.weight(1f).testTag("input_plan20_rate"),
            singleLine = true
        )

        OutlinedTextField(
            value = plan30Rate,
            onValueChange = { plan30Rate = it },
            label = { Text("Interés Plan 30 Días (%)") },
            modifier = Modifier.weight(1f).testTag("input_plan30_rate"),
            singleLine = true
        )
    }

    // List of Active Loan Plans
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Catálogo de Planes Activos", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                OutlinedButton(
                    onClick = { showNewPlanDialog = true },
                    modifier = Modifier.testTag("btn_add_plan"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Plan", fontSize = 12.sp)
                }
            }

            customPlans.forEach { (name, rate) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(name, fontWeight = FontWeight.Bold)
                        }
                        Text("Tasa: $rate", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    Text("Reglas de Cobro y Operación de Préstamos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Pago Diario Habilitado",
                subtitle = "Frecuencia de amortización por cuotas diarias",
                checked = paymentFrequency == "PAGO_DIARIO",
                onCheckedChange = { paymentFrequency = if (it) "PAGO_DIARIO" else "SEMANAL" },
                testTag = "switch_daily_payment"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Primer Pago al Día Siguiente",
                subtitle = "El primer vencimiento se establece 24h después del desembolso",
                checked = firstPaymentNextDay,
                onCheckedChange = { firstPaymentNextDay = it },
                testTag = "switch_first_payment_next_day"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Omitir Domingos",
                subtitle = "Excluye los domingos al calcular el calendario de pagos",
                checked = allowSkipSundays,
                onCheckedChange = { allowSkipSundays = it },
                testTag = "switch_skip_sundays"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Habilitar Cobro de Mora",
                subtitle = "Aplica recargos por cuota no pagada en su fecha de vencimiento",
                checked = enableLateFees,
                onCheckedChange = { enableLateFees = it },
                testTag = "switch_enable_late_fees"
            )

            if (enableLateFees) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = lateFeePercentage,
                        onValueChange = { lateFeePercentage = it },
                        label = { Text("Porcentaje de Mora (%)") },
                        modifier = Modifier.weight(1f).testTag("input_late_fee_pct"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxOverdueDays,
                        onValueChange = { maxOverdueDays = it },
                        label = { Text("Máx. Días de Atraso") },
                        modifier = Modifier.weight(1f).testTag("input_max_overdue_days"),
                        singleLine = true
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Permitir Renovaciones de Préstamo",
                subtitle = "Autoriza liquidar préstamo previo al tramitar un nuevo ciclo",
                checked = allowRenewals,
                onCheckedChange = { allowRenewals = it },
                testTag = "switch_allow_renewals"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Renovación + Préstamo Adicional",
                subtitle = "Permite desembolsar efectivo neto sumado a la renovación",
                checked = allowRenewalPlusLoan,
                onCheckedChange = { allowRenewalPlusLoan = it },
                testTag = "switch_renewal_plus_loan"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Múltiples Préstamos Simultáneos",
                subtitle = "Un cliente puede tener más de 1 préstamo activo con buen score",
                checked = allowMultipleLoans,
                onCheckedChange = { allowMultipleLoans = it },
                testTag = "switch_multiple_loans"
            )
        }
    }

    if (showNewPlanDialog) {
        AlertDialog(
            onDismissRequest = { showNewPlanDialog = false },
            title = { Text("Agregar Nuevo Plan de Préstamo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlanName,
                        onValueChange = { newPlanName = it },
                        label = { Text("Nombre del Plan (Ej. Plan 15 días)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPlanDays,
                        onValueChange = { newPlanDays = it },
                        label = { Text("Plazo en Días") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPlanRate,
                        onValueChange = { newPlanRate = it },
                        label = { Text("Tasa de Interés (%)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlanName.isNotBlank()) {
                            customPlans = customPlans + (newPlanName to "$newPlanRate%")
                            showNewPlanDialog = false
                            newPlanName = ""
                        }
                    }
                ) { Text("Agregar") }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlanDialog = false }) { Text("Cancelar") }
            }
        )
    }

    val p20 = (plan20Rate.toDoubleOrNull() ?: 20.0) / 100.0
    val p30 = (plan30Rate.toDoubleOrNull() ?: 30.0) / 100.0
    val feePct = (lateFeePercentage.toDoubleOrNull() ?: 5.0) / 100.0
    val maxDays = maxOverdueDays.toIntOrNull() ?: 15
    
    val isCriticalChange = p20 != config.plan20InterestRate ||
            p30 != config.plan30InterestRate ||
            feePct != config.lateFeePercentage ||
            maxDays != config.maxOverdueDays

    if (isCriticalChange) {
        CriticalSaveButton(
            testTag = "save_prestamos_button",
            onSave = {
                val updated = config.copy(
                    plan20InterestRate = p20,
                    plan30InterestRate = p30,
                    paymentFrequency = paymentFrequency,
                    firstPaymentNextDay = firstPaymentNextDay,
                    allowSkipSundays = allowSkipSundays,
                    enableLateFees = enableLateFees,
                    lateFeePercentage = feePct,
                    maxOverdueDays = maxDays,
                    allowRenewals = allowRenewals,
                    allowRenewalPlusLoan = allowRenewalPlusLoan,
                    allowMultipleLoans = allowMultipleLoans
                )
                viewModel.updateSystemConfig(updated, "Planes de Préstamo")
            },
            previousValue = "P20: ${config.plan20InterestRate}, P30: ${config.plan30InterestRate}, Mora: ${config.lateFeePercentage}",
            newValue = "P20: $p20, P30: $p30, Mora: $feePct",
            warning = "Modificar las tasas de interés o de mora de los planes predeterminados puede afectar operaciones futuras. Verifique que los valores ingresados sean correctos."
        )
    } else {
        SaveButton(
            testTag = "save_prestamos_button",
            onSave = {
                val updated = config.copy(
                    plan20InterestRate = p20,
                    plan30InterestRate = p30,
                    paymentFrequency = paymentFrequency,
                    firstPaymentNextDay = firstPaymentNextDay,
                    allowSkipSundays = allowSkipSundays,
                    enableLateFees = enableLateFees,
                    lateFeePercentage = feePct,
                    maxOverdueDays = maxDays,
                    allowRenewals = allowRenewals,
                    allowRenewalPlusLoan = allowRenewalPlusLoan,
                    allowMultipleLoans = allowMultipleLoans
                )
                viewModel.updateSystemConfig(updated, "Planes de Préstamo")
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 4. INTERESES Y MORA
// -----------------------------------------------------------------------------
@Composable
private fun CategoryInteresesMoraForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var lateFeeType by remember(config) { mutableStateOf(config.lateFeeType) }
    var graceDays by remember(config) { mutableStateOf(config.graceDays.toString()) }
    var interestCalculationMethod by remember(config) { mutableStateOf(config.interestCalculationMethod) }
    var penaltyCapPercentage by remember(config) { mutableStateOf((config.penaltyCapPercentage * 100).toString()) }

    OutlinedTextField(
        value = lateFeeType,
        onValueChange = { lateFeeType = it },
        label = { Text("Tipo de Cálculo de Mora (PORCENTAJE_DIARIO / FIJO / PORCENTAJE_CUOTA)") },
        modifier = Modifier.fillMaxWidth().testTag("input_late_fee_type"),
        singleLine = true
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = graceDays,
            onValueChange = { graceDays = it },
            label = { Text("Días de Gracia sin Recargo") },
            modifier = Modifier.weight(1f).testTag("input_grace_days"),
            singleLine = true
        )

        OutlinedTextField(
            value = penaltyCapPercentage,
            onValueChange = { penaltyCapPercentage = it },
            label = { Text("Tope Máximo de Recargo (%)") },
            modifier = Modifier.weight(1f).testTag("input_penalty_cap"),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = interestCalculationMethod,
        onValueChange = { interestCalculationMethod = it },
        label = { Text("Método de Cálculo de Interés (INTERES_SIMPLE / FLAT_RATE)") },
        modifier = Modifier.fillMaxWidth().testTag("input_interest_method"),
        singleLine = true
    )

    val isCriticalChange = lateFeeType != config.lateFeeType ||
            (graceDays.toIntOrNull() ?: 1) != config.graceDays ||
            interestCalculationMethod != config.interestCalculationMethod ||
            (penaltyCapPercentage.toDoubleOrNull() ?: 50.0) / 100.0 != config.penaltyCapPercentage

    if (isCriticalChange) {
        CriticalSaveButton(
            testTag = "save_intereses_button",
            onSave = {
                val gDays = graceDays.toIntOrNull() ?: 1
                val pCap = (penaltyCapPercentage.toDoubleOrNull() ?: 50.0) / 100.0

                val updated = config.copy(
                    lateFeeType = lateFeeType,
                    graceDays = gDays,
                    interestCalculationMethod = interestCalculationMethod,
                    penaltyCapPercentage = pCap
                )
                viewModel.updateSystemConfig(updated, "Intereses y Mora")
            },
            previousValue = "Tipo Mora: ${config.lateFeeType}, Tope: ${config.penaltyCapPercentage}",
            newValue = "Tipo Mora: $lateFeeType, Tope: ${(penaltyCapPercentage.toDoubleOrNull() ?: 50.0) / 100.0}",
            warning = "Estos cambios afectarán el cálculo de intereses y mora en NUEVOS préstamos, pero no deberían afectar a los históricos. Asegúrese de que las reglas de negocio soportan estos cambios."
        )
    } else {
        SaveButton(
            testTag = "save_intereses_button",
            onSave = {
                val gDays = graceDays.toIntOrNull() ?: 1
                val pCap = (penaltyCapPercentage.toDoubleOrNull() ?: 50.0) / 100.0

                val updated = config.copy(
                    lateFeeType = lateFeeType,
                    graceDays = gDays,
                    interestCalculationMethod = interestCalculationMethod,
                    penaltyCapPercentage = pCap
                )
                viewModel.updateSystemConfig(updated, "Intereses y Mora")
            }
        )
    }
}

// -----------------------------------------------------------------------------
// 5. USUARIOS Y SEGURIDAD
// -----------------------------------------------------------------------------
@Composable
private fun CategoryUsuariosSeguridadForm(
    config: SystemConfigEntity,
    viewModel: MainViewModel,
    onNavigateToUserManagement: () -> Unit
) {
    var sessionTimeout by remember(config) { mutableStateOf(config.sessionTimeoutMinutes.toString()) }
    var autoLockEnabled by remember(config) { mutableStateOf(config.autoLockEnabled) }
    var maxLoginAttempts by remember(config) { mutableStateOf(config.maxLoginAttempts.toString()) }
    var minPasswordLength by remember(config) { mutableStateOf(config.minPasswordLength.toString()) }
    var requireComplexity by remember(config) { mutableStateOf(config.requirePasswordComplexity) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = sessionTimeout,
            onValueChange = { sessionTimeout = it },
            label = { Text("Expiración de Sesión (Minutos)") },
            modifier = Modifier.weight(1f).testTag("input_session_timeout"),
            singleLine = true
        )

        OutlinedTextField(
            value = maxLoginAttempts,
            onValueChange = { maxLoginAttempts = it },
            label = { Text("Intentos Fallidos Permitidos") },
            modifier = Modifier.weight(1f).testTag("input_max_attempts"),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = minPasswordLength,
        onValueChange = { minPasswordLength = it },
        label = { Text("Longitud Mínima de Contraseña") },
        modifier = Modifier.fillMaxWidth().testTag("input_min_pass_len"),
        singleLine = true
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Bloqueo Automático por Intentos Fallidos",
                subtitle = "Inhabilita la cuenta temporalmente por 60s al superar límite",
                checked = autoLockEnabled,
                onCheckedChange = { autoLockEnabled = it },
                testTag = "switch_auto_lock"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Exigir Complejidad de Contraseña",
                subtitle = "Requiere mayúsculas, números y caracteres especiales",
                checked = requireComplexity,
                onCheckedChange = { requireComplexity = it },
                testTag = "switch_require_complexity"
            )
        }
    }

    OutlinedButton(
        onClick = onNavigateToUserManagement,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_user_management_security"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Group, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Administrar Cuentas, Roles y Permisos")
    }

    SaveButton(
        testTag = "save_seguridad_button",
        onSave = {
            val timeout = sessionTimeout.toIntOrNull() ?: 30
            val maxAtt = maxLoginAttempts.toIntOrNull() ?: 3
            val minLen = minPasswordLength.toIntOrNull() ?: 8

            val updated = config.copy(
                sessionTimeoutMinutes = timeout,
                autoLockEnabled = autoLockEnabled,
                maxLoginAttempts = maxAtt,
                minPasswordLength = minLen,
                requirePasswordComplexity = requireComplexity
            )
            viewModel.updateSystemConfig(updated, "Usuarios y Seguridad")
        }
    )
}

// -----------------------------------------------------------------------------
// 6. RUTAS Y SUCURSALES
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategoryRutasSucursalesForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var branches by remember(config) { mutableStateOf(config.branchesListCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }) }
    var routes by remember(config) { mutableStateOf(config.routesListCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }) }

    var newBranchText by remember { mutableStateOf("") }
    var newRouteText by remember { mutableStateOf("") }

    Text("Sucursales de Operación", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newBranchText,
                    onValueChange = { newBranchText = it },
                    label = { Text("Nueva Sucursal") },
                    modifier = Modifier.weight(1f).testTag("input_new_branch"),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newBranchText.isNotBlank()) {
                            branches = branches + newBranchText.trim()
                            newBranchText = ""
                        }
                    },
                    modifier = Modifier.testTag("btn_add_branch")
                ) { Text("Agregar") }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                branches.forEach { b ->
                    InputChip(
                        selected = true,
                        onClick = { branches = branches - b },
                        label = { Text(b) },
                        trailingIcon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text("Rutas de Cobranza en Campo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newRouteText,
                    onValueChange = { newRouteText = it },
                    label = { Text("Nueva Ruta") },
                    modifier = Modifier.weight(1f).testTag("input_new_route"),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newRouteText.isNotBlank()) {
                            routes = routes + newRouteText.trim()
                            newRouteText = ""
                        }
                    },
                    modifier = Modifier.testTag("btn_add_route")
                ) { Text("Agregar") }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                routes.forEach { r ->
                    InputChip(
                        selected = true,
                        onClick = { routes = routes - r },
                        label = { Text(r) },
                        trailingIcon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }

    SaveButton(
        testTag = "save_rutas_button",
        onSave = {
            val updated = config.copy(
                branchesListCsv = branches.joinToString(","),
                routesListCsv = routes.joinToString(",")
            )
            viewModel.updateSystemConfig(updated, "Rutas y Sucursales")
        }
    )
}

// -----------------------------------------------------------------------------
// 7. FONDOS
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFondosForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var funds by remember(config) { mutableStateOf(config.fundsListCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }) }
    var defaultFund by remember(config) { mutableStateOf(config.defaultFund) }
    var newFundText by remember { mutableStateOf("") }

    Text("Bolsas y Fondos Operativos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newFundText,
                    onValueChange = { newFundText = it },
                    label = { Text("Nuevo Fondo Operativo") },
                    modifier = Modifier.weight(1f).testTag("input_new_fund"),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newFundText.isNotBlank()) {
                            funds = funds + newFundText.trim()
                            newFundText = ""
                        }
                    },
                    modifier = Modifier.testTag("btn_add_fund")
                ) { Text("Agregar") }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                funds.forEach { f ->
                    InputChip(
                        selected = f == defaultFund,
                        onClick = { defaultFund = f },
                        label = { Text(f + if (f == defaultFund) " (Predeterminado)" else "") }
                    )
                }
            }
        }
    }

    OutlinedTextField(
        value = defaultFund,
        onValueChange = { defaultFund = it },
        label = { Text("Fondo Predeterminado para Asignación a Cobradores") },
        modifier = Modifier.fillMaxWidth().testTag("input_default_fund"),
        singleLine = true
    )

    SaveButton(
        testTag = "save_fondos_button",
        onSave = {
            val updated = config.copy(
                fundsListCsv = funds.joinToString(","),
                defaultFund = defaultFund
            )
            viewModel.updateSystemConfig(updated, "Fondos")
        }
    )
}

// -----------------------------------------------------------------------------
// 8. NOTIFICACIONES
// -----------------------------------------------------------------------------
@Composable
private fun CategoryNotificacionesForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var enableWhatsApp by remember(config) { mutableStateOf(config.enableWhatsAppAlerts) }
    var enablePush by remember(config) { mutableStateOf(config.enablePushNotifications) }
    var enableEmail by remember(config) { mutableStateOf(config.enableEmailAlerts) }
    var alertTriggers by remember(config) { mutableStateOf(config.alertTriggersCsv.split(",").map { it.trim() }) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Notificaciones por WhatsApp",
                subtitle = "Envío automático de recibos de cobro y recordatorios",
                checked = enableWhatsApp,
                onCheckedChange = { enableWhatsApp = it },
                testTag = "switch_whatsapp_notifications"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Notificaciones Push en App",
                subtitle = "Alertas inmediatas en terminales de cobradores y supervisores",
                checked = enablePush,
                onCheckedChange = { enablePush = it },
                testTag = "switch_push_notifications"
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            SwitchRow(
                title = "Notificaciones por Correo Electrónico",
                subtitle = "Resúmenes diarios de corte de caja y alertas críticas",
                checked = enableEmail,
                onCheckedChange = { enableEmail = it },
                testTag = "switch_email_notifications"
            )
        }
    }

    Text("Eventos Gatillo de Alerta", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

    val triggersList = listOf(
        "MORA_DETECTADA" to "Cliente entra en estado vencido",
        "APERTURA_CAJA" to "Cobrador realiza apertura de caja",
        "DESEMBOLSO_ALTO" to "Desembolso de préstamo mayor a $10,000 MXN"
    )

    triggersList.forEach { (code, label) ->
        val isChecked = code in alertTriggers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    alertTriggers = if (isChecked) alertTriggers - code else alertTriggers + code
                }
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { checked ->
                    alertTriggers = if (checked) alertTriggers + code else alertTriggers - code
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(code, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    SaveButton(
        testTag = "save_notificaciones_button",
        onSave = {
            val updated = config.copy(
                enableWhatsAppAlerts = enableWhatsApp,
                enablePushNotifications = enablePush,
                enableEmailAlerts = enableEmail,
                alertTriggersCsv = alertTriggers.joinToString(",")
            )
            viewModel.updateSystemConfig(updated, "Notificaciones")
        }
    )
}

// -----------------------------------------------------------------------------
// 9. WHATSAPP
// -----------------------------------------------------------------------------
@Composable
private fun CategoryWhatsAppForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var apiUrl by remember(config) { mutableStateOf(config.whatsappApiUrl) }
    var apiKey by remember(config) { mutableStateOf(config.whatsappApiKey) }
    var receiptTemplate by remember(config) { mutableStateOf(config.whatsappTemplatePaymentReceipt) }
    var reminderTemplate by remember(config) { mutableStateOf(config.whatsappTemplateReminder) }
    var autoSendReceipt by remember(config) { mutableStateOf(config.autoSendPaymentReceipt) }

    // New Fields
    var enableWhatsApp by remember(config) { mutableStateOf(config.enableWhatsAppAlerts) }
    var allowReminder by remember(config) { mutableStateOf(config.allowWhatsAppReminder) }
    var allowPaymentReceipt by remember(config) { mutableStateOf(config.allowWhatsAppPaymentReceipt) }
    var allowLateFee by remember(config) { mutableStateOf(config.allowWhatsAppLateFee) }
    var allowPromise by remember(config) { mutableStateOf(config.allowWhatsAppPromise) }
    var allowRenewal by remember(config) { mutableStateOf(config.allowWhatsAppRenewal) }
    var defaultCountryCode by remember(config) { mutableStateOf(config.whatsappDefaultCountryCode) }
    var businessName by remember(config) { mutableStateOf(config.whatsappBusinessName) }
    var messageSignature by remember(config) { mutableStateOf(config.whatsappMessageSignature) }
    var minIntervalMinutes by remember(config) { mutableStateOf(config.whatsappMinIntervalMinutes.toString()) }

    // State for templates
    val templates by viewModel.allMessageTemplates.collectAsState()
    var selectedTemplateIndex by remember { mutableStateOf(0) }
    val activeTemplate = templates.getOrNull(selectedTemplateIndex)

    var editingTemplateName by remember(activeTemplate) { mutableStateOf(activeTemplate?.name ?: "") }
    var editingTemplateText by remember(activeTemplate) { mutableStateOf(activeTemplate?.templateText ?: "") }

    Text(
        text = "Integración y Parámetros de WhatsApp",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Activar Integración de WhatsApp",
                subtitle = "Habilita la opción de enviar mensajes y notificaciones desde la aplicación",
                checked = enableWhatsApp,
                onCheckedChange = { enableWhatsApp = it },
                testTag = "switch_wa_global_enable"
            )
        }
    }

    OutlinedTextField(
        value = apiUrl,
        onValueChange = { apiUrl = it },
        label = { Text("URL de Endpoint API WhatsApp Gateway") },
        modifier = Modifier.fillMaxWidth().testTag("input_wa_url"),
        singleLine = true
    )

    OutlinedTextField(
        value = apiKey,
        onValueChange = { apiKey = it },
        label = { Text("API Key / Token de Acceso") },
        modifier = Modifier.fillMaxWidth().testTag("input_wa_key"),
        singleLine = true
    )

    Text(
        text = "Parámetros del Mensaje",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = defaultCountryCode,
            onValueChange = { defaultCountryCode = it },
            label = { Text("Código de País") },
            modifier = Modifier.weight(1f).testTag("input_wa_country_code"),
            singleLine = true
        )
        OutlinedTextField(
            value = minIntervalMinutes,
            onValueChange = { minIntervalMinutes = it },
            label = { Text("Intervalo Mínimo (Min)") },
            modifier = Modifier.weight(1f).testTag("input_wa_interval"),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = businessName,
        onValueChange = { businessName = it },
        label = { Text("Nombre Comercial de Empresa") },
        modifier = Modifier.fillMaxWidth().testTag("input_wa_business_name"),
        singleLine = true
    )

    OutlinedTextField(
        value = messageSignature,
        onValueChange = { messageSignature = it },
        label = { Text("Firma de Mensaje Predeterminada") },
        modifier = Modifier.fillMaxWidth().testTag("input_wa_signature"),
        singleLine = true
    )

    Text(
        text = "Permisos de Tipos de Mensaje",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Permitir Mensajes de Recordatorio",
                subtitle = "Permite enviar cuotas próximas o vencidas del día",
                checked = allowReminder,
                onCheckedChange = { allowReminder = it },
                testTag = "switch_wa_allow_reminder"
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "Permitir Mensajes de Pago Recibido",
                subtitle = "Permite enviar notificaciones y recibos de abono",
                checked = allowPaymentReceipt,
                onCheckedChange = { allowPaymentReceipt = it },
                testTag = "switch_wa_allow_receipt"
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "Permitir Mensajes de Mora",
                subtitle = "Permite enviar alertas de retraso avanzado y mora",
                checked = allowLateFee,
                onCheckedChange = { allowLateFee = it },
                testTag = "switch_wa_allow_latefee"
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "Permitir Mensajes de Promesa de Pago",
                subtitle = "Permite enviar avisos de promesas agendadas",
                checked = allowPromise,
                onCheckedChange = { allowPromise = it },
                testTag = "switch_wa_allow_promise"
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "Permitir Mensajes de Oferta de Renovación",
                subtitle = "Permite invitar a clientes selectos a renovar",
                checked = allowRenewal,
                onCheckedChange = { allowRenewal = it },
                testTag = "switch_wa_allow_renewal"
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = "Envío Automático al Registrar Pago",
                subtitle = "Envía mensaje de comprobante instantáneo al cliente tras cobrar",
                checked = autoSendReceipt,
                onCheckedChange = { autoSendReceipt = it },
                testTag = "switch_wa_auto_receipt"
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // DB Message Template Manager Section
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Edición de Plantillas de Base de Datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (templates.isNotEmpty()) {
                Text(
                    "Seleccione la plantilla que desea configurar:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEachIndexed { index, t ->
                        FilterChip(
                            selected = index == selectedTemplateIndex,
                            onClick = { selectedTemplateIndex = index },
                            label = { Text(t.name) },
                            modifier = Modifier.testTag("wa_tpl_chip_${t.code}")
                        )
                    }
                }

                if (activeTemplate != null) {
                    OutlinedTextField(
                        value = editingTemplateName,
                        onValueChange = { editingTemplateName = it },
                        label = { Text("Nombre descriptivo") },
                        modifier = Modifier.fillMaxWidth().testTag("input_wa_tpl_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editingTemplateText,
                        onValueChange = { editingTemplateText = it },
                        label = { Text("Mensaje de Plantilla") },
                        modifier = Modifier.fillMaxWidth().testTag("input_wa_tpl_text"),
                        minLines = 4
                    )

                    Text(
                        text = "Variables disponibles:\n{nombre} - Cliente | {folio} - Crédito | {monto} - Cantidad | {saldo} - Restante | {cuota} - Pago | {fecha_pago} - Día | {fecha_vencimiento} - Vencimiento | {mora} - Interés Mora | {total_pendiente} - Cuota + Mora | {nombre_empresa} - ERP | {telefono_empresa} - Teléfono",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            val updatedTemplate = activeTemplate.copy(
                                name = editingTemplateName,
                                templateText = editingTemplateText,
                                updatedAt = System.currentTimeMillis()
                            )
                            viewModel.saveMessageTemplate(updatedTemplate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.End).testTag("save_wa_tpl_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar Cambios de Plantilla")
                    }
                }
            } else {
                Text("Cargando plantilla o sin plantillas guardadas en base de datos...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    SaveButton(
        testTag = "save_whatsapp_button",
        onSave = {
            val updated = config.copy(
                whatsappApiUrl = apiUrl,
                whatsappApiKey = apiKey,
                whatsappTemplatePaymentReceipt = receiptTemplate,
                whatsappTemplateReminder = reminderTemplate,
                autoSendPaymentReceipt = autoSendReceipt,
                enableWhatsAppAlerts = enableWhatsApp,
                allowWhatsAppReminder = allowReminder,
                allowWhatsAppPaymentReceipt = allowPaymentReceipt,
                allowWhatsAppLateFee = allowLateFee,
                allowWhatsAppPromise = allowPromise,
                allowWhatsAppRenewal = allowRenewal,
                whatsappDefaultCountryCode = defaultCountryCode,
                whatsappBusinessName = businessName,
                whatsappMessageSignature = messageSignature,
                whatsappMinIntervalMinutes = minIntervalMinutes.toIntOrNull() ?: 5
            )
            viewModel.updateSystemConfig(updated, "WhatsApp")
        }
    )
}

// -----------------------------------------------------------------------------
// 10. GOOGLE MAPS
// -----------------------------------------------------------------------------
@Composable
private fun CategoryGoogleMapsForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var mapsApiKey by remember(config) { mutableStateOf(config.googleMapsApiKey) }
    var enableGeofencing by remember(config) { mutableStateOf(config.enableGeofencing) }
    var trackingInterval by remember(config) { mutableStateOf(config.gpsTrackingIntervalMinutes.toString()) }
    var mapsEnabled by remember(config) { mutableStateOf(config.mapsEnabled) }
    var locationRecordingEnabled by remember(config) { mutableStateOf(config.locationRecordingEnabled) }
    var clientLocationEnabled by remember(config) { mutableStateOf(config.clientLocationEnabled) }
    var visitLocationEnabled by remember(config) { mutableStateOf(config.visitLocationEnabled) }
    var minGpsAccuracyMeters by remember(config) { mutableStateOf(config.minGpsAccuracyMeters.toString()) }
    var useExternalNavigation by remember(config) { mutableStateOf(config.useExternalNavigation) }
    var gpsLocationHistoryEnabled by remember(config) { mutableStateOf(config.gpsLocationHistoryEnabled) }
    var gpsPrivacyModeEnabled by remember(config) { mutableStateOf(config.gpsPrivacyModeEnabled) }

    OutlinedTextField(
        value = mapsApiKey,
        onValueChange = { mapsApiKey = it },
        label = { Text("API Key de Google Maps SDK") },
        modifier = Modifier.fillMaxWidth().testTag("input_maps_api_key"),
        singleLine = true
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = trackingInterval,
            onValueChange = { trackingInterval = it },
            label = { Text("Rastreo GPS (Minutos)") },
            modifier = Modifier.weight(1f).testTag("input_gps_interval"),
            singleLine = true
        )
        OutlinedTextField(
            value = minGpsAccuracyMeters,
            onValueChange = { minGpsAccuracyMeters = it },
            label = { Text("Precisión Mínima (Metros)") },
            modifier = Modifier.weight(1f).testTag("input_gps_precision"),
            singleLine = true
        )
    }

    Text("Controles de Activación de Mapas & GPS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Mapas Habilitados",
                subtitle = "Habilita la visualización general de mapas y coordenadas en el sistema",
                checked = mapsEnabled,
                onCheckedChange = { mapsEnabled = it },
                testTag = "switch_maps_enabled"
            )
            HorizontalDivider()
            SwitchRow(
                title = "Geofencing Activo",
                subtitle = "Valida que las visitas de cobranza se realicen a menos de 100m del domicilio",
                checked = enableGeofencing,
                onCheckedChange = { enableGeofencing = it },
                testTag = "switch_geofencing"
            )
            HorizontalDivider()
            SwitchRow(
                title = "Registro de Ubicación Automático",
                subtitle = "Habilita la captura en segundo plano para auditorías de ruta",
                checked = locationRecordingEnabled,
                onCheckedChange = { locationRecordingEnabled = it },
                testTag = "switch_location_recording"
            )
            HorizontalDivider()
            SwitchRow(
                title = "Ubicación Geográfica de Clientes",
                subtitle = "Permite registrar y editar las coordenadas del domicilio del cliente",
                checked = clientLocationEnabled,
                onCheckedChange = { clientLocationEnabled = it },
                testTag = "switch_client_location_enabled"
            )
            HorizontalDivider()
            SwitchRow(
                title = "Ubicación de Visitas de Cobranza",
                subtitle = "Habilita el registro de coordenadas geográficas al subir una visita",
                checked = visitLocationEnabled,
                onCheckedChange = { visitLocationEnabled = it },
                testTag = "switch_visit_location_enabled"
            )
            HorizontalDivider()
            SwitchRow(
                title = "Uso de Navegación Externa",
                subtitle = "Permite disparar Intents hacia Google Maps, Waze o navegadores externos",
                checked = useExternalNavigation,
                onCheckedChange = { useExternalNavigation = it },
                testTag = "switch_use_external_navigation"
            )
            HorizontalDivider()
            SwitchRow(
                title = "Historial de Ubicación",
                subtitle = "Mantiene logs de rastreo de visitas y registros geográficos",
                checked = gpsLocationHistoryEnabled,
                onCheckedChange = { gpsLocationHistoryEnabled = it },
                testTag = "switch_gps_location_history_enabled"
            )
            HorizontalDivider()
            SwitchRow(
                title = "Modo de Privacidad Estricto",
                subtitle = "Oculta logs detallados a cobradores y requiere confirmación explícita",
                checked = gpsPrivacyModeEnabled,
                onCheckedChange = { gpsPrivacyModeEnabled = it },
                testTag = "switch_gps_privacy_mode_enabled"
            )
        }
    }

    SaveButton(
        testTag = "save_maps_button",
        onSave = {
            val interval = trackingInterval.toIntOrNull() ?: 5
            val accuracy = minGpsAccuracyMeters.toFloatOrNull() ?: 10.0f
            val updated = config.copy(
                googleMapsApiKey = mapsApiKey,
                enableGeofencing = enableGeofencing,
                gpsTrackingIntervalMinutes = interval,
                mapsEnabled = mapsEnabled,
                locationRecordingEnabled = locationRecordingEnabled,
                clientLocationEnabled = clientLocationEnabled,
                visitLocationEnabled = visitLocationEnabled,
                minGpsAccuracyMeters = accuracy,
                useExternalNavigation = useExternalNavigation,
                gpsLocationHistoryEnabled = gpsLocationHistoryEnabled,
                gpsPrivacyModeEnabled = gpsPrivacyModeEnabled
            )
            viewModel.updateSystemConfig(updated, "Google Maps")
        }
    )
}

// -----------------------------------------------------------------------------
// 11. RESPALDOS
// -----------------------------------------------------------------------------
@Composable
private fun CategoryRespaldosForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var frequency by remember(config) { mutableStateOf(config.backupFrequency) }
    var retentionDays by remember(config) { mutableStateOf(config.backupRetentionDays.toString()) }
    var location by remember(config) { mutableStateOf(config.backupLocation) }

    OutlinedTextField(
        value = frequency,
        onValueChange = { frequency = it },
        label = { Text("Frecuencia de Respaldo Automático (DIARIO / SEMANAL / MENSUAL)") },
        modifier = Modifier.fillMaxWidth().testTag("input_backup_freq"),
        singleLine = true
    )

    OutlinedTextField(
        value = retentionDays,
        onValueChange = { retentionDays = it },
        label = { Text("Días de Retención de Copias de Seguridad") },
        modifier = Modifier.fillMaxWidth().testTag("input_backup_retention"),
        singleLine = true
    )

    OutlinedTextField(
        value = location,
        onValueChange = { location = it },
        label = { Text("Ubicación de Almacenamiento del Respaldo") },
        modifier = Modifier.fillMaxWidth().testTag("input_backup_location"),
        singleLine = true
    )

    // Last backup status
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val lastBackupStr = remember(config.lastBackupDateMs) { sdf.format(Date(config.lastBackupDateMs)) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Estado de la Base de Datos", fontWeight = FontWeight.Bold)
            }
            Text("Último respaldo completado: $lastBackupStr", style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    onClick = {
                        val updated = config.copy(lastBackupDateMs = System.currentTimeMillis())
                        viewModel.updateSystemConfig(updated, "Respaldos")
                    },
                    modifier = Modifier.weight(1f).testTag("btn_create_backup_now")
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crear Respaldo Ahora", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.updateSystemConfig(config, "Restauración Simulación")
                    },
                    modifier = Modifier.weight(1f).testTag("btn_restore_backup_now")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restaurar Copia", fontSize = 12.sp)
                }
            }
        }
    }

    SaveButton(
        testTag = "save_respaldos_button",
        onSave = {
            val ret = retentionDays.toIntOrNull() ?: 30
            val updated = config.copy(
                backupFrequency = frequency,
                backupRetentionDays = ret,
                backupLocation = location
            )
            viewModel.updateSystemConfig(updated, "Respaldos")
        }
    )
}

// -----------------------------------------------------------------------------
// 12. AUDITORÍA
// -----------------------------------------------------------------------------
@Composable
private fun CategoryAuditoriaForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var logLevel by remember(config) { mutableStateOf(config.auditLogLevel) }
    var retentionDays by remember(config) { mutableStateOf(config.auditLogRetentionDays.toString()) }
    var logConfigChanges by remember(config) { mutableStateOf(config.logConfigChanges) }

    OutlinedTextField(
        value = logLevel,
        onValueChange = { logLevel = it },
        label = { Text("Nivel de Detalle de Audit Log (COMPLETO / ESTANDAR / MINIMO)") },
        modifier = Modifier.fillMaxWidth().testTag("input_audit_level"),
        singleLine = true
    )

    OutlinedTextField(
        value = retentionDays,
        onValueChange = { retentionDays = it },
        label = { Text("Días de Retención de Histórico de Auditoría") },
        modifier = Modifier.fillMaxWidth().testTag("input_audit_retention"),
        singleLine = true
    )

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow(
                title = "Registrar Cambios de Configuración",
                subtitle = "Guarda valores anteriores y nuevos en la bitácora inmutable",
                checked = logConfigChanges,
                onCheckedChange = { logConfigChanges = it },
                testTag = "switch_log_config_changes"
            )
        }
    }

    SaveButton(
        testTag = "save_auditoria_button",
        onSave = {
            val ret = retentionDays.toIntOrNull() ?: 90
            val updated = config.copy(
                auditLogLevel = logLevel,
                auditLogRetentionDays = ret,
                logConfigChanges = logConfigChanges
            )
            viewModel.updateSystemConfig(updated, "Auditoría")
        }
    )
}

// -----------------------------------------------------------------------------
// 13. NUMERACIONES AUTOMÁTICAS
// -----------------------------------------------------------------------------
@Composable
private fun CategoryNumeracionesForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var loanPrefix by remember(config) { mutableStateOf(config.loanPrefix) }
    var loanNextFolio by remember(config) { mutableStateOf(config.loanNextFolio.toString()) }
    var receiptPrefix by remember(config) { mutableStateOf(config.receiptPrefix) }
    var receiptNextFolio by remember(config) { mutableStateOf(config.receiptNextFolio.toString()) }
    var clientPrefix by remember(config) { mutableStateOf(config.clientPrefix) }
    var clientNextFolio by remember(config) { mutableStateOf(config.clientNextFolio.toString()) }
    var cashRegisterPrefix by remember(config) { mutableStateOf(config.cashRegisterPrefix) }
    var cashRegisterNextFolio by remember(config) { mutableStateOf(config.cashRegisterNextFolio.toString()) }

    Text("Folios y Prefijos de Préstamos", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = loanPrefix,
            onValueChange = { loanPrefix = it },
            label = { Text("Prefijo Préstamos") },
            modifier = Modifier.weight(1f).testTag("input_loan_prefix"),
            singleLine = true
        )
        OutlinedTextField(
            value = loanNextFolio,
            onValueChange = { loanNextFolio = it },
            label = { Text("Siguiente Folio") },
            modifier = Modifier.weight(1f).testTag("input_loan_next_folio"),
            singleLine = true
        )
    }

    Text("Folios y Prefijos de Recibos de Pago", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = receiptPrefix,
            onValueChange = { receiptPrefix = it },
            label = { Text("Prefijo Recibos") },
            modifier = Modifier.weight(1f).testTag("input_receipt_prefix"),
            singleLine = true
        )
        OutlinedTextField(
            value = receiptNextFolio,
            onValueChange = { receiptNextFolio = it },
            label = { Text("Siguiente Folio") },
            modifier = Modifier.weight(1f).testTag("input_receipt_next_folio"),
            singleLine = true
        )
    }

    Text("Folios y Prefijos de Clientes", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = clientPrefix,
            onValueChange = { clientPrefix = it },
            label = { Text("Prefijo Clientes") },
            modifier = Modifier.weight(1f).testTag("input_client_prefix"),
            singleLine = true
        )
        OutlinedTextField(
            value = clientNextFolio,
            onValueChange = { clientNextFolio = it },
            label = { Text("Siguiente Folio") },
            modifier = Modifier.weight(1f).testTag("input_client_next_folio"),
            singleLine = true
        )
    }

    Text("Folios y Prefijos de Corte de Caja", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = cashRegisterPrefix,
            onValueChange = { cashRegisterPrefix = it },
            label = { Text("Prefijo Cajas") },
            modifier = Modifier.weight(1f).testTag("input_cash_prefix"),
            singleLine = true
        )
        OutlinedTextField(
            value = cashRegisterNextFolio,
            onValueChange = { cashRegisterNextFolio = it },
            label = { Text("Siguiente Folio") },
            modifier = Modifier.weight(1f).testTag("input_cash_next_folio"),
            singleLine = true
        )
    }

    SaveButton(
        testTag = "save_numeraciones_button",
        onSave = {
            val updated = config.copy(
                loanPrefix = loanPrefix,
                loanNextFolio = loanNextFolio.toLongOrNull() ?: 1001L,
                receiptPrefix = receiptPrefix,
                receiptNextFolio = receiptNextFolio.toLongOrNull() ?: 5001L,
                clientPrefix = clientPrefix,
                clientNextFolio = clientNextFolio.toLongOrNull() ?: 2001L,
                cashRegisterPrefix = cashRegisterPrefix,
                cashRegisterNextFolio = cashRegisterNextFolio.toLongOrNull() ?: 3001L
            )
            viewModel.updateSystemConfig(updated, "Numeraciones Automáticas")
        }
    )
}

// -----------------------------------------------------------------------------
// 14. APARIENCIA
// -----------------------------------------------------------------------------
@Composable
private fun CategoryAparienciaForm(config: SystemConfigEntity, viewModel: MainViewModel) {
    var themeMode by remember(config) { mutableStateOf(config.themeMode) }
    var primaryBrandColor by remember(config) { mutableStateOf(config.primaryBrandColor) }
    var density by remember(config) { mutableStateOf(config.density) }

    OutlinedTextField(
        value = themeMode,
        onValueChange = { themeMode = it },
        label = { Text("Modo de Tema Visual (SISTEMA / CLARO / OSCURO)") },
        modifier = Modifier.fillMaxWidth().testTag("input_theme_mode"),
        singleLine = true
    )

    OutlinedTextField(
        value = primaryBrandColor,
        onValueChange = { primaryBrandColor = it },
        label = { Text("Color Primario de Marca (HEX)") },
        modifier = Modifier.fillMaxWidth().testTag("input_brand_color"),
        singleLine = true
    )

    OutlinedTextField(
        value = density,
        onValueChange = { density = it },
        label = { Text("Densidad de Interfaz (CÓMODA / COMPACTA)") },
        modifier = Modifier.fillMaxWidth().testTag("input_density"),
        singleLine = true
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { viewModel.toggleDarkMode() },
            modifier = Modifier.weight(1f).testTag("btn_toggle_dark_mode")
        ) {
            Icon(Icons.Default.Palette, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Probar Tema Oscuro")
        }
    }

    SaveButton(
        testTag = "save_apariencia_button",
        onSave = {
            val updated = config.copy(
                themeMode = themeMode,
                primaryBrandColor = primaryBrandColor,
                density = density
            )
            viewModel.updateSystemConfig(updated, "Apariencia")
        }
    )
}

// -----------------------------------------------------------------------------
// HELPER COMPONENTS
// -----------------------------------------------------------------------------
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun CriticalSaveButton(
    testTag: String,
    onSave: () -> Unit,
    previousValue: String,
    newValue: String,
    warning: String
) {
    var showDialog by remember { mutableStateOf(false) }

    Button(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(top = 8.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.Default.Warning, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Guardar Cambios Críticos", fontWeight = FontWeight.Bold)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirmar Cambio Crítico") },
            text = {
                Column {
                    Text(warning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Valor anterior:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(previousValue, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Valor nuevo:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(newValue, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave()
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar y Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SaveButton(testTag: String, onSave: () -> Unit) {
    Button(
        onClick = onSave,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(top = 8.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Save, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Guardar Cambios de Configuración", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryEvaluacionScoreForm(viewModel: MainViewModel) {
    val evalConfigState by viewModel.evaluationConfig.collectAsStateWithLifecycle()
    val evalConfig = evalConfigState ?: com.example.data.local.ClientEvaluationConfigEntity()

    var enableScore by remember(evalConfig) { mutableStateOf(evalConfig.enableScore) }
    var allowManualAdjustments by remember(evalConfig) { mutableStateOf(evalConfig.allowManualAdjustments) }
    var historicDaysText by remember(evalConfig) { mutableStateOf(evalConfig.historicPeriodDays.toString()) }
    var maxLoansText by remember(evalConfig) { mutableStateOf(evalConfig.maxActiveLoansPerClient.toString()) }

    var wPayment by remember(evalConfig) { mutableStateOf(evalConfig.weightPaymentHistory.toString()) }
    var wMora by remember(evalConfig) { mutableStateOf(evalConfig.weightMora.toString()) }
    var wLoans by remember(evalConfig) { mutableStateOf(evalConfig.weightLoansHistory.toString()) }
    var wCompliance by remember(evalConfig) { mutableStateOf(evalConfig.weightCompliancePct.toString()) }
    var wTenure by remember(evalConfig) { mutableStateOf(evalConfig.weightTenure.toString()) }
    var wCollection by remember(evalConfig) { mutableStateOf(evalConfig.weightCollectionBehavior.toString()) }

    var rExcelente by remember(evalConfig) { mutableStateOf(evalConfig.rangeExcelenteMin.toString()) }
    var rMuyBueno by remember(evalConfig) { mutableStateOf(evalConfig.rangeMuyBuenoMin.toString()) }
    var rBueno by remember(evalConfig) { mutableStateOf(evalConfig.rangeBuenoMin.toString()) }
    var rRegular by remember(evalConfig) { mutableStateOf(evalConfig.rangeRegularMin.toString()) }
    var rRiesgoAlto by remember(evalConfig) { mutableStateOf(evalConfig.rangeRiesgoAltoMin.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Parámetros Generales de Evaluación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        SwitchRow(
            title = "Activar Motor de Evaluation y Score",
            subtitle = "Calcula recomendaciones crediticias automáticas de apoyo a decisión",
            checked = enableScore,
            onCheckedChange = { enableScore = it },
            testTag = "switch_enable_score"
        )

        SwitchRow(
            title = "Permitir Ajustes Manuales de Score",
            subtitle = "Habilita a Supervisores/Admin para ingresar observaciones y sobrescribir score",
            checked = allowManualAdjustments,
            onCheckedChange = { allowManualAdjustments = it },
            testTag = "switch_allow_manual_adjust"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = historicDaysText,
                onValueChange = { historicDaysText = it },
                label = { Text("Periodo Análisis (días)") },
                modifier = Modifier.weight(1f).testTag("input_historic_days"),
                singleLine = true
            )

            OutlinedTextField(
                value = maxLoansText,
                onValueChange = { maxLoansText = it },
                label = { Text("Máx. Préstamos Activos") },
                modifier = Modifier.weight(1f).testTag("input_max_active_loans"),
                singleLine = true
            )
        }

        HorizontalDivider()

        Text("Ponderaciones por Factor (% Total Debe Sumar 100%)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = wPayment, onValueChange = { wPayment = it }, label = { Text("Historial Pagos (%)") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = wMora, onValueChange = { wMora = it }, label = { Text("Mora (%)") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = wLoans, onValueChange = { wLoans = it }, label = { Text("Historial Créditos (%)") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = wCompliance, onValueChange = { wCompliance = it }, label = { Text("Cumplimiento (%)") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = wTenure, onValueChange = { wTenure = it }, label = { Text("Antigüedad (%)") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = wCollection, onValueChange = { wCollection = it }, label = { Text("Cobranza (%)") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        HorizontalDivider()

        Text("Rangos Mínimos de Clasificación (0 - 100)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = rExcelente, onValueChange = { rExcelente = it }, label = { Text("Excelente Min") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = rMuyBueno, onValueChange = { rMuyBueno = it }, label = { Text("Muy Bueno Min") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = rBueno, onValueChange = { rBueno = it }, label = { Text("Bueno Min") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = rRegular, onValueChange = { rRegular = it }, label = { Text("Regular Min") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = rRiesgoAlto, onValueChange = { rRiesgoAlto = it }, label = { Text("Riesgo Alto Min") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        SaveButton(testTag = "btn_save_evaluation_config") {
            val newConfig = evalConfig.copy(
                enableScore = enableScore,
                allowManualAdjustments = allowManualAdjustments,
                historicPeriodDays = historicDaysText.toIntOrNull() ?: 180,
                maxActiveLoansPerClient = maxLoansText.toIntOrNull() ?: 2,
                weightPaymentHistory = wPayment.toDoubleOrNull() ?: 30.0,
                weightMora = wMora.toDoubleOrNull() ?: 25.0,
                weightLoansHistory = wLoans.toDoubleOrNull() ?: 15.0,
                weightCompliancePct = wCompliance.toDoubleOrNull() ?: 15.0,
                weightTenure = wTenure.toDoubleOrNull() ?: 5.0,
                weightCollectionBehavior = wCollection.toDoubleOrNull() ?: 10.0,
                rangeExcelenteMin = rExcelente.toIntOrNull() ?: 90,
                rangeMuyBuenoMin = rMuyBueno.toIntOrNull() ?: 80,
                rangeBuenoMin = rBueno.toIntOrNull() ?: 70,
                rangeRegularMin = rRegular.toIntOrNull() ?: 60,
                rangeRiesgoAltoMin = rRiesgoAlto.toIntOrNull() ?: 40
            )
            viewModel.saveEvaluationConfig(newConfig)
        }
    }
}
