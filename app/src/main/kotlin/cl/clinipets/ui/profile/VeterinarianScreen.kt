package cl.clinipets.ui.profile

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ActualizarPerfilRequest
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import cl.clinipets.openapi.models.Veterinario
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VeterinarianScreen(
    suggestedName: String?,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Campos de formulario (compartidos para crear/actualizar)
    var nombre by rememberSaveable { mutableStateOf(suggestedName.orEmpty()) }
    var licencia by rememberSaveable { mutableStateOf("") }
    var latitud by rememberSaveable { mutableStateOf("") }
    var longitud by rememberSaveable { mutableStateOf("") }
    var radio by rememberSaveable { mutableStateOf("") }

    // UI State usa el Enum del modelo Veterinario
    var selectedModes by remember { mutableStateOf(setOf<Veterinario.ModosAtencion>()) }

    // Estado local para saber si ya precargamos los campos desde el perfil
    var didPrefill by rememberSaveable { mutableStateOf(false) }
    // Estado para mostrar diálogo de éxito post acción
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastAction by rememberSaveable { mutableStateOf("idle") } // "idle" | "create" | "update"

    // Carga segura del perfil si aún no está disponible (prellenado al llegar)
    LaunchedEffect(Unit) {
        val s = vm.ui.value
        if (!s.isLoading && s.perfil == null) {
            vm.loadMyProfile()
        }
    }

    // Mostrar errores en el Snackbar y limpiarlos luego
    val error = uiState.error
    LaunchedEffect(error) {
        if (error != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    actionLabel = "Entendido"
                )
                vm.clearError()
            }
        }
    }

    // Prefill cuando llegue el perfil por primera vez
    LaunchedEffect(uiState.perfil) {
        val p = uiState.perfil
        if (p != null && !didPrefill) {
            nombre = p.nombreCompleto
            licencia = p.numeroLicencia.orEmpty()
            latitud = p.latitud?.toString().orEmpty()
            longitud = p.longitud?.toString().orEmpty()
            radio = p.radioCobertura?.toString().orEmpty()
            selectedModes = p.modosAtencion.toSet()
            didPrefill = true
        }
        if (p != null && lastAction != "idle") {
            showSuccessDialog = true
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (lastAction == "update") "Perfil actualizado" else "Solicitud enviada") },
            text = { Text(if (lastAction == "update") "Tus datos profesionales fueron actualizados." else "Tu perfil está pendiente de verificación.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    lastAction = "idle"
                    onCompleted()
                }) { Text("Aceptar") }
            }
        )
    }
    val isUpdating = uiState.perfil != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos profesionales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(24.dp), // Padding interior
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = if (isUpdating) "Actualiza tus datos profesionales." else "Completa tus datos para comenzar el proceso de verificación.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { if (!isUpdating) nombre = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = isUpdating,
                    enabled = !isUpdating
                )
            }

            item {
                OutlinedTextField(
                    value = licencia,
                    onValueChange = { if (!isUpdating) licencia = it },
                    label = { Text("Número de licencia (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = isUpdating,
                    enabled = !isUpdating
                )
            }

            item {
                Text(
                    text = "Modos de atención",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                ModeSelector(
                    selectedModes = selectedModes,
                    onToggle = { mode -> // 'mode' es Veterinario.ModosAtencion
                        selectedModes = if (selectedModes.contains(mode)) {
                            selectedModes - mode
                        } else {
                            selectedModes + mode
                        }
                    }
                )
            }

            item {
                Text(
                    text = "Ubicación y radio de cobertura",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Reemplazo de latitud/longitud + radio por mapa interactivo
            item {
                MapaCobertura(
                    latStr = latitud,
                    lonStr = longitud,
                    radioStr = radio,
                    scope = scope, // <-- AÑADIDO
                    onChange = { newLat, newLon, newRadio ->
                        latitud = newLat
                        longitud = newLon
                        radio = newRadio
                    }
                )
            }

            item {
                Button(
                    onClick = {
                        val lat = latitud.toDoubleOrNull()
                        val lon = longitud.toDoubleOrNull()
                        val rad = radio.toDoubleOrNull()
                        val lic = licencia.ifEmpty { null }

                        lastAction = if (isUpdating) "update" else "create"

                        if (isUpdating) {
                            val modosParaActualizar = selectedModes.mapNotNull { uiMode ->
                                try { ActualizarPerfilRequest.ModosAtencion.valueOf(uiMode.name) } catch (_: IllegalArgumentException) { null }
                            }.toSet()

                            vm.updateMyProfile(
                                ActualizarPerfilRequest(
                                    // 'nombre' y 'licencia' no se envían si se está actualizando
                                    // (ya que los campos están deshabilitados)
                                    // Si SÍ quieres enviarlos, quita 'readOnly' y 'enabled' de los OutlinedTextField
                                    nombreCompleto = null, // O `nombre` si permites editarlo
                                    numeroLicencia = null, // O `lic` si permites editarlo
                                    modosAtencion = modosParaActualizar,
                                    latitud = lat,
                                    longitud = lon,
                                    radioCobertura = rad
                                )
                            )
                        } else {
                            val modosParaRegistrar = selectedModes.mapNotNull { uiMode ->
                                try { RegistrarVeterinarioRequest.ModosAtencion.valueOf(uiMode.name) } catch (_: IllegalArgumentException) { null }
                            }.toSet()

                            vm.submit(
                                RegistrarVeterinarioRequest(
                                    nombreCompleto = nombre,
                                    numeroLicencia = lic,
                                    modosAtencion = modosParaRegistrar,
                                    latitud = lat,
                                    longitud = lon,
                                    radioCobertura = rad
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Text(if (isUpdating) "Actualizar Cobertura" else "Registrar") // Texto actualizado
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ModeSelector(
    selectedModes: Set<Veterinario.ModosAtencion>,
    onToggle: (Veterinario.ModosAtencion) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Veterinario.ModosAtencion.entries.forEach { mode ->
            val selected = selectedModes.contains(mode)
            FilterChip(
                selected = selected,
                onClick = { onToggle(mode) },
                label = { Text(mode.name) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

@Composable
private fun MapaCobertura(
    latStr: String,
    lonStr: String,
    radioStr: String,
    scope: CoroutineScope, // <-- AÑADIDO
    onChange: (latStr: String, lonStr: String, radioStr: String) -> Unit
) {
    val context = LocalContext.current

    // Permisos de ubicación
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Valor por defecto: Santiago de Chile
    val defaultLatLng = LatLng(-33.45, -70.66)
    val lat = latStr.toDoubleOrNull() ?: defaultLatLng.latitude
    val lon = lonStr.toDoubleOrNull() ?: defaultLatLng.longitude
    val radioKmInit = (radioStr.toDoubleOrNull() ?: 5.0).coerceIn(0.2, 5.0)

    val markerState = remember(lat, lon) { MarkerState(LatLng(lat, lon)) }
    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerState.position, if (latStr.toDoubleOrNull() != null) 13f else 10f)
    }

    // --- LÓGICA DE PERMISOS MEJORADA ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (hasLocationPermission) {
                // Al aceptar permisos, centra y anima
                centerOnMyLocation(context) { latLng ->
                    val r = radioStr.toDoubleOrNull() ?: radioKmInit
                    onChange(
                        String.format(Locale.US, "%.6f", latLng.latitude),
                        String.format(Locale.US, "%.6f", latLng.longitude),
                        String.format(Locale.US, "%.1f", r)
                    )
                    markerState.position = latLng
                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) }
                }
            }
        }
    )
    // --- FIN LÓGICA PERMISOS ---

    var radioKm by remember(radioKmInit) { mutableStateOf(radioKmInit) }

    // Centrar automáticamente una sola vez si hay permisos
    var didCenterOnce by remember { mutableStateOf(false) }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && !didCenterOnce) {
            centerOnMyLocation(context) { latLng ->
                markerState.position = latLng
                onChange(
                    String.format(Locale.US, "%.6f", latLng.latitude),
                    String.format(Locale.US, "%.6f", latLng.longitude),
                    String.format(Locale.US, "%.1f", radioKm)
                )
                scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) }
                didCenterOnce = true
            }
        }
    }

    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = hasLocationPermission) }
    val mapProperties = remember(hasLocationPermission) { MapProperties(isMyLocationEnabled = hasLocationPermission) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Barra de acciones
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                } else {
                    centerOnMyLocation(context) { latLng ->
                        markerState.position = latLng
                        // Ajusta cámara para encuadrar el círculo con el radio actual
                        fitCircleInView(cameraPositionState, latLng, radioKm, scope)
                        onChange(
                            String.format(Locale.US, "%.6f", latLng.latitude),
                            String.format(Locale.US, "%.6f", latLng.longitude),
                            String.format(Locale.US, "%.1f", radioKm)
                        )
                    }
                }
            }) { Text("Usar mi ubicación") }
        }

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            properties = mapProperties,
            onMapClick = { latLng ->
                markerState.position = latLng
                onChange(
                    String.format(Locale.US, "%.6f", latLng.latitude),
                    String.format(Locale.US, "%.6f", latLng.longitude),
                    String.format(Locale.US, "%.1f", radioKm)
                )
            }
        ) {

            Marker(
                state = markerState,
                draggable = false
            )
            Circle(
                center = markerState.position,
                radius = (radioKm * 1000.0), // Radio en metros
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                strokeColor = MaterialTheme.colorScheme.primary,
                strokeWidth = 2f
            )
        }

        // --- SLIDER MEJORADO ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Radio: ${String.format(Locale.US, "%.1f", radioKm)} km", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = radioKm.toFloat(),
                onValueChange = { v: Float ->
                    radioKm = v.toDouble().coerceIn(0.2, 5.0)
                },
                onValueChangeFinished = {
                    // Actualiza estado principal y encuadra círculo
                    onChange(
                        String.format(Locale.US, "%.6f", markerState.position.latitude),
                        String.format(Locale.US, "%.6f", markerState.position.longitude),
                        String.format(Locale.US, "%.1f", radioKm)
                    )
                    fitCircleInView(cameraPositionState, markerState.position, radioKm, scope)
                },
                valueRange = 0.2f..5f,
                steps = 98
            )
        }
        // --- FIN SLIDER ---

        // Coordenadas de referencia (solo lectura)
        Text(
            "Lat: ${String.format(Locale.US, "%.6f", markerState.position.latitude)}  |  Lon: ${String.format(Locale.US, "%.6f", markerState.position.longitude)}",
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider() // <-- CORREGIDO
    }
}

