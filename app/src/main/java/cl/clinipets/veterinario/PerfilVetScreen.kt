package cl.clinipets.veterinario

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

@Composable
fun PerfilVetScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory) {
    val vm: VeterinarioViewModel = viewModel(factory = viewModelFactory)
    val perfil by vm.perfil.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var licencia by remember { mutableStateOf("") }
    var modos by remember { mutableStateOf("DOMICILIO,CLINICA") }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }
    var radioKm by remember { mutableStateOf(10) }

    LaunchedEffect(Unit) {
        vm.cargarMiPerfil()
    }

    LaunchedEffect(perfil) {
        perfil?.let {
            nombre = it.nombreCompleto
            licencia = ""
            modos = it.modosAtencion.joinToString(",")
            lat = it.lat ?: 0.0
            lng = it.lng ?: 0.0
            radioKm = it.radioCoberturaKm ?: 10
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mi Perfil Veterinario", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (perfil != null) {
            Text(if (perfil!!.verificado) "Verificado ✅" else "No verificado ⏳")
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = licencia, onValueChange = { licencia = it }, label = { Text("Número de licencia (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = modos, onValueChange = { modos = it }, label = { Text("Modos (coma-separados)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lat.toString(), onValueChange = { lat = it.toDoubleOrNull() ?: lat }, label = { Text("Lat") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lng.toString(), onValueChange = { lng = it.toDoubleOrNull() ?: lng }, label = { Text("Lng") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = radioKm.toString(), onValueChange = { radioKm = it.toIntOrNull() ?: radioKm }, label = { Text("Radio cobertura (km)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            val listaModos = modos.split(',').map { it.trim().uppercase() }.filter { it.isNotBlank() }
            vm.actualizarPerfil(nombre, if (licencia.isBlank()) null else licencia, listaModos, lat, lng, radioKm)
        }, modifier = Modifier.fillMaxWidth()) { Text(if (isLoading) "Guardando..." else "Guardar cambios") }

        if (!error.isNullOrEmpty()) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

