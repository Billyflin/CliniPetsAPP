package cl.clinipets.ui.discovery

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import cl.clinipets.core.prefs.MapPrefs
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.discovery.VetNearby
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDiscoverScreen(
    repo: DiscoveryRepository,
    onOpenVet: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val vm = remember { MapDiscoverViewModel(repo) }
    val ui = vm.state.collectAsState().value
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val hasLocationPerms = remember { mutableStateOf(false) }
    val requestPermsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPerms.value = fine || coarse
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasLocationPerms.value = fine || coarse
        if (!hasLocationPerms.value) {
            requestPermsLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    LaunchedEffect(hasLocationPerms.value) {
        // Obtén última ubicación o fallback a DataStore o default
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        val defaultCenter = LatLng(-33.45, -70.66)
        var center = defaultCenter
        if (hasLocationPerms.value) {
            runCatching { fused.lastLocation.awaitOrNull() } // extension abajo
                .getOrNull()?.let { loc ->
                    center = LatLng(loc.latitude, loc.longitude)
                }
        } else {
            val savedLat = MapPrefs.lastLat(ctx).first()
            val savedLon = MapPrefs.lastLon(ctx).first()
            if (savedLat != null && savedLon != null) {
                center = LatLng(savedLat, savedLon)
            }
        }
        vm.setCenter(center.latitude, center.longitude)
        vm.buscar(center.latitude, center.longitude)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(ui.centerLat, ui.centerLon), 12f)
    }

    val showSheet = remember { mutableStateOf<VetNearby?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        floatingActionButton = {
            Box(Modifier.padding(16.dp)) {
                FloatingActionButton(onClick = {
                    val center = cameraPositionState.position.target
                    vm.buscar(center.latitude, center.longitude)
                    scope.launch {
                        MapPrefs.setLastPosition(ctx, center.latitude, center.longitude)
                        MapPrefs.setLastRadio(ctx, ui.radioMeters)
                    }
                }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Filled.Search, contentDescription = "Buscar aquí")
                }
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val fused = LocationServices.getFusedLocationProviderClient(ctx)
                            val loc = runCatching { fused.lastLocation.awaitOrNull() }.getOrNull()
                            if (loc != null) {
                                val here = LatLng(loc.latitude, loc.longitude)
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(here, 14f)
                            } else {
                                snackbar.showSnackbar("Ubicación no disponible", withDismissAction = true, duration = SnackbarDuration.Short)
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(top = 72.dp)
                        .align(Alignment.TopEnd),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Mi ubicación")
                }
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).padding(contentPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Círculo de radio
                val center = LatLng(ui.centerLat, ui.centerLon)
                com.google.maps.android.compose.Circle(
                    center = center,
                    radius = ui.radioMeters.toDouble(),
                    strokeColor = Color(red = 0x00, green = 0xAE, blue = 0xEF, alpha = 0x55),
                    fillColor = Color(red = 0x00, green = 0xAE, blue = 0xEF, alpha = 0x22)
                )
                // Markers
                ui.vets.forEach { vet ->
                    val markerColor = if (vet.openNow) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                    Marker(
                        state = MarkerState(LatLng(vet.lat, vet.lon)),
                        title = vet.nombre,
                        snippet = vet.ofertaNombre?.let { name ->
                            val precio = vet.ofertaPrecioMin?.let { p -> " - $" + p.toString() } ?: ""
                            "$name$precio"
                        },
                        onClick = {
                            showSheet.value = vet
                            true
                        },
                        icon = BitmapDescriptorFactory.defaultMarker(markerColor)
                    )
                }
            }

            if (ui.isLoading) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }

            ui.error?.let { err ->
                LaunchedEffect(err) {
                    snackbar.showSnackbar(err)
                }
            }
        }
    }

    showSheet.value?.let { vet ->
        ModalBottomSheet(onDismissRequest = { showSheet.value = null }, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
            Box(Modifier.padding(16.dp)) {
                Text(vet.nombre, style = MaterialTheme.typography.titleLarge)
                Text(if (vet.openNow) "Abierto ahora" else "Cerrado", color = if (vet.openNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                vet.ofertaNombre?.let { n ->
                    val precio = vet.ofertaPrecioMin?.let { p -> " - $" + p.toString() } ?: ""
                    Text("Oferta: $n$precio")
                }
                Button(onClick = { onOpenVet(vet.id) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Ver detalle")
                }
            }
        }
    }
}

// await helper para Task<Location>
private suspend fun com.google.android.gms.tasks.Task<android.location.Location>.awaitOrNull(): android.location.Location? =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        this.addOnSuccessListener { cont.resume(it, onCancellation = null) }
        this.addOnFailureListener { cont.resume(null, onCancellation = null) }
        this.addOnCanceledListener { cont.resume(null, onCancellation = null) }
    }
