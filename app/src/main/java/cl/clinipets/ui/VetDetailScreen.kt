package cl.clinipets.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.clinipets.data.dto.CrearReserva
import cl.clinipets.domain.agenda.AgendaRepository
import cl.clinipets.domain.catalogo.CatalogoRepository
import cl.clinipets.domain.mascotas.MascotasRepository
import kotlinx.coroutines.launch

@Composable
fun VetDetailScreen(
    vetId: String,
    catalogoRepository: CatalogoRepository,
    agendaRepository: AgendaRepository,
    mascotasRepository: MascotasRepository,
) {
    val scope = rememberCoroutineScope()
    val ofertas = remember { mutableStateOf("") }
    val slots = remember { mutableStateOf("") }
    val error = remember { mutableStateOf<String?>(null) }

    val fromIso = remember { mutableStateOf("") }
    val toIso = remember { mutableStateOf("") }
    val ofertaId = remember { mutableStateOf("") }
    val mascotaId = remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Text("Vet: $vetId")
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                error.value = null
                runCatching { catalogoRepository.ofertas(vetId = vetId) }
                    .onSuccess { ofertas.value = it }
                    .onFailure { e -> error.value = e.message }
            }
        }) { Text("Cargar ofertas") }
        if (ofertas.value.isNotBlank()) Text(ofertas.value)

        Spacer(Modifier.height(16.dp))
        Text("Consultar slots")
        OutlinedTextField(value = fromIso.value, onValueChange = { fromIso.value = it }, label = { Text("from ISO") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = toIso.value, onValueChange = { toIso.value = it }, label = { Text("to ISO") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = ofertaId.value, onValueChange = { ofertaId.value = it }, label = { Text("ofertaId (opcional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                error.value = null
                runCatching { agendaRepository.slots(vetId = vetId, fromIso = fromIso.value, toIso = toIso.value, ofertaId = ofertaId.value.ifBlank { null }) }
                    .onSuccess { slots.value = it }
                    .onFailure { e -> error.value = e.message }
            }
        }) { Text("Ver slots") }
        if (slots.value.isNotBlank()) Text(slots.value)

        Spacer(Modifier.height(16.dp))
        Text("Crear reserva")
        OutlinedTextField(value = mascotaId.value, onValueChange = { mascotaId.value = it }, label = { Text("mascotaId") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = ofertaId.value, onValueChange = { ofertaId.value = it }, label = { Text("ofertaId") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = fromIso.value, onValueChange = { fromIso.value = it }, label = { Text("inicio ISO") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                error.value = null
                runCatching {
                    agendaRepository.crearReserva(
                        CrearReserva(
                            mascotaId = mascotaId.value,
                            ofertaId = ofertaId.value,
                            inicio = fromIso.value
                        )
                    )
                }.onSuccess { slots.value = "Reserva creada" }
                    .onFailure { e -> error.value = e.message }
            }
        }) { Text("Reservar") }

        error.value?.let { Text("Error: $it") }
    }
}

