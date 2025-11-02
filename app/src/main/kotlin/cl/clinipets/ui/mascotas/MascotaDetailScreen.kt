package cl.clinipets.ui.mascotas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.UUID

@Composable
fun MascotaDetailScreen(
    mascotaId: UUID, onBack: () -> Unit, vm: MascotasViewModel = hiltViewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val mascota by vm.seleccionada.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) {
        vm.detalle(
            id = mascotaId
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = {
            vm.limpiarSeleccion()
            onBack()
        }) { Text("Volver") }

        when {
            cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            mascota == null -> {
                Text("No se encontró información de la mascota.")
            }

            else -> {
                Text(
                    text = mascota!!.nombre, style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text("Especie: ${mascota!!.especie.value}")
                mascota!!.raza?.takeIf { it.isNotBlank() }?.let { raza ->
                    Spacer(Modifier.height(4.dp))
                    Text("Raza: $raza")
                }
                Button(onClick = { vm.eliminar(mascotaId) }, content = { Text("Borrar") })
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
