package cl.clinipets.ui.agenda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.MisReservasQuery
import cl.clinipets.openapi.models.Reserva
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaGestionScreen(
    onBack: () -> Unit,
    vm: AgendaGestionViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbarHostState = SnackbarHostState()
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
        topBar = { TopAppBar(title = { Text("Agendas") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Mis reservas", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { mostrarFiltros.value = !mostrarFiltros.value }) { Text(if (mostrarFiltros.value) "Ocultar filtros" else "Filtros") }
            }

            AnimatedVisibility(
                visible = mostrarFiltros.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FiltrosPanel(ui = ui, vm = vm)
            }

            if (ui.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.error?.let { err ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(err, Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            val agrupadas = ui.reservas.groupBy { it.fecha.toString() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                agrupadas.forEach { (fecha, list) ->
                    item {
                        Text(fecha, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    items(list) { r -> ReservaItem(r,
                        onAceptar = { vm.confirmar(r) },
                        onCancelar = { reservaAConfirmarCancelacion.value = r }) }
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
                TextButton(onClick = {
                    vm.cancelar(r)
                    reservaAConfirmarCancelacion.value = null
                }) { Text("Sí, cancelar") }
            },
            dismissButton = { TextButton(onClick = { reservaAConfirmarCancelacion.value = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun FiltrosPanel(ui: AgendaGestionViewModel.UiState, vm: AgendaGestionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Filtros", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FiltroCheck("Cliente", ui.como.contains(MisReservasQuery.Como.CLIENTE)) { vm.toggleComo(MisReservasQuery.Como.CLIENTE) }
            FiltroCheck("Veterinario", ui.como.contains(MisReservasQuery.Como.VETERINARIO)) { vm.toggleComo(MisReservasQuery.Como.VETERINARIO) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FiltroCheck("Pendiente", ui.estados.contains(MisReservasQuery.Estados.PENDIENTE)) { vm.toggleEstado(MisReservasQuery.Estados.PENDIENTE) }
            FiltroCheck("Confirmada", ui.estados.contains(MisReservasQuery.Estados.CONFIRMADA)) { vm.toggleEstado(MisReservasQuery.Estados.CONFIRMADA) }
        }
        Text("Canceladas", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FiltroCheck("Por cliente", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_CLIENTE)) { vm.toggleEstado(MisReservasQuery.Estados.CANCELADA_CLIENTE) }
            FiltroCheck("Por vet", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_VETERINARIO)) { vm.toggleEstado(MisReservasQuery.Estados.CANCELADA_VETERINARIO) }
            FiltroCheck("Por clínica", ui.estados.contains(MisReservasQuery.Estados.CANCELADA_CLINICA)) { vm.toggleEstado(MisReservasQuery.Estados.CANCELADA_CLINICA) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FiltroCheck("Realizada", ui.estados.contains(MisReservasQuery.Estados.REALIZADA)) { vm.toggleEstado(MisReservasQuery.Estados.REALIZADA) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FiltroCheck("Domicilio", ui.modos.contains(MisReservasQuery.Modos.DOMICILIO)) { vm.toggleModo(MisReservasQuery.Modos.DOMICILIO) }
            FiltroCheck("Clínica", ui.modos.contains(MisReservasQuery.Modos.CLINICA)) { vm.toggleModo(MisReservasQuery.Modos.CLINICA) }
            FiltroCheck("Urgencia", ui.modos.contains(MisReservasQuery.Modos.URGENCIA)) { vm.toggleModo(MisReservasQuery.Modos.URGENCIA) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ui.fecha?.toString() ?: "",
                onValueChange = { s -> vm.setFecha(s.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }) },
                modifier = Modifier.weight(1f),
                label = { Text("Fecha (YYYY-MM-DD)") }
            )
            Button(onClick = vm::cargar) { Text("Aplicar") }
        }
    }
}

@Composable
private fun FiltroCheck(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label)
    }
}

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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val puedeAceptar = r.estado == Reserva.Estado.PENDIENTE
                val puedeCancelar = r.estado != Reserva.Estado.CANCELADA_CLIENTE && r.estado != Reserva.Estado.CANCELADA_VETERINARIO && r.estado != Reserva.Estado.CANCELADA_CLINICA && r.estado != Reserva.Estado.REALIZADA
                Button(onClick = onAceptar, enabled = puedeAceptar) { Text("Aceptar") }
                OutlinedButton(onClick = onCancelar, enabled = puedeCancelar) { Text("Cancelar") }
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
