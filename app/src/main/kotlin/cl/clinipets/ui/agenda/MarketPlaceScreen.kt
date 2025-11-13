package cl.clinipets.ui.agenda

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CrearOfertaRequest
import cl.clinipets.openapi.models.CrearSolicitudRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.OfertaVeterinarioResponse
import cl.clinipets.openapi.models.Procedimiento
import cl.clinipets.openapi.models.SolicitudDisponibleDto
import cl.clinipets.openapi.models.SolicitudServicioResponse
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketPlaceScreen(
    isVet: Boolean,
    onBack: () -> Unit,
    vm: MarketPlaceViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current

    // Toasts
    if (ui.error != null) {
        LaunchedEffect(ui.error) {
            Toast.makeText(ctx, ui.error, Toast.LENGTH_LONG).show()
            vm.clearStatus()
        }
    }
    if (ui.successMessage != null) {
        LaunchedEffect(ui.successMessage) {
            Toast.makeText(ctx, ui.successMessage, Toast.LENGTH_SHORT).show()
            vm.clearStatus()
        }
    }

    // Estado compartido para formulario (cliente y vet-actúa-como-cliente)
    var mascotaIdTxt by remember { mutableStateOf(TextFieldValue("")) }
    var sku by remember { mutableStateOf(TextFieldValue("VACUNA_FLOW")) }
    var selectedModo by remember { mutableStateOf(CrearSolicitudRequest.ModoAtencion.DOMICILIO) }
    var selectedPref by remember { mutableStateOf(CrearSolicitudRequest.PreferenciaLogistica.YO_LLEVO) }
    var fechaTxt by remember { mutableStateOf(TextFieldValue("")) }
    var selectedBloque by remember { mutableStateOf(CrearSolicitudRequest.BloqueSolicitado.MANANA) }
    var lat by remember { mutableStateOf(TextFieldValue("-33.45")) }
    var lon by remember { mutableStateOf(TextFieldValue("-70.66")) }
    var ofertandoPara by remember { mutableStateOf<SolicitudDisponibleDto?>(null) }
    var ofertaIdTxt by remember { mutableStateOf(TextFieldValue("")) } // reintroducido para aceptar oferta

    val onSolicitudEnviada: () -> Unit = {
        mascotaIdTxt = TextFieldValue("")
    }

    // Manejo de tabs según rol
    var vetTabIndex by remember { mutableStateOf(0) } // 0 Abiertas, 1 Mis ofertas, 2 Crear
    var clientTabIndex by remember { mutableStateOf(0) } // 0 Crear, 1 Mis solicitudes

    // Carga inicial según vista activa
    LaunchedEffect(isVet, vetTabIndex, clientTabIndex) {
        if (isVet) {
            when (vetTabIndex) {
                0 -> vm.refrescarSolicitudes()
                1 -> vm.refrescarMisOfertas()
                2 -> vm.loadFormData()
            }
        } else {
            when (clientTabIndex) {
                0 -> vm.loadFormData()
                1 -> vm.refrescarMisSolicitudes()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isVet) "Marketplace Vet" else "Marketplace") })
        }
    ) { padd ->
        Column(Modifier.padding(padd).fillMaxSize()) {
            if (isVet) {
                TabRow(selectedTabIndex = vetTabIndex) {
                    Tab(selected = vetTabIndex == 0, onClick = { vetTabIndex = 0 }, text = { Text("Solicitudes abiertas") })
                    Tab(selected = vetTabIndex == 1, onClick = { vetTabIndex = 1 }, text = { Text("Mis ofertas") })
                    Tab(selected = vetTabIndex == 2, onClick = { vetTabIndex = 2 }, text = { Text("Crear solicitud") })
                }
            } else {
                TabRow(selectedTabIndex = clientTabIndex) {
                    Tab(selected = clientTabIndex == 0, onClick = { clientTabIndex = 0 }, text = { Text("Crear solicitud") })
                    Tab(selected = clientTabIndex == 1, onClick = { clientTabIndex = 1 }, text = { Text("Mis solicitudes") })
                }
            }

            if (ui.error != null) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            }

            when {
                isVet && vetTabIndex == 0 -> VetSolicitudesTab(ui = ui, onRefresh = { vm.refrescarSolicitudes() }, onOfertar = { ofertandoPara = it })
                isVet && vetTabIndex == 1 -> MisOfertasTab(ofertas = ui.misOfertas, isLoading = ui.isLoading, onRefresh = { vm.refrescarMisOfertas() })
                isVet && vetTabIndex == 2 -> FormCrearSolicitud(
                    ui = ui,
                    mascotaIdTxt = mascotaIdTxt,
                    sku = sku,
                    mascotaSearch = "",
                    procedimientoSearch = "",
                    onMascotaSearchChange = { },
                    onProcedimientoSearchChange = { },
                    selectedModo = selectedModo,
                    selectedPref = selectedPref,
                    selectedBloque = selectedBloque,
                    fechaTxt = fechaTxt,
                    lat = lat,
                    lon = lon,
                    showAceptarSection = false,
                    onToggleAceptarSection = { },
                    ofertaIdTxt = ofertaIdTxt,
                    onOfertaIdChange = { ofertaIdTxt = it },
                    onModoChange = { selectedModo = it },
                    onPrefChange = { selectedPref = it },
                    onBloqueChange = { selectedBloque = it },
                    onFechaChange = { fechaTxt = it },
                    onLatChange = { lat = it },
                    onLonChange = { lon = it },
                    onMascotaSelected = { mascotaIdTxt = TextFieldValue(it.id.toString()) },
                    onProcedimientoSelected = { sku = TextFieldValue(it.sku ?: "") },
                    onSubmit = { val mascotaId = runCatching { UUID.fromString(mascotaIdTxt.text) }.getOrNull() ?: return@FormCrearSolicitud
                        val fechaParsed = runCatching { LocalDate.parse(fechaTxt.text) }.getOrElse { LocalDate.now().plusDays(1) }
                        vm.submitSolicitud(
                            CrearSolicitudRequest(
                                mascotaId = mascotaId,
                                procedimientoSku = sku.text,
                                modoAtencion = selectedModo,
                                preferenciaLogistica = selectedPref,
                                fecha = fechaParsed,
                                bloqueSolicitado = selectedBloque,
                                latitud = lat.text.toDoubleOrNull() ?: 0.0,
                                longitud = lon.text.toDoubleOrNull() ?: 0.0
                            ),
                            onSuccess = onSolicitudEnviada
                        )
                    },
                    onAceptarOferta = {
                        val id = runCatching { UUID.fromString(ofertaIdTxt.text) }.getOrNull() ?: return@FormCrearSolicitud
                        vm.aceptarOferta(id)
                    },
                    onLimpiar = {
                        mascotaIdTxt = TextFieldValue("")
                        sku = TextFieldValue("")
                        fechaTxt = TextFieldValue(LocalDate.now().plusDays(3).toString())
                        lat = TextFieldValue("-33.45")
                        lon = TextFieldValue("-70.66")
                        ofertaIdTxt = TextFieldValue("")
                    }
                )
                !isVet && clientTabIndex == 0 -> FormCrearSolicitud(
                    ui = ui,
                    mascotaIdTxt = mascotaIdTxt,
                    sku = sku,
                    mascotaSearch = "",
                    procedimientoSearch = "",
                    onMascotaSearchChange = { },
                    onProcedimientoSearchChange = { },
                    selectedModo = selectedModo,
                    selectedPref = selectedPref,
                    selectedBloque = selectedBloque,
                    fechaTxt = fechaTxt,
                    lat = lat,
                    lon = lon,
                    showAceptarSection = false,
                    onToggleAceptarSection = { },
                    ofertaIdTxt = ofertaIdTxt,
                    onOfertaIdChange = { ofertaIdTxt = it },
                    onModoChange = { selectedModo = it },
                    onPrefChange = { selectedPref = it },
                    onBloqueChange = { selectedBloque = it },
                    onFechaChange = { fechaTxt = it },
                    onLatChange = { lat = it },
                    onLonChange = { lon = it },
                    onMascotaSelected = { mascotaIdTxt = TextFieldValue(it.id.toString()) },
                    onProcedimientoSelected = { sku = TextFieldValue(it.sku ?: "") },
                    onSubmit = { val mascotaId = runCatching { UUID.fromString(mascotaIdTxt.text) }.getOrNull() ?: return@FormCrearSolicitud
                        val fechaParsed = runCatching { LocalDate.parse(fechaTxt.text) }.getOrElse { LocalDate.now().plusDays(1) }
                        vm.submitSolicitud(
                            CrearSolicitudRequest(
                                mascotaId = mascotaId,
                                procedimientoSku = sku.text,
                                modoAtencion = selectedModo,
                                preferenciaLogistica = selectedPref,
                                fecha = fechaParsed,
                                bloqueSolicitado = selectedBloque,
                                latitud = lat.text.toDoubleOrNull() ?: 0.0,
                                longitud = lon.text.toDoubleOrNull() ?: 0.0
                            ),
                            onSuccess = onSolicitudEnviada
                        )
                    },
                    onAceptarOferta = {
                        val id = runCatching { UUID.fromString(ofertaIdTxt.text) }.getOrNull() ?: return@FormCrearSolicitud
                        vm.aceptarOferta(id)
                    },
                    onLimpiar = {
                        mascotaIdTxt = TextFieldValue("")
                        sku = TextFieldValue("")
                        fechaTxt = TextFieldValue(LocalDate.now().plusDays(3).toString())
                        lat = TextFieldValue("-33.45")
                        lon = TextFieldValue("-70.66")
                        ofertaIdTxt = TextFieldValue("")
                    }
                )
                !isVet && clientTabIndex == 1 -> MisSolicitudesTab(solicitudes = ui.misSolicitudes, isLoading = ui.isLoading, onRefresh = { vm.refrescarMisSolicitudes() })
            }

            if (ui.reservasAceptadas.isNotEmpty()) {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Reservas creadas", style = MaterialTheme.typography.titleMedium)
                        ui.reservasAceptadas.forEach { r -> Text("• ${r.id} · ${r.estado} · ${r.horaInicio} - ${r.horaFin}") }
                    }
                }
            }
        }
    }

    // Diálogo oferta (vet)
    if (ofertandoPara != null) {
        DialogOferta(
            isSubmitting = ui.isSubmitting,
            solicitud = ofertandoPara!!,
            onDismiss = { ofertandoPara = null },
            onSubmit = { req ->
                vm.enviarOferta(
                    solicitudId = ofertandoPara!!.id.toString(),
                    request = req,
                    onSuccess = { ofertandoPara = null }
                )
            }
        )
    }

    if (ui.isSubmitting) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(color = Color.Black.copy(alpha = 0.4f), modifier = Modifier.fillMaxSize()) {}
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Procesando...", color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

