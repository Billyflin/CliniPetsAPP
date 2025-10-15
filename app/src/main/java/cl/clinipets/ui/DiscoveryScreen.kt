package cl.clinipets.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import cl.clinipets.domain.discovery.DiscoveryRepository
import kotlinx.coroutines.launch

@Composable
fun DiscoveryScreen(repo: DiscoveryRepository, onOpenVet: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val lat = remember { mutableStateOf(-33.45) }
    val lon = remember { mutableStateOf(-70.66) }
    val radio = remember { mutableStateOf(3000) }
    val result = remember { mutableStateOf("") }
    val vetId = remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = lat.value.toString(), onValueChange = { lat.value = it.toDoubleOrNull() ?: lat.value }, label = { Text("lat") })
        OutlinedTextField(value = lon.value.toString(), onValueChange = { lon.value = it.toDoubleOrNull() ?: lon.value }, label = { Text("lon") })
        OutlinedTextField(value = radio.value.toString(), onValueChange = { radio.value = it.toIntOrNull() ?: radio.value }, label = { Text("radio m") })
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                try {
                    result.value = repo.buscar(lat.value, lon.value, radio.value)
                } catch (e: Exception) {
                    result.value = "Error: ${e.message}"
                }
            }
        }) { Text("Buscar") }
        Spacer(Modifier.height(12.dp))
        Text(result.value)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = vetId.value, onValueChange = { vetId.value = it }, label = { Text("vetId") })
        Spacer(Modifier.height(8.dp))
        Button(onClick = { if (vetId.value.isNotBlank()) onOpenVet(vetId.value) }) { Text("Ver ofertas del Vet") }
    }
}
