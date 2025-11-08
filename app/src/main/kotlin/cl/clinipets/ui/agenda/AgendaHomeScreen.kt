@file:Suppress("UnusedParameter")
package cl.clinipets.ui.agenda

// Pantalla unificada de Agenda (hub) — reemplaza pantallas legacy.

import android.annotation.SuppressLint
import android.location.LocationManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CrearOfertaRequest
import cl.clinipets.openapi.models.CrearSolicitudRequest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaHomeScreen(
    isVet: Boolean,
    onBack: () -> Unit,
    onNavigateToMiDisponibilidad: () -> Unit,
    onNavigateToMiCatalogo: () -> Unit,
    ofertasVm: OfertasViewModel = hiltViewModel(),
    reservasVm: ReservasViewModel = hiltViewModel(),
    solicitudesVm: SolicitudesViewModel = hiltViewModel()
) {
    // Para ambos roles iniciamos en la primera pestaña.
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = if (isVet) listOf("Solicitudes", "Nueva solicitud", "Reservas") else listOf("Nueva solicitud", "Reservas")

    val ofertasUi by ofertasVm.ui.collectAsState()
    val reservasUi by reservasVm.ui.collectAsState()
    val solUi by solicitudesVm.ui.collectAsState()
    var selectedSolicitud by remember { mutableStateOf<SolicitudDisponibleUi?>(null) }

    LaunchedEffect(isVet) {
        if (isVet) ofertasVm.refrescarSolicitudes()
        reservasVm.refreshIfNeeded()
        solicitudesVm.loadFormData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = {
                    if (isVet) {
                        IconButton(onClick = { ofertasVm.refrescarSolicitudes() }) { Icon(Icons.Filled.Refresh, contentDescription = null) }
                        FilledTonalIconButton(onClick = onNavigateToMiDisponibilidad) { Icon(Icons.Filled.EditCalendar, contentDescription = null) }
                        FilledTonalIconButton(onClick = onNavigateToMiCatalogo) { Icon(Icons.Filled.ManageAccounts, contentDescription = null) }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(label) })
                }
            }
            if (isVet) {
                when (selectedTab) {
                    0 -> SolicitudesVetList(ofertasUi, onOfertar = { selectedSolicitud = it }, onRefrescar = { ofertasVm.refrescarSolicitudes() })
                    1 -> SolicitudServicioForm(solUi = solUi, vm = solicitudesVm)
                    2 -> ReservasResumen(ui = reservasUi, onRefrescar = { reservasVm.refreshIfNeeded(force = true) })
                }
            } else {
                when (selectedTab) {
                    0 -> SolicitudServicioForm(solUi = solUi, vm = solicitudesVm)
                    1 -> ReservasResumen(ui = reservasUi, onRefrescar = { reservasVm.refreshIfNeeded(force = true) })
                }
            }
        }
    }

    selectedSolicitud?.let { solicitud ->
        OfertaDialogSimple(solicitud = solicitud, onDismiss = { selectedSolicitud = null }) { req ->
            ofertasVm.enviarOferta(solicitud.id, req) { selectedSolicitud = null }
        }
    }
}