// ------------------ Subcomposables ------------------

@Composable
private fun VetSolicitudesTab(
    ui: MarketPlaceViewModel.UiState,
    onRefresh: () -> Unit,
    onOfertar: (SolicitudDisponibleDto) -> Unit
) {
    if (ui.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (ui.solicitudesDisponibles.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay solicitudes disponibles") }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refrescar") }
            }
        }
        items(ui.solicitudesDisponibles) { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Solicitud ${s.id}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        EstadoChip(text = s.modoAtencion.name)
                    }
                    Text(s.procedimiento?.nombre ?: s.procedimiento?.sku ?: "(procedimiento)")
                    Text("${s.fecha} · ${s.bloqueSolicitado}")
                    Text("(${s.latitud}, ${s.longitud})")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { /* descartar local */ }, enabled = !ui.isSubmitting) { Text("Descartar") }
                        Button(onClick = { onOfertar(s) }, enabled = !ui.isSubmitting) { Text("Ofertar") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumSegmented(options: List<T>, selected: T, onSelected: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { i, opt ->
            SegmentedButton(
                selected = opt == selected,
                onClick = { onSelected(opt) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                label = { Text(opt.name.replace('_', ' ')) }
            )
        }
    }
}

@Composable
private fun DialogOferta(
    isSubmitting: Boolean,
    solicitud: SolicitudDisponibleDto,
    onDismiss: () -> Unit,
    onSubmit: (CrearOfertaRequest) -> Unit
) {
    var precioServicio by remember { mutableStateOf(TextFieldValue("16000")) }
    var precioLogistica by remember { mutableStateOf(TextFieldValue("0")) }
    var horaCirugia by remember { mutableStateOf(TextFieldValue("10:00")) }
    var horaRetiro by remember { mutableStateOf(TextFieldValue("")) }
    var horaEntrega by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        CrearOfertaRequest(
                            precioServicio = precioServicio.text.toIntOrNull() ?: 0,
                            precioLogistica = precioLogistica.text.toIntOrNull() ?: 0,
                            horaRetiroPropuesta = horaRetiro.text.ifBlank { "" },
                            horaCirugiaPropuesta = horaCirugia.text.ifBlank { "" },
                            horaEntregaEstimada = horaEntrega.text.ifBlank { "" }
                        )
                    )
                },
                enabled = !isSubmitting
            ) { if (isSubmitting) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Enviar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancelar") }
        },
        title = { Text("Ofertar (${solicitud.modoAtencion})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = precioServicio,
                    onValueChange = { precioServicio = it },
                    label = { Text("Precio servicio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precioLogistica,
                    onValueChange = { precioLogistica = it },
                    label = { Text("Precio logística") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = horaCirugia,
                    onValueChange = { horaCirugia = it },
                    label = { Text("Hora propuesta (ej: 10:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = horaRetiro,
                    onValueChange = { horaRetiro = it },
                    label = { Text("Hora retiro (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = horaEntrega,
                    onValueChange = { horaEntrega = it },
                    label = { Text("Hora entrega (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun MisSolicitudesTab(solicitudes: List<SolicitudServicioResponse>, isLoading: Boolean, onRefresh: () -> Unit) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refrescar") }
            }
        }
        if (solicitudes.isEmpty()) {
            item { Text("No tienes solicitudes todavía") }
        } else {
            items(solicitudes) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Solicitud ${s.id}", style = MaterialTheme.typography.titleMedium)
                        Text("Estado: ${s.estado}")
                        Text("Fecha: ${s.fecha}")
                        Text("Procedimiento: ${s.procedimientoSku}")
                    }
                }
            }
        }
    }
}

@Composable
private fun MisOfertasTab(ofertas: List<OfertaVeterinarioResponse>, isLoading: Boolean, onRefresh: () -> Unit) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refrescar") }
            }
        }
        if (ofertas.isEmpty()) {
            item { Text("No has realizado ofertas todavía") }
        } else {
            items(ofertas) { o ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Oferta ${o.id}", style = MaterialTheme.typography.titleMedium)
                        Text("Estado: ${o.estado}")
                        Text("Precio total: ${o.precioTotal}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormCrearSolicitud(
    ui: MarketPlaceViewModel.UiState,
    mascotaIdTxt: TextFieldValue,
    sku: TextFieldValue,
    mascotaSearch: String,
    procedimientoSearch: String,
    onMascotaSearchChange: (String) -> Unit,
    onProcedimientoSearchChange: (String) -> Unit,
    selectedModo: CrearSolicitudRequest.ModoAtencion,
    selectedPref: CrearSolicitudRequest.PreferenciaLogistica,
    selectedBloque: CrearSolicitudRequest.BloqueSolicitado,
    fechaTxt: TextFieldValue,
    lat: TextFieldValue,
    lon: TextFieldValue,
    showAceptarSection: Boolean,
    onToggleAceptarSection: () -> Unit,
    ofertaIdTxt: TextFieldValue,
    onOfertaIdChange: (TextFieldValue) -> Unit,
    onModoChange: (CrearSolicitudRequest.ModoAtencion) -> Unit,
    onPrefChange: (CrearSolicitudRequest.PreferenciaLogistica) -> Unit,
    onBloqueChange: (CrearSolicitudRequest.BloqueSolicitado) -> Unit,
    onFechaChange: (TextFieldValue) -> Unit,
    onLatChange: (TextFieldValue) -> Unit,
    onLonChange: (TextFieldValue) -> Unit,
    onMascotaSelected: (Mascota) -> Unit,
    onProcedimientoSelected: (Procedimiento) -> Unit,
    onSubmit: () -> Unit,
    onAceptarOferta: () -> Unit,
    onLimpiar: () -> Unit
) {
    val fechaOk = runCatching { LocalDate.parse(fechaTxt.text) }.isSuccess
    val mascotaLista = ui.mascotas.filter { it.nombre?.contains(mascotaSearch, true) == true || mascotaSearch.isBlank() }
    val procedimientoLista = ui.procedimientos.filter { (it.nombre?.contains(procedimientoSearch, true) == true) || procedimientoSearch.isBlank() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Crear solicitud", style = MaterialTheme.typography.titleMedium) }
        item { Text("Mascota", style = MaterialTheme.typography.titleSmall) }
        item {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = mascotaSearch,
                    onValueChange = onMascotaSearchChange,
                    label = { Text("Buscar mascota") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SpacerSmall()
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp)) {
                    if (mascotaLista.isEmpty()) Text("Sin resultados") else mascotaLista.take(6).forEach { m ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onMascotaSelected(m) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(m.nombre ?: m.id.toString())
                            if (m.id.toString() == mascotaIdTxt.text) EstadoChip("Seleccionada")
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        item { Text("Procedimiento", style = MaterialTheme.typography.titleSmall) }
        item {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = procedimientoSearch,
                    onValueChange = onProcedimientoSearchChange,
                    label = { Text("Buscar procedimiento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SpacerSmall()
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp)) {
                    if (procedimientoLista.isEmpty()) Text("Sin resultados") else procedimientoLista.take(6).forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onProcedimientoSelected(p) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.nombre ?: p.sku ?: "(SKU)")
                            if ((p.sku ?: "") == sku.text) EstadoChip("Seleccionado")
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        item { EnumSegmented(options = listOf(CrearSolicitudRequest.ModoAtencion.DOMICILIO, CrearSolicitudRequest.ModoAtencion.CLINICA), selected = selectedModo, onSelected = onModoChange) }
        item { EnumSegmented(options = listOf(CrearSolicitudRequest.PreferenciaLogistica.YO_LLEVO, CrearSolicitudRequest.PreferenciaLogistica.RETIRO_Y_ENTREGA), selected = selectedPref, onSelected = onPrefChange) }
        item { EnumSegmented(options = listOf(CrearSolicitudRequest.BloqueSolicitado.MANANA, CrearSolicitudRequest.BloqueSolicitado.TARDE), selected = selectedBloque, onSelected = onBloqueChange) }
        item {
            OutlinedTextField(
                value = fechaTxt,
                onValueChange = onFechaChange,
                label = { Text("Fecha (YYYY-MM-DD)") },
                isError = !fechaOk,
                supportingText = { if (!fechaOk) Text("Formato inválido") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = lat, onValueChange = onLatChange, label = { Text("Latitud") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = lon, onValueChange = onLonChange, label = { Text("Longitud") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSubmit, enabled = !ui.isSubmitting && fechaOk && mascotaIdTxt.text.isNotBlank() && sku.text.isNotBlank()) {
                    if (ui.isSubmitting) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Publicar")
                }
                OutlinedButton(onClick = onLimpiar, enabled = !ui.isSubmitting) { Text("Limpiar") }
            }
        }
        item {
            Text(
                text = if (!showAceptarSection) "Aceptar oferta (opcional)" else "Aceptar oferta",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clickable { onToggleAceptarSection() }
            )
        }
        if (showAceptarSection) {
            item {
                OutlinedTextField(
                    value = ofertaIdTxt,
                    onValueChange = onOfertaIdChange,
                    label = { Text("Oferta ID (UUID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (ofertaIdTxt.text.isNotBlank() && runCatching { UUID.fromString(ofertaIdTxt.text) }.isFailure) Text("Formato inválido")
                    }
                )
            }
            item {
                val uuid = runCatching { UUID.fromString(ofertaIdTxt.text) }.getOrNull()
                Button(onClick = onAceptarOferta, enabled = uuid != null && !ui.isSubmitting) {
                    if (ui.isSubmitting) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Aceptar oferta")
                }
            }
        }
        item { SpacerSmall() }
    }
}

@Composable
private fun SpacerSmall() { Box(Modifier.size(6.dp)) }

@Composable
private fun EstadoChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
