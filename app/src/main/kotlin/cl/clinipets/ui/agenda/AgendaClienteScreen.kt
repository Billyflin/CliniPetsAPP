package cl.clinipets.ui.agenda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.MisReservasQuery
import cl.clinipets.openapi.models.Reserva
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// Se mantienen las mismas definiciones de forma
private val UI_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private val fieldShape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
private val chipShape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
private val buttonShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
private val reservaCardShape = RoundedCornerShape(topStart = 8.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 8.dp)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AgendaClienteScreen( // Renombrado
    onBack: () -> Unit,
    onVerMapa: (UUID) -> Unit,
    userId: UUID?,
    vm: AgendaGestionViewModel = hiltViewModel() // Se usa el mismo VM
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val mostrarFiltros = remember { mutableStateOf(false) }
    val reservaAConfirmarCancelacion = remember { mutableStateOf<Reserva?>(null) }

    LaunchedEffect(ui.actionMessage) {
        ui.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.consumirMensaje()
        }
    }

    // [CAMBIO CLAVE]
    // Al entrar, forzamos el ViewModel al modo "CLIENTE" antes de cargar.
    LaunchedEffect(Unit) {
        val currentState = vm.ui.value.como
        // 1. Si "VETERINARIO" está activado, lo desactivamos
        if (currentState.contains(MisReservasQuery.Como.VETERINARIO)) {
            vm.toggleComo(MisReservasQuery.Como.VETERINARIO)
        }
        // 2. Si "CLIENTE" no está activado, lo activamos
        if (!currentState.contains(MisReservasQuery.Como.CLIENTE)) {
            vm.toggleComo(MisReservasQuery.Como.CLIENTE)
        }
        // 3. Cargamos los datos
        vm.cargar()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        topBar = {
            TopAppBar(
                title = { Text("Mis Reservas (Cliente)") }, // Título actualizado
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mis reservas como cliente", style = MaterialTheme.typography.titleMedium) // Título actualizado
                        TextButton(onClick = { mostrarFiltros.value = !mostrarFiltros.value }) {
                            Text(if (mostrarFiltros.value) "Ocultar filtros" else "Mostrar filtros")
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = mostrarFiltros.value,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        FiltrosPanelCliente( // Panel de filtros modificado
                            ui = ui,
                            onToggleEstado = vm::toggleEstado,
                            onToggleModo = vm::toggleModo,
                            onSetFecha = vm::setFecha,
                            onAplicar = vm::cargar
                        )
                    }
                }

                if (ui.isLoading) {
                    item {
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    }
                }

                ui.error?.let { err ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(err, Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                val agrupadas = ui.reservas.groupBy { it.fecha.format(UI_DATE_FORMATTER) }

                if (agrupadas.isEmpty() && !ui.isLoading) {
                    item {
                        Text(
                            "No se encontraron reservas con los filtros seleccionados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                agrupadas.entries.sortedBy { it.key }.forEach { (fecha, list) ->
                    stickyHeader {
                        FechaHeader(fecha)
                    }
                    items(list, key = { it.id!! }) { r ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ReservaItem(
                                r = r,
                                onAceptar = { vm.confirmar(r) },
                                onCancelar = { reservaAConfirmarCancelacion.value = r },
                                onVerMapa = { onVerMapa(r.id!!) },
                                userId = userId
                            )
                        }
                    }
                }
            }
        }
    }

    reservaAConfirmarCancelacion.value?.let { r ->
        AlertDialog(
            onDismissRequest = { reservaAConfirmarCancelacion.value = null },
            title = { Text("Cancelar reserva") },
            text = { Text("¿Seguro que deseas cancelar la reserva ${r.procedimientoSku} del ${r.fecha}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.cancelar(r)
                        reservaAConfirmarCancelacion.value = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Sí, cancelar") }
            },
            dismissButton = { TextButton(onClick = { reservaAConfirmarCancelacion.value = null }) { Text("Cerrar") } }
        )
    }
}

// [CAMBIO CLAVE]
// Se crea un panel de filtros específico que NO incluye el filtro de Rol.
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosPanelCliente(
    ui: AgendaGestionViewModel.UiState,
    onToggleEstado: (MisReservasQuery.Estados) -> Unit,
    onToggleModo: (MisReservasQuery.Modos) -> Unit,
    onSetFecha: (LocalDate?) -> Unit,
    onAplicar: () -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val selectedDateMillis = remember(ui.fecha) {
        ui.fecha?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis ?: Instant.now().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            onSetFecha(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onSetFecha(null)
                    showDatePicker = false
                }) { Text("Limpiar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Sección "Filtrar por Rol" ELIMINADA

            Text("Estado", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroChipItem("Pendiente", ui.estados.contains(MisReservasQuery.Estados.PENDIENTE), chipShape) { onToggleEstado(MisReservasQuery.Estados.PENDIENTE) }
                FiltroChipItem("Confirmada", ui.estados.contains(MisReservasQuery.Estados.CONFIRMADA), chipShape) { onToggleEstado(MisReservasQuery.Estados.CONFIRMADA) }
                FiltroChipItem("Realizada", ui.estados.contains(MisReservasQuery.Estados.REALIZADA), chipShape) { onToggleEstado(MisReservasQuery.Estados.REALIZADA) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroChipItem("Cancelada (Cliente)", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_CLIENTE), chipShape) { onToggleEstado(MisReservasQuery.Estados.CANCELADA_CLIENTE) }
                FiltroChipItem("Cancelada (Vet)", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_VETERINARIO), chipShape) { onToggleEstado(MisReservasQuery.Estados.CANCELADA_VETERINARIO) }
                FiltroChipItem("Cancelada (Clínica)", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_CLINICA), chipShape) { onToggleEstado(MisReservasQuery.Estados.CANCELADA_CLINICA) }
            }

            Text("Modo", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroChipItem("Domicilio", ui.modos.contains(MisReservasQuery.Modos.DOMICILIO), chipShape) { onToggleModo(MisReservasQuery.Modos.DOMICILIO) }
                FiltroChipItem("Clínica", ui.modos.contains(MisReservasQuery.Modos.CLINICA), chipShape) { onToggleModo(MisReservasQuery.Modos.CLINICA) }
                FiltroChipItem("Urgencia", ui.modos.contains(MisReservasQuery.Modos.URGENCIA), chipShape) { onToggleModo(MisReservasQuery.Modos.URGENCIA) }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .weight(1f)
                    .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = ui.fecha?.format(UI_DATE_FORMATTER) ?: "",
                        onValueChange = {},
                        label = { Text("Fecha") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, "Seleccionar fecha") },
                        shape = fieldShape
                    )
                }
                Button(onClick = onAplicar, shape = buttonShape) { Text("Aplicar") }
            }
        }
    }
}


