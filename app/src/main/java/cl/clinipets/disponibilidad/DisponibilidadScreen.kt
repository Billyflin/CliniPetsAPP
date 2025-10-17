package cl.clinipets.disponibilidad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import cl.clinipets.network.Bloque

@Composable
fun DisponibilidadScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory) {
    val vm: DisponibilidadViewModel = viewModel(factory = viewModelFactory)
    val bloques by vm.bloques.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val vetId by vm.vetId.collectAsState()

    var fecha by remember { mutableStateOf("") } // YYYY-MM-DD
    var diaSemana by remember { mutableStateOf(1) }
    var horaInicio by remember { mutableStateOf("09:00") }
    var horaFin by remember { mutableStateOf("18:00") }

    var excFecha by remember { mutableStateOf("") }
    var excTipo by remember { mutableStateOf("CERRADO") }
    var excHoraInicio by remember { mutableStateOf("") }
    var excHoraFin by remember { mutableStateOf("") }
    var excMotivo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.cargarMiVetId() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Disponibilidad del Veterinario", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text("VetId: ${vetId ?: "-"}")

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if (fecha.isNotBlank()) vm.consultar(fecha) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Consultar bloques") }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Bloques", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(bloques) { b: Bloque -> BloqueRow(b) } }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Crear regla semanal", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = diaSemana.toString(), onValueChange = { diaSemana = it.toIntOrNull() ?: diaSemana }, label = { Text("Día semana (1=Lun..7=Dom)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = horaInicio, onValueChange = { horaInicio = it }, label = { Text("Hora inicio (HH:mm)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = horaFin, onValueChange = { horaFin = it }, label = { Text("Hora fin (HH:mm)") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { vm.crearRegla(diaSemana, horaInicio, horaFin) }) { Text("Crear regla") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Crear excepción", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = excFecha, onValueChange = { excFecha = it }, label = { Text("Fecha (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = excTipo, onValueChange = { excTipo = it }, label = { Text("Tipo (CERRADO/OTRO)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = excHoraInicio, onValueChange = { excHoraInicio = it }, label = { Text("Hora inicio (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = excHoraFin, onValueChange = { excHoraFin = it }, label = { Text("Hora fin (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = excMotivo, onValueChange = { excMotivo = it }, label = { Text("Motivo (opcional)") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { vm.crearExcepcion(excFecha, excTipo, excHoraInicio.ifBlank { null }, excHoraFin.ifBlank { null }, excMotivo.ifBlank { null }) }) { Text("Crear excepción") }

        if (!error.isNullOrEmpty()) Text(error ?: "", color = MaterialTheme.colorScheme.error)
        if (isLoading) Text("Cargando...")
    }
}

@Composable
private fun BloqueRow(b: Bloque) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("${b.inicio} - ${b.fin}")
    }
}

