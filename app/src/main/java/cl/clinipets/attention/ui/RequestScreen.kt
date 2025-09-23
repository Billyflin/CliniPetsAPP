package cl.clinipets.attention.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import cl.clinipets.attention.model.VetLite
import cl.clinipets.attention.presentation.AttentionViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun RequestScreen(
    vm: AttentionViewModel = hiltViewModel(), onVetTap: (VetLite) -> Unit = {}
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { MapsInitializer.initialize(ctx) }

    val permission = Manifest.permission.ACCESS_FINE_LOCATION
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val reqPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission.value = granted
            if (granted) vm.startRealtime() else
                Toast.makeText(ctx, "Se requiere ubicación", Toast.LENGTH_SHORT).show()
        }

    LaunchedEffect(Unit) {
        if (hasPermission.value) vm.startRealtime() else reqPermission.launch(permission)
    }

    val camera = rememberCameraPositionState()

    if (state.loading) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Solicitar atención") }) }
        ) { pads ->
            Column(
                modifier = Modifier
                    .padding(pads)
                    .fillMaxSize()
            ) {
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        val myLoc = state.location
                        if (myLoc != null) {
                            val nearestVetLoc = remember(state.vets) {
                                state.vets
                                    .filter { it.location != null }
                                    .minByOrNull { it.distanceMeters }
                                    ?.location
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
                                    mapToolbarEnabled = false,
                                    zoomControlsEnabled = false
                                )
                            ) {
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

                                MapEffect(myLoc, nearestVetLoc) { map ->
                                    if (nearestVetLoc != null && hasPermission.value) {
                                        val me = LatLng(myLoc.lat, myLoc.lng)
                                        val vet = LatLng(nearestVetLoc.lat, nearestVetLoc.lng)
                                        val bounds = LatLngBounds.builder()
                                            .include(me).include(vet).build()
                                        val padding = 80
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLngBounds(bounds, padding)
                                        )
                                    } else if (hasPermission.value) {
                                        val me = LatLng(myLoc.lat, myLoc.lng)
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(me, 15f)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                state.error ?: "Activa la ubicación para continuar",
                                Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetSelector(
    pets: List<Pair<String, String>>, selectedId: String?, onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = pets.firstOrNull { it.first == selectedId }?.second ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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

private fun formatMeters(m: Int): String =
    if (m < 1000) "$m m" else String.format("%.1f km", m / 1000.0)
