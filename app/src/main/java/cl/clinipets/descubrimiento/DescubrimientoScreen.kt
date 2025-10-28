package cl.clinipets.descubrimiento

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.clinipets.network.Oferta
import cl.clinipets.network.Procedimiento
import cl.clinipets.network.VetItem
import com.google.android.gms.location.LocationServices
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.LatLng

@Composable
fun DescubrimientoScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory, onReservarDesdeOferta: (sku: String, especie: String?) -> Unit) {
    val vm: DescubrimientoViewModel = viewModel(factory = viewModelFactory)
    val vets by vm.veterinarios.collectAsState()
    val procedimientos by vm.procedimientos.collectAsState()
    val ofertas by vm.ofertas.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var query by remember { mutableStateOf("") }
    var especie by remember { mutableStateOf("") }
    var abiertoAhora by remember { mutableStateOf(false) }

    // Ubicación actual y búsqueda con geo
    GeoHeader(onGeoReady = { lat, lng ->
        vm.buscarVeterinarios(lat = lat, lng = lng, radioKm = 10, especie = if (especie.isBlank()) null else especie, abiertoAhora = abiertoAhora)
    })

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Descubrimiento", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar procedimiento") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Abierto ahora", modifier = Modifier.padding(end = 8.dp))
            Switch(checked = abiertoAhora, onCheckedChange = {
                abiertoAhora = it
                vm.buscarVeterinarios(lat = null, lng = null, radioKm = 10, especie = if (especie.isBlank()) null else especie, abiertoAhora = abiertoAhora)
            })
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { vm.cargarProcedimientos(especie = if (especie.isBlank()) null else especie, q = if (query.isBlank()) null else query) }, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar procedimientos")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Procedimientos", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(procedimientos) { p: Procedimiento -> ProcedimientoRow(p) } }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { vm.buscarVeterinarios(null, null, 10, especie = if (especie.isBlank()) null else especie, abiertoAhora = abiertoAhora) }, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar veterinarios cercanos")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Mapa de veterinarios", style = MaterialTheme.typography.titleMedium)
        VetsMap(vets)

        Spacer(modifier = Modifier.height(12.dp))
        Text("Veterinarios", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(vets) { v: VetItem -> VetRow(v) } }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { vm.buscarOfertas(especie = if (especie.isBlank()) null else especie) }, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar ofertas")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Ofertas", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(ofertas) { o: Oferta -> OfertaRow(o) { sku, esp -> onReservarDesdeOferta(sku, esp) } } }

        if (!error.isNullOrEmpty()) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }
        if (isLoading) {
            Text("Cargando...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ProcedimientoRow(p: Procedimiento) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(p.nombre, style = MaterialTheme.typography.titleMedium)
        Text("Duración: ${p.duracionMinutos} min", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VetRow(v: VetItem) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Text(v.nombre, style = MaterialTheme.typography.titleMedium)
        Text("Modos: ${v.modos.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
        v.distanciaKm?.let { Text("A ${String.format(java.util.Locale.getDefault(), "%.1f", it)} km", style = MaterialTheme.typography.bodySmall) }
        if (v.verificado) Text("Verificado", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun OfertaRow(o: Oferta, onReservar: (sku: String, especie: String?) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(o.procedimientoNombre + " - $${o.precio}", style = MaterialTheme.typography.titleMedium)
        Text("Duración: ${o.duracionMinutos} min", style = MaterialTheme.typography.bodySmall)
        // Acción primaria: Reservar con esta oferta
        Button(onClick = {
            val especie = o.compatibleEspecies.firstOrNull()
            onReservar(o.procedimientoSku, especie)
        }, modifier = Modifier.padding(top = 6.dp)) {
            Text("Reservar con esta oferta")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun GeoHeader(onGeoReady: (Double?, Double?) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                onGeoReady(loc?.latitude, loc?.longitude)
            }.addOnFailureListener {
                onGeoReady(null, null)
            }
        } else {
            onGeoReady(null, null)
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

@Composable
private fun VetsMap(vets: List<VetItem>) {
    val cameraPositionState = rememberCameraPositionState()
    GoogleMap(modifier = Modifier.fillMaxWidth().height(200.dp), cameraPositionState = cameraPositionState) {
        vets.forEach { v ->
            if (v.lat != null && v.lng != null) {
                Marker(state = MarkerState(position = LatLng(v.lat, v.lng)), title = v.nombre)
            }
        }
    }
}
