package cl.clinipets.agenda

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
fun AgendaVetScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory) {
    val vm: AgendaVetViewModel = viewModel(factory = viewModelFactory)
    val reservas by vm.reservas.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val vetId by vm.vetId.collectAsState()

    var nuevoEstado by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.cargarVetYAgenda() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Agenda del Veterinario", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("VetId: ${vetId ?: "-"}")

        Spacer(modifier = Modifier.height(12.dp))
        if (isLoading) Text("Cargando...")
        if (!error.isNullOrEmpty()) Text(error ?: "", color = MaterialTheme.colorScheme.error)

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(reservas) { r: Reserva ->
                ReservaItem(r = r, onCambiarEstado = { estado, mot -> vm.cambiarEstado(r.id, estado, mot) })
            }
        }
    }
}

@Composable
private fun ReservaItem(r: Reserva, onCambiarEstado: (String, String?) -> Unit) {
    var estado by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("${r.inicio} - ${r.modo}")
        Text("Reserva ${r.id} | Mascota ${r.mascota.id}")
        Text("Estado: ${r.estado}")

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = estado, onValueChange = { estado = it }, label = { Text("Nuevo estado") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = motivo, onValueChange = { motivo = it }, label = { Text("Motivo (opcional)") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { if (estado.isNotBlank()) onCambiarEstado(estado, motivo.ifBlank { null }) }) { Text("Aplicar estado") }
    }
}

