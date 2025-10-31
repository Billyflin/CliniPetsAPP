package cl.clinipets.feature.veterinario.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import java.util.UUID

@Composable
fun VeterinarioOnboardingRoute(
    onBack: () -> Unit,
    onCompletado: (UUID) -> Unit,
    viewModel: VeterinarioOnboardingViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is VeterinarioOnboardingEvento.RegistroCompleto -> onCompletado(evento.perfilId)
            }
        }
    }

    VeterinarioOnboardingScreen(
        estado = estado,
        onBack = onBack,
        onNombreChange = viewModel::onNombreChange,
        onNumeroLicenciaChange = viewModel::onNumeroLicenciaChange,
        onUbicacionSeleccionada = viewModel::onUbicacionSeleccionada,
        onLimpiarUbicacion = viewModel::limpiarUbicacion,
        onRadioChange = viewModel::onRadioChange,
        onToggleModo = viewModel::toggleModo,
        onRegistrar = viewModel::registrar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VeterinarioOnboardingScreen(
    estado: VeterinarioOnboardingUiState,
    onBack: () -> Unit,
    onNombreChange: (String) -> Unit,
    onNumeroLicenciaChange: (String) -> Unit,
    onUbicacionSeleccionada: (Double, Double) -> Unit,
    onLimpiarUbicacion: () -> Unit,
    onRadioChange: (Double) -> Unit,
    onToggleModo: (RegistrarVeterinarioRequest.ModosAtencion) -> Unit,
    onRegistrar: () -> Unit,
) {
    val chips = RegistrarVeterinarioRequest.ModosAtencion.values()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Ser veterinario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Completa tus datos para activar tu cuenta de veterinario.")
            estado.error?.let { error ->
                Text(text = error.mensaje ?: "No pudimos completar el registro.")
            }

            OutlinedTextField(
                value = estado.nombreCompleto,
                onValueChange = onNombreChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Nombre completo") },
                isError = estado.nombreError != null,
                supportingText = estado.nombreError?.let { { Text(text = it) } },
            )

            OutlinedTextField(
                value = estado.numeroLicencia,
                onValueChange = onNumeroLicenciaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Número de licencia (opcional)") },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Modos de atención")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { modo ->
                        FilterChip(
                            selected = estado.modosAtencion.contains(modo),
                            onClick = { onToggleModo(modo) },
                            label = { Text(text = modo.value) },
                        )
                    }
                }
            }

            UbicacionSelector(
                estado = estado,
                onUbicacionSeleccionada = onUbicacionSeleccionada,
                onRadioChange = onRadioChange,
                onLimpiarUbicacion = onLimpiarUbicacion,
            )

            Button(
                onClick = onRegistrar,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (estado.cargando) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(text = "Registrar")
            }
        }
    }
}

@Composable
private fun UbicacionSelector(
    estado: VeterinarioOnboardingUiState,
    onUbicacionSeleccionada: (Double, Double) -> Unit,
    onRadioChange: (Double) -> Unit,
    onLimpiarUbicacion: () -> Unit,
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val permisos = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
    var tienePermisoUbicacion by remember {
        mutableStateOf(
            context.tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION) ||
                context.tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }
    val permisosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permisos ->
        tienePermisoUbicacion = permisos.values.any { it }
    }

    LaunchedEffect(Unit) {
        if (!tienePermisoUbicacion) {
            permisosLauncher.launch(permisos)
        }
    }

    LaunchedEffect(tienePermisoUbicacion) {
        if (tienePermisoUbicacion && estado.latitud == null && estado.longitud == null) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onUbicacionSeleccionada(location.latitude, location.longitude)
                    }
                }
        }
    }

    val ubicacionSeleccionada = remember(estado.latitud, estado.longitud) {
        estado.latitud?.let { lat ->
            estado.longitud?.let { lon -> LatLng(lat, lon) }
        }
    }
    val defaultPosition = remember { LatLng(-33.4489, -70.6693) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            ubicacionSeleccionada ?: defaultPosition,
            if (ubicacionSeleccionada != null) 13f else 11f,
        )
    }

    LaunchedEffect(ubicacionSeleccionada) {
        ubicacionSeleccionada?.let { destino ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(destino, 13f),
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = tienePermisoUbicacion,
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = tienePermisoUbicacion,
                zoomControlsEnabled = false,
            ),
            onMapClick = { latLng ->
                onUbicacionSeleccionada(latLng.latitude, latLng.longitude)
            },
        ) {
            ubicacionSeleccionada?.let { posicion ->
                Marker(
                    state = MarkerState(position = posicion),
                    title = "Zona de cobertura",
                )
                Circle(
                    center = posicion,
                    radius = estado.radioCoberturaKm * METROS_POR_KM,
                    fillColor = COLOR_RADIO_RELLENO,
                    strokeColor = COLOR_RADIO_BORDE,
                )
            }
        }

        if (!tienePermisoUbicacion) {
            Text(text = "Activa los permisos de ubicación para centrarte automáticamente.")
            TextButton(onClick = { permisosLauncher.launch(permisos) }) {
                Text(text = "Solicitar permiso")
            }
        }

        if (ubicacionSeleccionada != null) {
            val coords = remember(ubicacionSeleccionada) {
                String.format(
                    Locale.getDefault(),
                    "%.5f, %.5f",
                    ubicacionSeleccionada.latitude,
                    ubicacionSeleccionada.longitude,
                )
            }

            Text(text = "Coordenadas seleccionadas: $coords")
            Text(text = "Radio de cobertura: ${estado.radioCoberturaKm.toInt()} km")
            Slider(
                value = estado.radioCoberturaKm.toFloat(),
                onValueChange = { nuevo ->
                    onRadioChange(nuevo.toDouble())
                },
                valueRange = 1f..50f,
                steps = 48,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onLimpiarUbicacion) {
                    Text(text = "Limpiar selección")
                }
            }
        } else {
            Text(text = "Toca el mapa para seleccionar tu zona de atención.")
        }
    }
}

private fun Context.tienePermiso(permiso: String): Boolean =
    ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED

private const val METROS_POR_KM = 1000.0
private val COLOR_RADIO_RELLENO = Color(0x334CAF50)
private val COLOR_RADIO_BORDE = Color(0xFF4CAF50)