// Helpers para encuadrar el círculo en el mapa
private fun circleBounds(center: LatLng, radiusKm: Double): LatLngBounds {
    val latRad = Math.toRadians(center.latitude)
    val dLat = radiusKm / 111.0 // ~111 km por grado
    val dLng = radiusKm / (111.0 * kotlin.math.max(0.0001, kotlin.math.cos(latRad)))
    val sw = LatLng(center.latitude - dLat, center.longitude - dLng)
    val ne = LatLng(center.latitude + dLat, center.longitude + dLng)
    return LatLngBounds(sw, ne)
}

private fun fitCircleInView(
    cameraPositionState: CameraPositionState,
    center: LatLng,
    radiusKm: Double,
    scope: CoroutineScope,
    paddingPx: Int = 48
) {
    val bounds = circleBounds(center, radiusKm)
    scope.launch {
        try {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
        } catch (_: Exception) {
            // Ignorar si el mapa aún no tiene tamaño calculado; se ajustará con futuras interacciones
        }
    }
}

// --- LÓGICA DE UBICACIÓN MEJORADA ---
@SuppressLint("MissingPermission") // La UI ya comprueba el permiso antes de llamar
private fun centerOnMyLocation(
    context: android.content.Context,
    onLocated: (LatLng) -> Unit
) {
    val fused = LocationServices.getFusedLocationProviderClient(context)
    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { loc ->
            if (loc != null) {
                onLocated(LatLng(loc.latitude, loc.longitude))
            } else {
                // Fallback a última ubicación conocida
                fused.lastLocation.addOnSuccessListener { last ->
                    if (last != null) onLocated(LatLng(last.latitude, last.longitude))
                }
            }
        }
        .addOnFailureListener {
            // Fallback en caso de fallo
            fused.lastLocation.addOnSuccessListener { last ->
                if (last != null) onLocated(LatLng(last.latitude, last.longitude))
            }
        }
}