package cl.clinipets.attention.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.attention.model.VetLite
import cl.clinipets.attention.presentation.AttentionViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    vm: AttentionViewModel = hiltViewModel(), onVetTap: (VetLite) -> Unit = {}
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    // Inicializa Maps SDK para evitar NPE de CameraUpdateFactory
    LaunchedEffect(Unit) { MapsInitializer.initialize(ctx) }

    // Permisos de ubicación
    val permission = Manifest.permission.ACCESS_FINE_LOCATION
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                ctx, permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val reqPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission.value = granted
            if (granted) vm.startRealtime() else Toast.makeText(
                ctx, "Se requiere ubicación", Toast.LENGTH_SHORT
            ).show()
        }
    LaunchedEffect(Unit) {
        if (hasPermission.value) vm.startRealtime() else reqPermission.launch(permission)
    }

    val camera = rememberCameraPositionState()
    val myMarker = rememberMarkerState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Solicitar atención") }) }) { pads ->

        Column(
            modifier = Modifier
                .padding(pads)
                .fillMaxSize()
        ) {

            // Selector de mascota
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                PetSelector(
                    pets = state.pets,
                    selectedId = state.selectedPetId,
                    onSelect = vm::onPetSelected
                )
            }

            // Mapa con mi posición y vets en vivo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(Modifier.fillMaxSize()) {
                    val myLoc = state.location
                    if (myLoc != null) {
                        LaunchedEffect(myLoc) {
                            val here = LatLng(myLoc.lat, myLoc.lng)
                            myMarker.position = here
                            if (hasPermission.value) {
                                camera.animate(CameraUpdateFactory.newLatLngZoom(here, 15f))
                            }
                        }
                        GoogleMap(
                            modifier = Modifier.matchParentSize(),
                            cameraPositionState = camera,
                            properties = MapProperties(
                                isMyLocationEnabled = hasPermission.value,
                                maxZoomPreference = 20f,
                                minZoomPreference = 12f
                            ),
                            uiSettings = MapUiSettings(
                                myLocationButtonEnabled = true,

                                zoomControlsEnabled = false
                            )
                        ) {

                            // vets cercanos (como pins)
                            state.vets.forEach { vet ->
                                val vLoc = vet.location ?: return@forEach
                                val markerState = rememberUpdatedMarkerState(
                                    position = LatLng(vLoc.lat, vLoc.lng)
                                )
                                Marker(
                                    state = markerState,
                                    title = vet.name,
                                    snippet = "${vet.rating}★ · ${formatMeters(vet.distanceMeters)}"
                                )
                            }
                        }
                    } else {
                        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                        else Text(
                            state.error ?: "Activa la ubicación para continuar",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Lista en vivo de vets
            Text(
                text = "Veterinarios cercanos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.vets, key = { it.id }) { vet ->
                    VetCard(vet = vet, onClick = { onVetTap(vet) })
                }
            }
        }
    }
}

/* ------------------------ Subcomposables ------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetSelector(
    pets: List<Pair<String, String>>, selectedId: String?, onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = pets.firstOrNull { it.first == selectedId }?.second ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            leadingIcon = { Icon(Icons.Default.Pets, contentDescription = null) },
            label = { Text("Mascota") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) })
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            pets.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    onSelect(id)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun VetCard(vet: VetLite, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(), onClick = onClick
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(vet.name, style = MaterialTheme.typography.titleMedium)
                Text("${vet.rating} ★", style = MaterialTheme.typography.bodySmall)
            }
            Text(formatMeters(vet.distanceMeters), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/* ------------------------ Helpers ------------------------ */

private fun formatMeters(m: Int): String =
    if (m < 1000) "$m m" else String.format("%.1f km", m / 1000.0)
