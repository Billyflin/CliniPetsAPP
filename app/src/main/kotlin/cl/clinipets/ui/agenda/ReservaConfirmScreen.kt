package cl.clinipets.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.DiscoveryRequest
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaConfirmScreen(
    procedimientoSku: String,
    modo: DiscoveryRequest.ModoAtencion,
    fecha: String,
    horaInicio: String,
    lat: Double?,
    lng: Double?,
    veterinarioId: UUID?,
    direccion: String?,
    referencias: String?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: ReservaViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    // Inicializar VM con datos mínimos para confirmar (no recargar slots)
    LaunchedEffect(Unit) {
        vm.init(
            mascotaId = UUID.randomUUID(), // no se usa en VM
            procedimientoSku = procedimientoSku,
            modo = modo,
            lat = lat,
            lng = lng,
            veterinarioId = veterinarioId
        )
        vm.setFecha(fecha)
        vm.setHoraInicio(horaInicio)
        direccion?.let(vm::setDireccion)
        referencias?.let(vm::setReferencias)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Confirmación de reserva") })
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Revisa la información antes de enviar:", style = MaterialTheme.typography.titleSmall)
            Surface(tonalElevation = 2.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Procedimiento: $procedimientoSku")
                    Text("Modo: ${modo.name}")
                    Text("Fecha: $fecha")
                    Text("Hora: $horaInicio")
                    lat?.let { Text("Lat: $it") }
                    lng?.let { Text("Lng: $it") }
                    veterinarioId?.let { Text("Veterinario: $it") }
                    direccion?.let { Text("Dirección: $it") }
                    referencias?.let { Text("Referencias: $it") }
                }
            }

            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { vm.crearReserva(onDone) },
                enabled = !ui.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(if (ui.isSubmitting) "Enviando..." else "Confirmar reserva")
            }

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}

