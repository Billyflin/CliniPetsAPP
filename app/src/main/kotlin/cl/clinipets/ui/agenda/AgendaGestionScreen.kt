package cl.clinipets.ui.agenda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi // NUEVO: Para stickyHeader
import androidx.compose.foundation.clickable // NUEVO
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // NUEVO
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi // NUEVO: Para FlowRow
import androidx.compose.foundation.layout.FlowRow // NUEVO
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer // NUEVO
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height // NUEVO
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size // NUEVO
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons // NUEVO
import androidx.compose.material.icons.automirrored.filled.ArrowBack // NUEVO
import androidx.compose.material.icons.filled.CalendarToday // NUEVO
import androidx.compose.material.icons.filled.Check // NUEVO
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults // NUEVO
import androidx.compose.material3.DatePicker // NUEVO
import androidx.compose.material3.DatePickerDialog // NUEVO
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip // NUEVO
import androidx.compose.material3.Icon // NUEVO
import androidx.compose.material3.IconButton // NUEVO
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults // NUEVO
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState // NUEVO
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable // NUEVO
import androidx.compose.runtime.setValue // NUEVO
import androidx.compose.ui.Alignment // NUEVO
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.MisReservasQuery
import cl.clinipets.openapi.models.Reserva
import java.time.Instant // NUEVO
import java.time.LocalDate
import java.time.ZoneId // NUEVO
import java.time.format.DateTimeFormatter // NUEVO

