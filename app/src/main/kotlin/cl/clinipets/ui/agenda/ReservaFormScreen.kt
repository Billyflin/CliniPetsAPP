package cl.clinipets.ui.agenda

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.DiscoveryRequest
import cl.clinipets.openapi.models.Intervalo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val fieldShape = RoundedCornerShape(16.dp)
private val buttonShape = RoundedCornerShape(24.dp)
private val slotCardShape = RoundedCornerShape(20.dp)


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

    val openDatePicker = remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching {
            LocalDate.parse(ui.fecha).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Revisa los datos antes de confirmar tu reserva.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ui.error?.let {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    Text("Fecha y horario", style = MaterialTheme.typography.titleSmall)
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(fieldShape)
                                .clickable { openDatePicker.value = true }
                        ) {
                            OutlinedTextField(
                                value = ui.fecha,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                label = { Text("Fecha") },
                                shape = fieldShape,
                                enabled = false
                            )
                        }
                        Button(
                            onClick = { openDatePicker.value = true },
                            shape = buttonShape
                        ) { Text("Cambiar") }
                    }
                }

                if (openDatePicker.value) {
                    item {
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
                }

                if (ui.isLoadingSlots) {
                    item {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }

                if (ui.slots.isNotEmpty()) {
                    item {
                        Text("Horarios disponibles", style = MaterialTheme.typography.titleSmall)
                    }
                    items(ui.slots) { slot: Intervalo ->
                        val selected = slot.inicio == ui.horaInicio
                        Card(
                            onClick = { vm.seleccionarSlot(slot) },
                            shape = slotCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "${slot.inicio} - ${slot.fin}",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else null
                                )
                                if (selected) {
                                    Text(
                                        "(seleccionado)",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else if (!ui.isLoadingSlots) {
                    item {
                        Text("No hay horarios disponibles para la fecha seleccionada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    Text("Ubicación", style = MaterialTheme.typography.titleSmall)
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setUsarMiUbicacion(!ui.usarMiUbicacion) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text("Usar mi ubicación", modifier = Modifier.weight(1f))
                        Switch(checked = ui.usarMiUbicacion, onCheckedChange = { vm.setUsarMiUbicacion(it) })
                    }
                }

                if (ui.usarMiUbicacion) {
                    item {
                        Text(
                            text = "Usaremos tu ubicación actual para la reserva.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = ui.direccion,
                            onValueChange = vm::setDireccion,
                            label = { Text("Dirección (opcional)") },
                            placeholder = { Text("Calle, número, comuna") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = ui.referencias,
                            onValueChange = vm::setReferencias,
                            label = { Text("Referencias (opcional)") },
                            placeholder = { Text("Piso, portón, timbre, etc.") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fieldShape
                        )
                    }
                }

                item {
                    // [CAMBIO] La validación ahora comprueba si horaInicio NO es nulo.
                    val isTimeSlotSelected = ui.horaInicio != null
                    val isFormValid = !ui.isSubmitting && isTimeSlotSelected

                    Button(
                        onClick = {
                            onContinuarConfirmacion(
                                ui.fecha,
                                ui.horaInicio!!, // <-- CAMBIO: Se usa !! (es seguro por la validación)
                                ui.direccion.ifBlank { null },
                                ui.referencias.ifBlank { null }
                            )
                        },
                        enabled = isFormValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = buttonShape
                    ) { Text("Revisar y confirmar") }
                }
            }
        }
    }
}