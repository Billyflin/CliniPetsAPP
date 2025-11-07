package cl.clinipets.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ReservarSlotClinicaRequest
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservasScreen(
    onBack: () -> Unit,
    vm: ReservasViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var ofertaIdInput by remember { mutableStateOf("") }
    var slotVetId by remember { mutableStateOf("") }
    var slotMascotaId by remember { mutableStateOf("") }
    var slotSku by remember { mutableStateOf("") }
    var slotFecha by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var slotHora by remember { mutableStateOf("09:00") }
    var reservaIdInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                if (ui.error != null) {
                    AssistChip(onClick = { vm.clearStatus() }, label = { Text(ui.error!!) })
                }
                if (ui.successMessage != null) {
                    AssistChip(onClick = { vm.clearStatus() }, label = { Text(ui.successMessage!!) })
                }
                if (localError != null) {
                    AssistChip(onClick = { localError = null }, label = { Text(localError!!) })
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Aceptar oferta", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = ofertaIdInput,
                            onValueChange = { ofertaIdInput = it },
                            label = { Text("Oferta ID (UUID)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val uuid = ofertaIdInput.toUuidOrNull()
                                if (uuid == null) {
                                    localError = "ID de oferta inválido"
                                } else {
                                    localError = null
                                    vm.aceptarOferta(uuid) {
                                        ofertaIdInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !ui.isWorking
                        ) {
                            Text("Aceptar oferta")
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Reservar clínica", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = slotVetId,
                            onValueChange = { slotVetId = it },
                            label = { Text("Veterinario ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = slotMascotaId,
                            onValueChange = { slotMascotaId = it },
                            label = { Text("Mascota ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = slotSku,
                            onValueChange = { slotSku = it },
                            label = { Text("Procedimiento SKU") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        RowFields(
                            fecha = slotFecha,
                            hora = slotHora,
                            onFechaChange = { slotFecha = it },
                            onHoraChange = { slotHora = it }
                        )
                        Button(
                            onClick = {
                                val vet = slotVetId.toUuidOrNull()
                                val pet = slotMascotaId.toUuidOrNull()
                                val fecha = slotFecha.toLocalDateOrNull()
                                if (vet == null || pet == null) {
                                    localError = "IDs inválidos"
                                    return@Button
                                }
                                if (slotSku.isBlank()) {
                                    localError = "SKU requerido"
                                    return@Button
                                }
                                if (fecha == null) {
                                    localError = "Fecha inválida"
                                    return@Button
                                }
                                val request = ReservarSlotClinicaRequest(
                                    veterinarioId = vet,
                                    mascotaId = pet,
                                    procedimientoSku = slotSku,
                                    fecha = fecha,
                                    horaInicio = slotHora
                                )
                                localError = null
                                vm.reservarSlotClinica(request) {
                                    slotHora = "09:00"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !ui.isWorking
                        ) {
                            Text("Reservar")
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Confirmar reserva", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = reservaIdInput,
                            onValueChange = { reservaIdInput = it },
                            label = { Text("Reserva ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val uuid = reservaIdInput.toUuidOrNull()
                                if (uuid == null) {
                                    localError = "ID de reserva inválido"
                                } else {
                                    localError = null
                                    vm.confirmarReserva(uuid) {
                                        reservaIdInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !ui.isWorking
                        ) {
                            Text("Confirmar")
                        }
                    }
                }
            }

            ui.ultimaReserva?.let { reserva ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Última reserva", fontWeight = FontWeight.Bold)
                            Text("ID: ${reserva.id}")
                            Text("Estado: ${reserva.estado}")
                            Text("Fecha: ${reserva.fecha} ${reserva.horaInicio}-${reserva.horaFin}")
                            Text("Cliente: ${reserva.cliente.nombreCompleto}")
                            Text("Veterinario: ${reserva.veterinario.nombreCompleto}")
                            Text("Mascota: ${reserva.mascota.nombre}")
                            Text("Procedimiento: ${reserva.procedimiento.nombre}")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RowFields(
    fecha: String,
    hora: String,
    onFechaChange: (String) -> Unit,
    onHoraChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = fecha,
            onValueChange = onFechaChange,
            label = { Text("Fecha (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = hora,
            onValueChange = onHoraChange,
            label = { Text("Hora inicio (HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun String.toUuidOrNull(): UUID? =
    runCatching { UUID.fromString(this) }.getOrNull()

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()
