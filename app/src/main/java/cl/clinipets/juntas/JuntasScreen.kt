package cl.clinipets.juntas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun JuntasScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory) {
    val vm: JuntasViewModel = viewModel(factory = viewModelFactory)
    val junta by vm.junta.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var reservaId by remember { mutableStateOf("") }
    var nuevaLat by remember { mutableStateOf(0.0) }
    var nuevaLng by remember { mutableStateOf(0.0) }
    var nuevoEstado by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Juntas", modifier = Modifier.padding(bottom = 8.dp))

        OutlinedTextField(value = reservaId, onValueChange = { reservaId = it }, label = { Text("reservaId para crear junta") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { vm.crearJunta(reservaId) }, modifier = Modifier.fillMaxWidth()) { Text("Iniciar Junta") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Junta actual:")
        if (junta != null) {
            Text("id: ${junta!!.id}")
            Text("reservaId: ${junta!!.reservaId}")
            Text("estado: ${junta!!.estado}")

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = nuevoEstado, onValueChange = { nuevoEstado = it }, label = { Text("nuevo estado (EN_CAMINO, EN_SITIO, FINALIZADA)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { if (nuevoEstado.isNotBlank()) vm.cambiarEstado(junta!!.id, nuevoEstado) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Cambiar estado") }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = nuevaLat.toString(), onValueChange = { nuevaLat = it.toDoubleOrNull() ?: nuevaLat }, label = { Text("lat") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = nuevaLng.toString(), onValueChange = { nuevaLng = it.toDoubleOrNull() ?: nuevaLng }, label = { Text("lng") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.actualizarUbicacion(junta!!.id, nuevaLat, nuevaLng) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Actualizar ubicación") }

            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { vm.finalizarJunta(junta!!.id, null) }, modifier = Modifier.fillMaxWidth()) { Text("Finalizar Junta") }
        } else {
            Text("No hay junta activa")
        }

        if (isLoading) Text("Cargando...")
        if (!error.isNullOrEmpty()) Text(error ?: "")
    }
}