// Formateador para la fecha en la UI (ej: 15-11-2025)
private val UI_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class) // NUEVO
@Composable
fun AgendaGestionScreen(
    onBack: () -> Unit,
    vm: AgendaGestionViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() } // remember
    val mostrarFiltros = remember { mutableStateOf(false) }
    val reservaAConfirmarCancelacion = remember { mutableStateOf<Reserva?>(null) }

    LaunchedEffect(ui.actionMessage) {
        ui.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.consumirMensaje()
        }
    }

    LaunchedEffect(Unit) { vm.cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Agendas") }, // Título mejorado
                // NUEVO: Botón de navegación para volver
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(), // Quitado padding de 16dp para que la lista use todo el espacio
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fila de título y filtros (con padding)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp), // Padding solo arriba
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mis reservas", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { mostrarFiltros.value = !mostrarFiltros.value }) {
                    Text(if (mostrarFiltros.value) "Ocultar filtros" else "Mostrar filtros")
                }
            }

            // Panel de filtros animado (con padding)
            AnimatedVisibility(
                visible = mostrarFiltros.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                // El panel de filtros ahora es un Composable separado y mejorado
                FiltrosPanel(
                    ui = ui,
                    onToggleComo = vm::toggleComo,
                    onToggleEstado = vm::toggleEstado,
                    onToggleModo = vm::toggleModo,
                    onSetFecha = vm::setFecha,
                    onAplicar = vm::cargar
                )
            }

            if (ui.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

            ui.error?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp) // Padding para el error
                ) {
                    Text(err, Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            // CAMBIO: Lista agrupada con Sticky Headers
            val agrupadas = ui.reservas.groupBy { it.fecha.format(UI_DATE_FORMATTER) }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp) // Padding para la lista
            ) {
                // Ordenamos por fecha para asegurar el orden
                agrupadas.entries.sortedBy { it.key }.forEach { (fecha, list) ->
                    // NUEVO: Cabecera fija
                    stickyHeader {
                        FechaHeader(fecha)
                    }
                    items(list, key = { it.id!! }) { r ->
                        ReservaItem(r,
                            onAceptar = { vm.confirmar(r) },
                            onCancelar = { reservaAConfirmarCancelacion.value = r }
                        )
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
                // CAMBIO: Botón de confirmación con color de error
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

// NUEVO: Composable para la cabecera de fecha
@Composable
private fun FechaHeader(fecha: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f), // Color para la cabecera
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


// === CAMBIO: Panel de Filtros completamente rediseñado ===
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosPanel(
    ui: AgendaGestionViewModel.UiState,
    onToggleComo: (MisReservasQuery.Como) -> Unit,
    onToggleEstado: (MisReservasQuery.Estados) -> Unit,
    onToggleModo: (MisReservasQuery.Modos) -> Unit,
    onSetFecha: (LocalDate?) -> Unit,
    onAplicar: () -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // --- Lógica del DatePicker ---
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
                    onSetFecha(null) // Permite limpiar la fecha
                    showDatePicker = false
                }) { Text("Limpiar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    // --- Fin DatePicker ---

    Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text("Filtrar por Rol", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroChipItem("Cliente", ui.como.contains(MisReservasQuery.Como.CLIENTE)) { onToggleComo(MisReservasQuery.Como.CLIENTE) }
                FiltroChipItem("Veterinario", ui.como.contains(MisReservasQuery.Como.VETERINARIO)) { onToggleComo(MisReservasQuery.Como.VETERINARIO) }
            }

            Text("Estado", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroChipItem("Pendiente", ui.estados.contains(MisReservasQuery.Estados.PENDIENTE)) { onToggleEstado(MisReservasQuery.Estados.PENDIENTE) }
                FiltroChipItem("Confirmada", ui.estados.contains(MisReservasQuery.Estados.CONFIRMADA)) { onToggleEstado(MisReservasQuery.Estados.CONFIRMADA) }
                FiltroChipItem("Realizada", ui.estados.contains(MisReservasQuery.Estados.REALIZADA)) { onToggleEstado(MisReservasQuery.Estados.REALIZADA) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroChipItem("Cancelada (Cliente)", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_CLIENTE)) { onToggleEstado(MisReservasQuery.Estados.CANCELADA_CLIENTE) }
                FiltroChipItem("Cancelada (Vet)", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_VETERINARIO)) { onToggleEstado(MisReservasQuery.Estados.CANCELADA_VETERINARIO) }
                FiltroChipItem("Cancelada (Clínica)", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_CLINICA)) { onToggleEstado(MisReservasQuery.Estados.CANCELADA_CLINICA) }
            }

            Text("Modo", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroChipItem("Domicilio", ui.modos.contains(MisReservasQuery.Modos.DOMICILIO)) { onToggleModo(MisReservasQuery.Modos.DOMICILIO) }
                FiltroChipItem("Clínica", ui.modos.contains(MisReservasQuery.Modos.CLINICA)) { onToggleModo(MisReservasQuery.Modos.CLINICA) }
                FiltroChipItem("Urgencia", ui.modos.contains(MisReservasQuery.Modos.URGENCIA)) { onToggleModo(MisReservasQuery.Modos.URGENCIA) }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Campo de fecha con DatePicker
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
                        trailingIcon = { Icon(Icons.Default.CalendarToday, "Seleccionar fecha") }
                    )
                }
                // Botón aplicar
                Button(onClick = onAplicar) { Text("Aplicar") }
            }
        }
    }
}

// NUEVO: Composable reutilizable para los FilterChip
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltroChipItem(label: String, selected: Boolean, onToggle: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(label) },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    modifier = Modifier.size(18.dp) // Tamaño del ícono de check
                )
            }
        }
    )
}

// Composable 'FiltroCheck' eliminado, ya no se usa.

@Composable
private fun ReservaItem(r: Reserva, onAceptar: () -> Unit, onCancelar: () -> Unit) {
    Surface(tonalElevation = 2.dp, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(r.procedimientoSku, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val puedeAceptar = r.estado == Reserva.Estado.PENDIENTE
                val puedeCancelar = r.estado != Reserva.Estado.CANCELADA_CLIENTE && r.estado != Reserva.Estado.CANCELADA_VETERINARIO && r.estado != Reserva.Estado.CANCELADA_CLINICA && r.estado != Reserva.Estado.REALIZADA

                Button(onClick = onAceptar, enabled = puedeAceptar) { Text("Aceptar") }

                // CAMBIO: "Cancelar" es ahora un TextButton para menor énfasis y con color de error
                TextButton(
                    onClick = onCancelar,
                    enabled = puedeCancelar,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Cancelar") }
            }
        }
    }
}

// Los composables de Badges (EstadoBadge, ModoBadge, SimpleBadge) están perfectos, se dejan igual.
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