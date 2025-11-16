package cl.clinipets.ui.junta

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.UUID

@Composable
fun JuntaComposable(
    reservaId: UUID,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: JuntaViewModel = hiltViewModel()
) {
    LaunchedEffect(reservaId) { vm.iniciar(reservaId) }
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    var serviceStarted by remember { mutableStateOf(false) }
    LaunchedEffect(ui.isVet, ui) {
        if (ui.isVet && !serviceStarted && ui.reservaId != null) {
            vm.iniciarServicioFondo(context)
            serviceStarted = true
        }
    }
    JuntaScreen(
        ui = ui,
        onToggleShare = { vm.setShareLocation(it) },
        onBack = onBack,
        onRefreshReserva = { vm.refreshReserva() },
        modifier = modifier,
        vm = vm
    )
}

private fun distanciaKm(a: LatLng, b: LatLng): Double {
    val R = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val sinLat = Math.sin(dLat / 2)
    val sinLon = Math.sin(dLon / 2)
    val h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon
    return 2 * R * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuntaScreen(
    ui: JuntaViewModel.UiState,
    onToggleShare: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRefreshReserva: () -> Unit,
    modifier: Modifier = Modifier,
    vm: JuntaViewModel
) {
    val reserva = ui.reserva
    val vetPosActual = ui.ultimasPosiciones.firstOrNull()

    val cameraPositionState = rememberCameraPositionState()
    LaunchedEffect(reserva?.lat, reserva?.lng, vetPosActual?.lat, vetPosActual?.lng) {
        val destino = if (reserva?.lat != null && reserva.lng != null) LatLng(reserva.lat, reserva.lng) else null
        val vet = vetPosActual?.let { LatLng(it.lat, it.lng) }
        if (destino != null && vet != null) {
            val builder = LatLngBounds.builder().include(destino).include(vet)
            val bounds = builder.build()
            // Intentar mover cámara mostrando ambos puntos con padding
            runCatching {
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }.onFailure {
                // Fallback a centro si falla (por ej. si mapa aún no mide)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng((destino.latitude + vet.latitude)/2.0, (destino.longitude + vet.longitude)/2.0),
                    12f
                )
            }
        } else {
            val target = destino ?: vet ?: LatLng(-33.4489, -70.6693)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(target, 14f)
        }
    }

    val centerRequest = remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // Mover cámara cuando centerRequest cambia
    LaunchedEffect(centerRequest.value) {
        centerRequest.value?.let { (lat, lng) ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 16f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Junta / Reserva") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = onRefreshReserva, enabled = !ui.loadingReserva) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refrescar reserva")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                vm.obtenerMiUbicacion { lat, lng -> centerRequest.value = lat to lng }
            }) {
                Icon(Icons.Filled.LocationSearching, contentDescription = "Centrar en mi ubicación")
            }
        }
    ) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding)) {
            ui.error?.let { err ->
                AssistChip(onClick = {}, label = { Text(err) })
            }
            if (ui.loadingReserva) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (ui.conectado) "Conectado" else "Conectando...", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(16.dp))
                if (ui.isVet) {
                    FilterChip(
                        selected = ui.shareEnabled,
                        onClick = { onToggleShare(!ui.shareEnabled) },
                        label = { Text(if (ui.shareEnabled) "Compartiendo ubicación" else "Compartir ubicación") }
                    )
                }
            }

            reserva?.let { r ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Reserva #${r.id}", style = MaterialTheme.typography.titleMedium)
                        Text("Fecha: ${r.fecha} ${r.horaInicio}-${r.horaFin}")
                        Text("Modo: ${r.modoAtencion}")
                        Text("Estado: ${r.estado}")
                        r.direccionTexto?.let { Text("Dirección: $it") }
                        if (r.lat != null && r.lng != null) {
                            Text("Ubicación reservada: ${"%.5f".format(r.lat)} , ${"%.5f".format(r.lng)}")
                        } else {
                            Text("Reserva sin coordenadas (lat/lng)")
                        }
                        r.referencias?.let { Text("Referencias: $it") }
                        if (r.lat != null && r.lng != null && vetPosActual != null) {
                            val dist = distanciaKm(LatLng(r.lat, r.lng), LatLng(vetPosActual.lat, vetPosActual.lng))
                            Text("Distancia vet-destino: ${"%.2f".format(dist)} km")
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    if (reserva?.lat != null && reserva.lng != null) {
                        Marker(
                            state = MarkerState(position = LatLng(reserva.lat, reserva.lng)),
                            title = "Reserva",
                            snippet = reserva.direccionTexto ?: "Destino"
                        )
                    }
                    if (vetPosActual != null) {
                        Marker(
                            state = MarkerState(position = LatLng(vetPosActual.lat, vetPosActual.lng)),
                            title = "Veterinario",
                            snippet = vetPosActual.speed?.let { "Vel: ${"%.1f".format(it)} km/h" } ?: ""
                        )
                        val trayectoria = ui.ultimasPosiciones.take(25).map { LatLng(it.lat, it.lng) }
                        if (trayectoria.size >= 2) {
                            Polyline(points = trayectoria, color = Color(0xFF1976D2), width = 6f)
                        }
                    }
                }
            }

            Text("Últimas posiciones (${ui.ultimasPosiciones.size})", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(ui.ultimasPosiciones) { p ->
                    ListItem(
                        headlineContent = { Text("${"%.5f".format(p.lat)}, ${"%.5f".format(p.lng)}") },
                        supportingContent = {
                            Text(buildString {
                                p.speed?.let { append("Vel: ${"%.1f".format(it)} km/h  ") }
                                p.heading?.let { append("Heading: ${"%.0f".format(it)}°  ") }
                                p.ts?.let { append("ts: $it") }
                            })
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
