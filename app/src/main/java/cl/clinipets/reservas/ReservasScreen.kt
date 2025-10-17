package cl.clinipets.reservas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.clinipets.network.Reserva

@Composable
fun ReservasScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory) {
    val vm: ReservasViewModel = viewModel(factory = viewModelFactory)
    val reservas by vm.reservas.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var mascotaId by remember { mutableStateOf("") }
    var veterinarioId by remember { mutableStateOf("") }
    var procedimientoSku by remember { mutableStateOf("") }
    var inicio by remember { mutableStateOf("") }
    var modo by remember { mutableStateOf("CLINICA") }
    var direccion by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    // cancel dialog state
    var cancelId by remember { mutableStateOf<String?>(null) }
    var cancelMotivo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadMisReservas() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mis reservas")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("Crear reserva (prueba)") }
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) Text("Cargando...")
        if (!error.isNullOrEmpty()) Text(error ?: "")

        LazyColumn { items(reservas) { r: Reserva ->
            ReservaRow(
                r,
                onCancel = { cancelId = r.id }
            )
        } }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Crear reserva (manual)") },
            text = {
                Column {
                    OutlinedTextField(value = mascotaId, onValueChange = { mascotaId = it }, label = { Text("mascotaId") })
                    OutlinedTextField(value = veterinarioId, onValueChange = { veterinarioId = it }, label = { Text("veterinarioId") })
                    OutlinedTextField(value = procedimientoSku, onValueChange = { procedimientoSku = it }, label = { Text("procedimientoSku") })
                    OutlinedTextField(value = inicio, onValueChange = { inicio = it }, label = { Text("inicio ISO8601") })
                    OutlinedTextField(value = modo, onValueChange = { modo = it }, label = { Text("modo (DOMICILIO/CLINICA)") })
                    OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text("direccionAtencion (si DOMICILIO)") })
                    OutlinedTextField(value = notas, onValueChange = { notas = it }, label = { Text("notas") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.crearReserva(mascotaId, veterinarioId, procedimientoSku, inicio, modo, if (modo=="DOMICILIO") direccion else null, notas) { success, _ ->
                        if (success) showCreate = false
                    }
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancelar") } }
        )
    }

    if (cancelId != null) {
        AlertDialog(
            onDismissRequest = { cancelId = null },
            title = { Text("Cancelar reserva") },
            text = {
                Column {
                    Text("Indica un motivo (opcional)")
                    OutlinedTextField(value = cancelMotivo, onValueChange = { cancelMotivo = it }, label = { Text("Motivo") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = cancelId!!
                    vm.cancelarReserva(id, cancelMotivo.ifBlank { null }) { ok ->
                        if (ok) cancelId = null
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { cancelId = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun ReservaRow(r: Reserva, onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Reserva ${r.id ?: "-"}")
        Row(modifier = Modifier.fillMaxWidth()) {
            EstadoChip(r.estado)
        }
        Text("Mascota: ${r.mascota.nombre} | Vet: ${r.veterinario.nombreCompleto ?: "-"}")
        Text("Inicio: ${r.inicio} | Modo: ${r.modo}")
        Spacer(modifier = Modifier.height(6.dp))
        if (r.estado == "PENDIENTE" || r.estado == "CONFIRMADA") {
            TextButton(onClick = onCancel) { Text("Cancelar reserva") }
        }
    }
}

@Composable
private fun EstadoChip(estado: String) {
    val label = when (estado) {
        "PENDIENTE" -> "Pendiente"
        "CONFIRMADA" -> "Confirmada"
        "EN_CURSO" -> "En curso"
        "COMPLETADA" -> "Completada"
        "CANCELADA_CLIENTE" -> "Cancelada (cliente)"
        "CANCELADA_VETERINARIO" -> "Cancelada (veterinario)"
        else -> estado
    }
    AssistChip(onClick = {}, label = { Text(label) }, colors = AssistChipDefaults.assistChipColors())
}
