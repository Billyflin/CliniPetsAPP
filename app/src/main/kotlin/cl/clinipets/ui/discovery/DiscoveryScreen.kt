package cl.clinipets.ui.discovery

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.DiscoveryRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Procedimiento
import cl.clinipets.openapi.models.Procedimiento.ModosHabilitados
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    onBack: () -> Unit,
    onContinuarReserva: (mascotaId: UUID, procedimientoSku: String, modo: DiscoveryRequest.ModoAtencion, lat: Double?, lng: Double?, veterinarioId: UUID?, precioSugerido: Int?) -> Unit,
    vm: DiscoveryViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.loadFormData()
        // Intentar cargar ubicación del usuario automáticamente
        cargarUbicacionUsuario(context) { lat, lng ->
            vm.setUbicacionUsuario(lat, lng)
        }
    }

    // Determinar procedimiento seleccionado para extraer modos habilitados (enum del modelo)
    val procedimientoSeleccionado = ui.procedimientos.firstOrNull { it.sku == ui.procedimientoSeleccionadoSku }
    val modosHabilitados: Set<ModosHabilitados>? = procedimientoSeleccionado?.modosHabilitados?.toSet()

    fun modoHabilitado(modo: DiscoveryRequest.ModoAtencion): Boolean {
        val mh = modosHabilitados ?: return true // si no hay restricciones, todos permitidos
        return mh.any { it.name == modo.name }
    }

    // Si hay procedimiento y el modo actual no está habilitado, forzar uno válido
    SideEffect {
        if (procedimientoSeleccionado != null && modosHabilitados != null && modosHabilitados.isNotEmpty()) {
            val actual = ui.modoAtencion
            if (!modoHabilitado(actual)) {
                val nuevo = DiscoveryRequest.ModoAtencion.entries.firstOrNull { modoHabilitado(it) }
                if (nuevo != null) {
                    vm.setModoAtencion(nuevo)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitar servicio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ui.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            ui.error?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = "Elige una mascota, un servicio y el modo de atención.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MascotaSelector(
                mascotas = ui.mascotas,
                selectedId = ui.mascotaSeleccionadaId,
                onSelect = vm::setMascotaSeleccionada
            )

            ProcedimientoSelector(
                procedimientos = ui.procedimientos,
                selectedSku = ui.procedimientoSeleccionadoSku,
                onSelect = vm::setProcedimientoSeleccionado
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val todosLosModos = DiscoveryRequest.ModoAtencion.entries.toTypedArray()
                val modosParaMostrar = todosLosModos.filter { modoHabilitado(it) }

                modosParaMostrar.forEach { modo ->
                    FilterChip(
                        selected = ui.modoAtencion == modo,
                        onClick = { vm.setModoAtencion(modo) },
                        label = { Text(modo.name) }
                    )
                }
            }

            // Botones principales de buscar/continuar (mantener)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.buscar() },
                    enabled = !ui.isSubmitting && ui.mascotaSeleccionadaId != null && !ui.procedimientoSeleccionadoSku.isNullOrBlank()
                ) {
                    Text("Buscar veterinarios")
                }
                Button(
                    onClick = {
                        val id = ui.mascotaSeleccionadaId
                        val sku = ui.procedimientoSeleccionadoSku
                        if (id != null && !sku.isNullOrBlank()) {
                            onContinuarReserva(id, sku, ui.modoAtencion, ui.latitud, ui.longitud, null, null)
                        }
                    },
                    enabled = ui.mascotaSeleccionadaId != null && !ui.procedimientoSeleccionadoSku.isNullOrBlank()
                ) {
                    Text("Continuar a reserva")
                }
            }

            // Lista de resultados: cada tarjeta permite reservar directamente con ese veterinario
            if (ui.resultados.isNotEmpty()) {
                Text("Resultados", style = MaterialTheme.typography.titleSmall)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ui.resultados) { v ->
                        Card {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(v.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    v.distanciaKm?.let { Text(String.format("%.1f km", it), style = MaterialTheme.typography.bodySmall) }
                                    v.precio?.let { Text("$it", style = MaterialTheme.typography.bodySmall) }
                                }
                                // Mostrar modos habilitados del proveedor
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    v.modosAtencion.forEach { m ->
                                        FilterChip(selected = ui.modoAtencion == m, onClick = { vm.setModoAtencion(m) }, label = { Text(m.name) })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val id = ui.mascotaSeleccionadaId
                                        val sku = ui.procedimientoSeleccionadoSku
                                        if (id != null && !sku.isNullOrBlank()) {
                                            onContinuarReserva(id, sku, ui.modoAtencion, ui.latitud, ui.longitud, v.id, v.precio)
                                        }
                                    }) { Text("Reservar aquí") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MascotaSelector(
    mascotas: List<Mascota>,
    selectedId: UUID?,
    onSelect: (UUID?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = mascotas.firstOrNull { it.id == selectedId }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Mascota", style = MaterialTheme.typography.titleSmall)
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Text(
                text = selected?.nombre ?: "Selecciona mascota",
                modifier = Modifier.padding(12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (expanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    mascotas.forEach { m ->
                        Text(
                            text = m.nombre,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(m.id)
                                    expanded = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcedimientoSelector(
    procedimientos: List<Procedimiento>,
    selectedSku: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = procedimientos.firstOrNull { it.sku == selectedSku }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Servicio", style = MaterialTheme.typography.titleSmall)
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Text(
                text = selected?.let { "${it.nombre} (${it.sku})" } ?: "Selecciona servicio",
                modifier = Modifier.padding(12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (expanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    procedimientos.forEach { p ->
                        Text(
                            text = "${p.nombre} (${p.sku})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(p.sku)
                                    expanded = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun cargarUbicacionUsuario(
    context: android.content.Context,
    onLocated: (Double, Double) -> Unit
) {
    try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocated(loc.latitude, loc.longitude)
                } else {
                    fused.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            onLocated(last.latitude, last.longitude)
                        } else {
                            Log.w("DiscoveryScreen", "No se pudo obtener ubicación (loc y lastLocation nulos)")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DiscoveryScreen", "Error obteniendo ubicación", e)
            }
    } catch (e: SecurityException) {
        Log.e("DiscoveryScreen", "Permiso de ubicación no concedido", e)
    }
}
