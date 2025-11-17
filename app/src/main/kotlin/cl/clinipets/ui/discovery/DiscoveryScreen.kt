package cl.clinipets.ui.discovery

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import cl.clinipets.openapi.models.DiscoveryRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Procedimiento
import cl.clinipets.openapi.models.Procedimiento.ModosHabilitados
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.UUID

private val fieldShape = RoundedCornerShape(16.dp)
private val chipShape = RoundedCornerShape(12.dp)
private val buttonShape = RoundedCornerShape(24.dp)
private val resultCardShape = RoundedCornerShape(28.dp)

private fun DiscoveryRequest.ModoAtencion.toFriendlyString(): String {
    return this.name.split('_').joinToString(" ") {
        it.lowercase().replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
}

private enum class SortType {
    NONE,
    DISTANCE,
    PRICE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    onBack: () -> Unit,
    onContinuarReserva: (
        mascotaId: UUID,
        procedimientoSku: String,
        modo: DiscoveryRequest.ModoAtencion,
        lat: Double?,
        lng: Double?,
        veterinarioId: UUID?,
        precioSugerido: Int?,
        veterinarioNombre: String?
    ) -> Unit,
    vm: DiscoveryViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    var sortType by remember { mutableStateOf(SortType.NONE) }

    LaunchedEffect(Unit) {
        vm.loadFormData()
        cargarUbicacionUsuario(context) { lat, lng ->
            vm.setUbicacionUsuario(lat, lng)
        }
    }

    val procedimientoSeleccionado =
        ui.procedimientos.firstOrNull { it.sku == ui.procedimientoSeleccionadoSku }
    val modosHabilitADOS: Set<ModosHabilitados>? =
        procedimientoSeleccionado?.modosHabilitados?.toSet()

    fun modoHabilitado(modo: DiscoveryRequest.ModoAtencion): Boolean {
        val mh = modosHabilitADOS ?: return true
        return mh.any { it.name == modo.name }
    }

    SideEffect {
        if (procedimientoSeleccionado != null && modosHabilitADOS != null && modosHabilitADOS.isNotEmpty()) {
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
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        topBar = {
            TopAppBar(
                title = { Text("Solicitar servicio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (ui.isLoading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }

                    ui.error?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(12.dp)
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

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Modo de atención", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val todosLosModos = DiscoveryRequest.ModoAtencion.entries.toTypedArray()
                            val modosParaMostrar = todosLosModos.filter { modoHabilitado(it) }

                            modosParaMostrar.forEach { modo ->
                                FilterChip(
                                    selected = ui.modoAtencion == modo,
                                    onClick = { vm.setModoAtencion(modo) },
                                    label = { Text(modo.toFriendlyString()) },
                                    shape = chipShape
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.buscar() },
                            enabled = !ui.isSubmitting && ui.mascotaSeleccionadaId != null && !ui.procedimientoSeleccionadoSku.isNullOrBlank(),
                            shape = buttonShape
                        ) {
                            if (ui.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Buscar veterinarios")
                            }
                        }
                        Button(
                            onClick = {
                                val id = ui.mascotaSeleccionadaId
                                val sku = ui.procedimientoSeleccionadoSku
                                if (id != null && !sku.isNullOrBlank()) {
                                    onContinuarReserva(
                                        id,
                                        sku,
                                        ui.modoAtencion,
                                        ui.latitud,
                                        ui.longitud,
                                        null,
                                        null,
                                        null
                                    )
                                }
                            },
                            enabled = !ui.isSubmitting && ui.mascotaSeleccionadaId != null && !ui.procedimientoSeleccionadoSku.isNullOrBlank(),
                            shape = buttonShape
                        ) {
                            Text("Continuar a reserva")
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortedResultados = remember(ui.resultados, sortType) {
                            when (sortType) {
                                SortType.NONE -> ui.resultados
                                SortType.DISTANCE -> ui.resultados.sortedBy { it.distanciaKm ?: Double.MAX_VALUE }
                                SortType.PRICE -> ui.resultados.sortedBy { it.precio ?: Int.MAX_VALUE }
                            }
                        }

                        if (ui.resultados.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Resultados", style = MaterialTheme.typography.titleSmall)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = sortType == SortType.DISTANCE,
                                        onClick = {
                                            sortType =
                                                if (sortType == SortType.DISTANCE) SortType.NONE else SortType.DISTANCE
                                        },
                                        label = { Text("Distancia") },
                                        shape = chipShape
                                    )
                                    FilterChip(
                                        selected = sortType == SortType.PRICE,
                                        onClick = {
                                            sortType =
                                                if (sortType == SortType.PRICE) SortType.NONE else SortType.PRICE
                                        },
                                        label = { Text("Precio") },
                                        shape = chipShape
                                    )
                                }
                            }

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(sortedResultados) { v ->
                                    Card(
                                        shape = resultCardShape,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(v.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                v.distanciaKm?.let {
                                                    Text(
                                                        String.format("%.1f km", it),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                v.precio?.let {
                                                    Text(
                                                        "$it",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                v.modosAtencion.forEach { m ->
                                                    Surface(
                                                        shape = MaterialTheme.shapes.small,
                                                        tonalElevation = 2.dp
                                                    ) {
                                                        Text(
                                                            text = m.toFriendlyString(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        val id = ui.mascotaSeleccionadaId
                                                        val sku = ui.procedimientoSeleccionadoSku
                                                        if (id != null && !sku.isNullOrBlank()) {
                                                            onContinuarReserva(
                                                                id,
                                                                sku,
                                                                ui.modoAtencion,
                                                                ui.latitud,
                                                                ui.longitud,
                                                                v.id,
                                                                v.precio,
                                                                v.nombreCompleto
                                                            )
                                                        }
                                                    },
                                                    shape = buttonShape
                                                ) { Text("Reservar aquí") }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (!ui.isLoading && !ui.isSubmitting && ui.resultados.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No se encontraron veterinarios con esos criterios.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MascotaSelector(
    mascotas: List<Mascota>,
    selectedId: UUID?,
    onSelect: (UUID?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = mascotas.firstOrNull { it.id == selectedId }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Mascota", style = MaterialTheme.typography.titleSmall)

        if (mascotas.isEmpty()) {
            Text(
                "No tienes mascotas registradas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier
        ) {
            OutlinedTextField(
                value = selected?.nombre ?: "Selecciona mascota",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = fieldShape,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                maxLines = 1
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                mascotas.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.nombre) },
                        onClick = {
                            onSelect(m.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcedimientoSelector(
    procedimientos: List<Procedimiento>,
    selectedSku: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = procedimientos.firstOrNull { it.sku == selectedSku }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Servicio", style = MaterialTheme.typography.titleSmall)

        if (procedimientos.isEmpty()) {
            Text(
                "No hay servicios disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier
        ) {
            OutlinedTextField(
                value = selected?.let { "${it.nombre} (${it.sku})" } ?: "Selecciona servicio",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = fieldShape,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                maxLines = 1
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                procedimientos.forEach { p ->
                    DropdownMenuItem(
                        text = { Text("${p.nombre} (${p.sku})") },
                        onClick = {
                            onSelect(p.sku)
                            expanded = false
                        }
                    )
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
                            Log.w(
                                "DiscoveryScreen",
                                "No se pudo obtener ubicación (loc y lastLocation nulos)"
                            )
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
