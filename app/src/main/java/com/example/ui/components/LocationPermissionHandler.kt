package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

enum class LocationPermissionStatus {
    GRANTED,
    LIMITED,
    DENIED,
    GPS_DISABLED,
    NOT_REQUESTED
}

object LocationPermissionHelper {
    fun checkPermission(context: Context): LocationPermissionStatus {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            return LocationPermissionStatus.DENIED
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (!isGpsEnabled) {
            return LocationPermissionStatus.GPS_DISABLED
        }

        return if (fineGranted) LocationPermissionStatus.GRANTED else LocationPermissionStatus.LIMITED
    }
}

@Composable
fun LocationPermissionCard(
    modifier: Modifier = Modifier,
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(LocationPermissionHelper.checkPermission(context)) }
    var showRationaleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            permissionStatus = LocationPermissionHelper.checkPermission(context)
            if (permissionStatus == LocationPermissionStatus.GRANTED || permissionStatus == LocationPermissionStatus.LIMITED) {
                onPermissionGranted()
            }
        } else {
            permissionStatus = LocationPermissionStatus.DENIED
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (permissionStatus) {
                LocationPermissionStatus.GRANTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                LocationPermissionStatus.LIMITED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                LocationPermissionStatus.GPS_DISABLED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (permissionStatus) {
                    LocationPermissionStatus.GRANTED -> Icons.Default.GpsFixed
                    LocationPermissionStatus.LIMITED -> Icons.Default.Warning
                    LocationPermissionStatus.GPS_DISABLED -> Icons.Default.GpsOff
                    else -> Icons.Default.LocationOff
                },
                contentDescription = "Estado de GPS",
                tint = when (permissionStatus) {
                    LocationPermissionStatus.GRANTED -> MaterialTheme.colorScheme.primary
                    LocationPermissionStatus.LIMITED -> MaterialTheme.colorScheme.tertiary
                    LocationPermissionStatus.GPS_DISABLED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (permissionStatus) {
                        LocationPermissionStatus.GRANTED -> "GPS Activo y Concedido"
                        LocationPermissionStatus.LIMITED -> "Ubicación Aproximada Concedida"
                        LocationPermissionStatus.GPS_DISABLED -> "GPS Desactivado en Dispositivo"
                        else -> "Permiso de Ubicación Requerido"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = when (permissionStatus) {
                        LocationPermissionStatus.GRANTED -> "Las coordenadas de la visita se certificarán con alta precisión."
                        LocationPermissionStatus.LIMITED -> "Se recomienda conceder precisión fina para certificación de ruta."
                        LocationPermissionStatus.GPS_DISABLED -> "Por favor encienda el GPS de su teléfono para registrar la visita."
                        else -> "Permite acceder al GPS para registrar clientes y rutas de cobranza."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (permissionStatus != LocationPermissionStatus.GRANTED) {
                Button(
                    onClick = {
                        if (permissionStatus == LocationPermissionStatus.GPS_DISABLED) {
                            try {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                showRationaleDialog = true
                            }
                        } else {
                            showRationaleDialog = true
                        }
                    },
                    modifier = Modifier.testTag("btn_request_location_perm")
                ) {
                    Text(if (permissionStatus == LocationPermissionStatus.GPS_DISABLED) "Activar" else "Conceder")
                }
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            icon = { Icon(Icons.Default.GpsFixed, contentDescription = null) },
            title = { Text("Permiso de Ubicación GPS", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "RAMA ERP Microfinanzas utiliza la ubicación GPS de su dispositivo exclusivamente para:\n\n" +
                            "• Certificar la presencia física del cobrador durante el registro de visitas.\n" +
                            "• Calcular distancias y tiempos de traslado entre clientes.\n" +
                            "• Optimizar el orden secuencial de la ruta diaria.\n\n" +
                            "Su privacidad está protegida. Ningún dato se comparte fuera de la operación financiera."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Continuar y Solicitar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
