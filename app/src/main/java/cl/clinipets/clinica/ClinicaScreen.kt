package cl.clinipets.clinica

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ClinicaScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory) {
    val vm: ClinicaViewModel = viewModel(factory = viewModelFactory)
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val diagnosticos by vm.diagnosticos.collectAsState()
    val tratamientos by vm.tratamientos.collectAsState()

    val sintomas = remember { mutableStateListOf<String>() }
    var sintomaInput by remember { mutableStateOf("") }

    var diagInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Herramientas clínicas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Sugerencias de diagnóstico", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = sintomaInput, onValueChange = { sintomaInput = it }, label = { Text("Añadir síntoma") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (sintomaInput.isNotBlank()) {
                sintomas.add(sintomaInput.trim())
                sintomaInput = ""
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Agregar síntoma") }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Síntomas: ${sintomas.joinToString(", ")}")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { vm.sugerenciasDiagnostico(sintomas.toList()) }, enabled = sintomas.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Obtener diagnósticos sugeridos") }

        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn { items(diagnosticos) { d ->
            Text(d, modifier = Modifier.padding(vertical = 4.dp))
        } }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Sugerencias de tratamiento", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = diagInput, onValueChange = { diagInput = it }, label = { Text("Diagnóstico") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { if (diagInput.isNotBlank()) vm.sugerenciasTratamiento(diagInput) }, modifier = Modifier.fillMaxWidth()) { Text("Obtener tratamientos") }

        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn { items(tratamientos) { t ->
            Text("• $t", modifier = Modifier.padding(vertical = 4.dp))
        } }

        if (!error.isNullOrEmpty()) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }
        if (isLoading) {
            Text("Cargando...")
        }
    }
}

