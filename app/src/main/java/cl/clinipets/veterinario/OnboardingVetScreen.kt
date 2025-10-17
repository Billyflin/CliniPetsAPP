package cl.clinipets.veterinario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
fun OnboardingVetScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory, onSuccess: () -> Unit) {
    val vm: VeterinarioViewModel = viewModel(factory = viewModelFactory)
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var licencia by remember { mutableStateOf("") }
    var modoDomicilio by remember { mutableStateOf(true) }
    var modoClinica by remember { mutableStateOf(false) }
    var modoUrgencia by remember { mutableStateOf(false) }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }
    var radioKm by remember { mutableStateOf(10) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ser veterinario", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = licencia, onValueChange = { licencia = it }, label = { Text("Número de licencia (opcional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        Text("Modos de atención")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth().toggleable(value = modoDomicilio, onValueChange = { modoDomicilio = it })) {
                    Text("Domicilio")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth().toggleable(value = modoClinica, onValueChange = { modoClinica = it })) {
                    Text("Clínica")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth().toggleable(value = modoUrgencia, onValueChange = { modoUrgencia = it })) {
                    Text("Urgencia")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = radioKm.toString(), onValueChange = { radioKm = it.toIntOrNull() ?: radioKm }, label = { Text("Radio de cobertura (km)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val modos = mutableListOf<String>()
            if (modoDomicilio) modos.add("DOMICILIO")
            if (modoClinica) modos.add("CLINICA")
            if (modoUrgencia) modos.add("URGENCIA")
            if (nombre.isBlank() || modos.isEmpty()) {
                // simple validation
                return@Button
            }
            vm.crearVeterinario(nombre, if (licencia.isBlank()) null else licencia, modos, null, null, radioKm) { success ->
                if (success) onSuccess()
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isLoading) "Creando..." else "Enviar")
        }

        if (!error.isNullOrEmpty()) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

