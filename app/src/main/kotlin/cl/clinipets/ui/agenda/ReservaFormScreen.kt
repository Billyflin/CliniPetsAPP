package cl.clinipets.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import cl.clinipets.openapi.models.DiscoveryRequest
import cl.clinipets.openapi.models.Intervalo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaFormScreen(
    mascotaId: UUID,
    procedimientoSku: String,
    modo: DiscoveryRequest.ModoAtencion,
    lat: Double?,
    lng: Double?,
    veterinarioId: UUID? = null,
    precioSugerido: Int? = null,
    onBack: () -> Unit,
    onReservada: () -> Unit,
    onContinuarConfirmacion: (fecha: String, horaInicio: String, direccion: String?, referencias: String?) -> Unit,
    vm: ReservaViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.init(mascotaId, procedimientoSku, modo, lat, lng, veterinarioId) }

    // DatePicker dialog state
    val openDatePicker = remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching {
            LocalDate.parse(ui.fecha).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Confirmar reserva")
                        Text(
                            text = procedimientoSku,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mensaje informativo
            Text(
                text = "Revisa los datos antes de confirmar tu reserva.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Error en un contenedor más visible
            ui.error?.let {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // --- Sección: Fecha y horario ---
            Text("Fecha y horario", style = MaterialTheme.typography.titleSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ui.fecha,
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    label = { Text("Fecha") }
                )
                Button(onClick = { openDatePicker.value = true }) { Text("Cambiar") }
            }

            if (openDatePicker.value) {
                DatePickerDialog(
                    onDismissRequest = { openDatePicker.value = false },
                    confirmButton = {
                        Button(onClick = {
                            val millis: Long? = pickerState.selectedDateMillis
                            if (millis != null) {
                                val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                                vm.setFecha(ld.toString())
                            }
                            openDatePicker.value = false
                        }) { Text("Aceptar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { openDatePicker.value = false }) { Text("Cancelar") }
                    }
                ) {
                    DatePicker(state = pickerState)
                }
            }

            if (ui.isLoadingSlots) {
                androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            if (ui.slots.isNotEmpty()) {
                Text("Horarios disponibles", style = MaterialTheme.typography.titleSmall)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ui.slots) { slot: Intervalo ->
                        val selected = slot.inicio == ui.horaInicio
                        androidx.compose.material3.Card(onClick = { vm.seleccionarSlot(slot) }) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${slot.inicio} - ${slot.fin}")
                                if (selected) Text("(seleccionado)", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else if (!ui.isLoadingSlots) {
                Text("No hay horarios disponibles para la fecha seleccionada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // --- Sección: Ubicación ---
            Text("Ubicación", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Usar mi ubicación")
                Switch(checked = ui.usarMiUbicacion, onCheckedChange = { vm.setUsarMiUbicacion(it) })
            }
            if (ui.usarMiUbicacion) {
                Text(
                    text = "Usaremos tu ubicación actual para la reserva.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextField(
                    value = ui.direccion,
                    onValueChange = vm::setDireccion,
                    label = { Text("Dirección (opcional)") },
                    placeholder = { Text("Calle, número, comuna") }
                )
                OutlinedTextField(
                    value = ui.referencias,
                    onValueChange = vm::setReferencias,
                    label = { Text("Referencias (opcional)") },
                    placeholder = { Text("Piso, portón, timbre, etc.") }
                )
            }

            // --- Acciones finales ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        onContinuarConfirmacion(
                            ui.fecha,
                            ui.horaInicio,
                            ui.direccion.ifBlank { null },
                            ui.referencias.ifBlank { null }
                        )
                    },
                    enabled = !ui.isSubmitting,
                    modifier = Modifier.weight(1f)
                ) { Text("Revisar y confirmar") }

                Button(
                    onClick = { vm.crearReserva(onReservada) },
                    enabled = !ui.isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (ui.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(if (ui.isSubmitting) "Creando..." else "Confirmar ahora")
                }
            }
        }
    }
}