@Composable
private fun SolicitudesVetList(
    ui: OfertasViewModel.UiState,
    onOfertar: (SolicitudDisponibleUi) -> Unit,
    onRefrescar: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onRefrescar, enabled = !ui.isLoading) { Icon(Icons.Filled.Refresh, contentDescription = null); Spacer(Modifier.padding(2.dp)); Text("Refrescar") }
            AssistChip(onClick = {}, label = { Text("Disponibles: ${ui.solicitudesDisponibles.size}") })
        }
        if (ui.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.error?.let { AssistChip(onClick = {}, label = { Text(it) }) }
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ui.solicitudesDisponibles.size) { idx ->
                val s = ui.solicitudesDisponibles[idx]
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.procedimientoNombre ?: "Procedimiento", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.mascotaNombre ?: "Mascota", style = MaterialTheme.typography.bodySmall)
                            Text(listOfNotNull(s.fecha, s.bloqueSolicitado, s.modoAtencion).joinToString(" • "), style = MaterialTheme.typography.labelSmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onOfertar(s) }) { Text("Ofertar") }
                        }
                    }
                }
            }
            if (ui.solicitudesDisponibles.isEmpty() && !ui.isLoading) {
                item { Text("No hay solicitudes por ahora", modifier = Modifier.padding(16.dp)) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ReservasResumen(
    ui: ReservasViewModel.UiState,
    onRefrescar: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Reservas", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onRefrescar, enabled = !ui.isWorking) { Icon(Icons.Filled.CalendarMonth, contentDescription = null); Spacer(Modifier.padding(2.dp)); Text("Actualizar") }
        }
        if (ui.isWorking) LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.error?.let { AssistChip(onClick = {}, label = { Text(it) }) }
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ui.ultimaReserva?.let { r ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Última reserva", fontWeight = FontWeight.Bold)
                            Text("ID: ${r.id}")
                            Text("Estado: ${r.estado}")
                            Text("Fecha: ${r.fecha} ${r.horaInicio}-${r.horaFin}")
                            Text("Cliente: ${r.cliente.nombreCompleto}")
                            Text("Mascota: ${r.mascota.nombre}")
                            Text("Procedimiento: ${r.procedimiento.nombre}")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun OfertaDialogSimple(
    solicitud: SolicitudDisponibleUi,
    onDismiss: () -> Unit,
    onSubmit: (CrearOfertaRequest) -> Unit
) {
    var precioServicio by remember { mutableStateOf("") }
    var precioLogistica by remember { mutableStateOf("0") }
    var hora by remember { mutableStateOf("10:00") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ofertar a ${solicitud.procedimientoNombre ?: "servicio"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { AssistChip(onClick = { error = null }, label = { Text(it) }) }
                OutlinedTextField(value = precioServicio, onValueChange = { precioServicio = it.filter { c -> c.isDigit() } }, label = { Text("Precio servicio") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = precioLogistica, onValueChange = { precioLogistica = it.filter { c -> c.isDigit() } }, label = { Text("Precio logística") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hora, onValueChange = { hora = it }, label = { Text("Hora propuesta (HH:mm)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ps = precioServicio.toIntOrNull(); val pl = precioLogistica.toIntOrNull()
                if (ps == null || pl == null || hora.isBlank()) { error = "Datos inválidos"; return@TextButton }
                onSubmit(CrearOfertaRequest(ps, pl, hora, null, null))
            }) { Text("Enviar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// Nuevo formulario mejorado para flujo de crear solicitud (cliente)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolicitudServicioForm(
    solUi: SolicitudesViewModel.UiState,
    vm: SolicitudesViewModel
) {
    val context = LocalContext.current
    var mascotaSel by remember { mutableStateOf(solUi.mascotas.firstOrNull()) }
    var procSel by remember { mutableStateOf(solUi.procedimientos.firstOrNull()) }
    var showMascotaDialog by remember { mutableStateOf(false) }
    var showProcDialog by remember { mutableStateOf(false) }
    var bloque by remember { mutableStateOf(CrearSolicitudRequest.BloqueSolicitado.MANANA) }
    var modo by remember { mutableStateOf(CrearSolicitudRequest.ModoAtencion.DOMICILIO) }
    var pref by remember { mutableStateOf(CrearSolicitudRequest.PreferenciaLogistica.YO_LLEVO) }
    var fecha by remember { mutableStateOf(LocalDate.now().plusDays(2)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var ubicacionAuto by remember { mutableStateOf<Pair<Double,Double>?>(null) }
    var solicitandoUbicacion by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun obtenerUbicacionActual() {
        solicitandoUbicacion = true
        runCatching {
            val lm = context.getSystemService<LocationManager>()
            val providers = lm?.getProviders(true).orEmpty()
            val loc = providers.asSequence()
                .mapNotNull { prov -> runCatching { lm?.getLastKnownLocation(prov) }.getOrNull() }
                .maxByOrNull { it.accuracy }
            loc?.let { ubicacionAuto = it.latitude to it.longitude }
        }.onFailure { }
        solicitandoUbicacion = false
    }

    Column(Modifier.fillMaxSize()) {
        if (solUi.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        solUi.error?.let { AssistChip(onClick = { vm.clearStatus() }, label = { Text(it) }) }
        solUi.successMessage?.let { AssistChip(onClick = { vm.clearStatus() }, label = { Text(it) }) }
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Solicitar servicio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }

            // Mascota selector (Dialog)
            item {
                OutlinedTextField(
                    value = mascotaSel?.nombre ?: "Selecciona mascota",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mascota") },
                    modifier = Modifier.fillMaxWidth().clickable { showMascotaDialog = true }
                )
            }

            // Procedimiento selector (Dialog)
            item {
                OutlinedTextField(
                    value = procSel?.nombre ?: "Selecciona servicio",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Procedimiento") },
                    modifier = Modifier.fillMaxWidth().clickable { showProcDialog = true }
                )
            }

            // Fecha picker (campo de solo lectura que abre diálogo)
            item {
                OutlinedTextField(
                    value = fecha.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )
            }

            // Bloque chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bloque", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrearSolicitudRequest.BloqueSolicitado.entries.forEach { b ->
                            FilterChip(selected = b == bloque, onClick = { bloque = b }, label = { Text(b.name) })
                        }
                    }
                }
            }
            // Modo atención
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Modo Atención", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrearSolicitudRequest.ModoAtencion.entries.forEach { m ->
                            FilterChip(selected = m == modo, onClick = { modo = m }, label = { Text(m.name) })
                        }
                    }
                }
            }
            // Preferencia logística
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Logística", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CrearSolicitudRequest.PreferenciaLogistica.entries.forEach { p ->
                            FilterChip(selected = p == pref, onClick = { pref = p }, label = { Text(p.name) })
                        }
                    }
                }
            }
            // Ubicación automática
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val ubicTxt = ubicacionAuto?.let { "Lat: %.4f, Lng: %.4f".format(it.first, it.second) } ?: "Sin ubicación"
                    Text(ubicTxt, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { obtenerUbicacionActual() }, enabled = !solicitandoUbicacion) { Text(if (solicitandoUbicacion) "Buscando..." else "Usar ubicación") }
                }
            }
            // Botón enviar
            item {
                Button(
                    onClick = {
                        val mId = mascotaSel?.id ?: return@Button
                        val sku = procSel?.sku ?: return@Button
                        val request = CrearSolicitudRequest(
                            mascotaId = mId,
                            procedimientoSku = sku,
                            modoAtencion = modo,
                            preferenciaLogistica = pref,
                            fecha = fecha,
                            bloqueSolicitado = bloque,
                            latitud = ubicacionAuto?.first ?: -33.45,
                            longitud = ubicacionAuto?.second ?: -70.66
                        )
                        vm.submitSolicitud(request) {}
                    },
                    enabled = !solUi.isSubmitting && mascotaSel != null && procSel != null
                ) { Text(if (solUi.isSubmitting) "Enviando..." else "Publicar solicitud") }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
        // Diálogos de selección y fecha
        if (showMascotaDialog) {
            AlertDialog(
                onDismissRequest = { showMascotaDialog = false },
                title = { Text("Selecciona mascota") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        solUi.mascotas.forEach { m ->
                            TextButton(onClick = { mascotaSel = m; showMascotaDialog = false }) { Text(m.nombre) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showMascotaDialog = false }) { Text("Cerrar") } }
            )
        }
        if (showProcDialog) {
            AlertDialog(
                onDismissRequest = { showProcDialog = false },
                title = { Text("Selecciona servicio") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        solUi.procedimientos.forEach { p ->
                            TextButton(onClick = { procSel = p; showProcDialog = false }) { Text(p.nombre) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showProcDialog = false }) { Text("Cerrar") } }
            )
        }
        if (showDatePicker) {
            val dateState = rememberDatePickerState()
            DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        fecha = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Ok") }
            }) {
                DatePicker(state = dateState)
            }
        }
    }
}
