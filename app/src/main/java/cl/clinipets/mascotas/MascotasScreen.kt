package cl.clinipets.mascotas

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.clinipets.network.Mascota

@Composable
fun MascotasScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory) {
    val vm: MascotasViewModel = viewModel(factory = viewModelFactory)
    val mascotas by vm.mascotas.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }
    var nuevaEspecie by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.loadMascotas()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mis mascotas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Agregar mascota")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Text("Cargando...", style = MaterialTheme.typography.bodyLarge)
        }

        if (!error.isNullOrEmpty()) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(mascotas) { mascota: Mascota ->
                MascotaRow(mascota = mascota, onDelete = { id -> vm.eliminarMascota(id) })
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Crear mascota") },
            text = {
                Column {
                    OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it }, label = { Text("Nombre") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = nuevaEspecie, onValueChange = { nuevaEspecie = it }, label = { Text("Especie (PERRO/GATO)") })
                }
            },
            confirmButton = {
                Log.d("MascotasScreen", "Confirm button clicked")
                TextButton(onClick = {
                    if (nuevoNombre.isNotBlank() && (nuevaEspecie == "PERRO" || nuevaEspecie == "GATO")) {
                        vm.crearMascota(nuevoNombre, nuevaEspecie) { success ->
                            if (success) {
                                showCreate = false
                                nuevoNombre = ""
                                nuevaEspecie = ""
                            }
                        }
                    } else {
                        // simple validation
                    }
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MascotaRow(mascota: Mascota, onDelete: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(mascota.nombre, style = MaterialTheme.typography.titleMedium)
            Text(mascota.especie, style = MaterialTheme.typography.bodySmall)
        }
        val id = mascota.id
        IconButton(onClick = { if (id != null) onDelete(id) }, enabled = id != null) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar")
        }
    }
}
