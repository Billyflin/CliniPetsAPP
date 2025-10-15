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
import cl.clinipets.data.dto.ReservaDto
import cl.clinipets.domain.agenda.AgendaRepository
import kotlinx.coroutines.launch

@Composable
fun ReservasScreen(repo: AgendaRepository) {
    val scope = rememberCoroutineScope()
    val loading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }
    val reservas = remember { mutableStateOf<List<ReservaDto>>(emptyList()) }
    val creada = remember { mutableStateOf<ReservaDto?>(null) }

    val mascotaId = remember { mutableStateOf("") }
    val ofertaId = remember { mutableStateOf("") }
    val inicioIso = remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Text("Mis reservas")
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                loading.value = true
                error.value = null
                runCatching { repo.reservasMias() }
                    .onSuccess { reservas.value = it }
                    .onFailure { e -> error.value = e.message }
                loading.value = false
            }
        }) { Text("Cargar") }

        Spacer(Modifier.height(12.dp))
        if (loading.value) Text("Cargando…")
        error.value?.let { Text("Error: $it") }
        reservas.value.forEach { r ->
            Text("• ${r.reservaId ?: "(sin id)"} - oferta=${r.ofertaId} inicio=${r.inicio} estado=${r.estado}")
        }

        Spacer(Modifier.height(24.dp))
        Text("Crear reserva")
        OutlinedTextField(value = mascotaId.value, onValueChange = { mascotaId.value = it }, label = { Text("mascotaId") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = ofertaId.value, onValueChange = { ofertaId.value = it }, label = { Text("ofertaId") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = inicioIso.value, onValueChange = { inicioIso.value = it }, label = { Text("inicio ISO") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                loading.value = true
                error.value = null
                runCatching {
                    repo.crearReserva(
                        CrearReserva(
                            mascotaId = mascotaId.value,
                            ofertaId = ofertaId.value,
                            inicio = inicioIso.value
                        )
                    )
                }.onSuccess { creada.value = it }
                    .onFailure { e -> error.value = e.message }
                loading.value = false
            }
        }) { Text("Crear") }
        creada.value?.let { r ->
            Text("Reserva creada: ${r.reservaId ?: "(sin id)"} estado=${r.estado}")
        }
    }
}
