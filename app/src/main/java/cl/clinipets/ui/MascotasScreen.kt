package cl.clinipets.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.clinipets.domain.mascotas.MascotasRepository
import kotlinx.coroutines.launch
import java.util.logging.Logger

@Suppress("FunctionName")
@Composable
fun MascotasScreen(repo: MascotasRepository) {
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<cl.clinipets.data.dto.Mascota>() }
    val loading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    val nombreNueva = remember { mutableStateOf("") }
    val especieNueva = remember { mutableStateOf<cl.clinipets.data.dto.Especie>(cl.clinipets.data.dto.Especie.PERRO) }

    val editarId = remember { mutableStateOf("") }
    val editarNombre = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading.value = true
        error.value = null
        runCatching { repo.listarMias() }
            .onSuccess { list -> items.clear(); items.addAll(list) }
            .onFailure { e -> error.value = e.message }
        loading.value = false
    }

    Column(Modifier.padding(16.dp)) {
        Text(text = "Mis mascotas")
        if (loading.value) Text("Cargando…")
        error.value?.let { Text("Error: $it") }

        // Crear mascota rápida
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = nombreNueva.value,
                onValueChange = { nombreNueva.value = it },
                label = { Text("Nombre nueva") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                scope.launch {
                    loading.value = true
                    error.value = null
                    runCatching {
                        repo.crear(
                            cl.clinipets.data.dto.CrearMascota(
                                nombre = nombreNueva.value.ifBlank { "SinNombre" },
                                especie = especieNueva.value
                            )
                        )
                    }.onSuccess { nueva ->
                        items.add(0, nueva)
                        nombreNueva.value = ""
                    }.onFailure { e -> error.value = e.message }
                    loading.value = false
                }
            }) { Text("Agregar") }
        }

        Spacer(Modifier.height(16.dp))
        // Editar por id (simple)
        Text("Editar nombre por ID")
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(value = editarId.value, onValueChange = { editarId.value = it }, label = { Text("ID mascota") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = editarNombre.value, onValueChange = { editarNombre.value = it }, label = { Text("Nuevo nombre") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val id = editarId.value
            if (id.isBlank()) return@Button
            scope.launch {
                loading.value = true
                error.value = null
                runCatching {
                    repo.actualizar(id, cl.clinipets.data.dto.ActualizarMascota(nombre = editarNombre.value.ifBlank { null }))
                }.onSuccess { updated ->
                    val idx = items.indexOfFirst { it.id == id }
                    if (idx >= 0) items[idx] = updated
                }.onFailure { e -> error.value = e.message }
                loading.value = false
            }
        }) { Text("Guardar cambios") }

        Spacer(Modifier.height(16.dp))
        // Lista simple con eliminar
        items.forEach { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Logger.getLogger("MascotasScreen").info("Mostrando mascota: $m")
                Text(text = "${m.nombre} (${m.especie.name})")
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val id = m.id ?: return@Button
                    scope.launch {
                        loading.value = true
                        error.value = null
                        runCatching { repo.eliminar(id) }
                            .onSuccess { items.removeAll { it.id == id } }
                            .onFailure { e -> error.value = e.message }
                        loading.value = false
                    }
                }) { Text("Eliminar") }
            }
        }
    }
}