// --- Los composables de abajo (FechaHeader, FiltroChipItem, ReservaItem, Badges) son idénticos a AgendaGestionScreen ---

@Composable
private fun FechaHeader(fecha: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = fecha,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltroChipItem(label: String, selected: Boolean, shape: Shape, onToggle: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(label) },
        shape = shape,
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

@Composable
private fun ReservaItem(
    r: Reserva,
    onAceptar: () -> Unit,
    onCancelar: () -> Unit,
    userId: UUID?,
    onVerMapa: () -> Unit
) {
    Surface(
        shape = reservaCardShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(r.procedimientoSku, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                EstadoBadge(r.estado)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModoBadge(r.modoAtencion.name)
                r.veterinarioId?.let { SimpleBadge("Vet") }
                r.clinicaId?.let { SimpleBadge("Clínica") }
            }
            Text("${r.horaInicio} - ${r.horaFin}", style = MaterialTheme.typography.bodyMedium)
            r.direccionTexto?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val puedeAceptar = r.estado == Reserva.Estado.PENDIENTE && (r.clienteId != userId)
                val puedeCancelar = r.estado != Reserva.Estado.CANCELADA_CLIENTE && r.estado != Reserva.Estado.CANCELADA_VETERINARIO && r.estado != Reserva.Estado.CANCELADA_CLINICA && r.estado != Reserva.Estado.REALIZADA
                val puedeVerMapa = r.estado == Reserva.Estado.CONFIRMADA

                Button(onClick = onAceptar, enabled = puedeAceptar, shape = buttonShape) { Text("Aceptar") }

                TextButton(
                    onClick = onCancelar,
                    enabled = puedeCancelar,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Cancelar") }

                if (puedeVerMapa) {
                    TextButton(onClick = onVerMapa) { Text("Ver mapa") }
                }
            }
        }
    }
}

@Composable
private fun EstadoBadge(estado: Reserva.Estado) {
    val (bg, fg) = when (estado) {
        Reserva.Estado.PENDIENTE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        Reserva.Estado.CONFIRMADA -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Reserva.Estado.CANCELADA_CLIENTE, Reserva.Estado.CANCELADA_VETERINARIO, Reserva.Estado.CANCELADA_CLINICA -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Reserva.Estado.REALIZADA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(estado.value, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ModoBadge(modo: String) {
    val (bg, fg) = when (modo) {
        "DOMICILIO" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "CLINICA" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "URGENCIA" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) { Text(modo, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = fg, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun SimpleBadge(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}