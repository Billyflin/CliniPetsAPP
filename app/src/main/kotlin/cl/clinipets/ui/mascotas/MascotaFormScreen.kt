package cl.clinipets.ui.mascotas

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.CrearMascota
import java.util.UUID

@Composable
fun MascotaFormScreen(
    mascotaId: UUID?,
    onBack: () -> Unit,
    vm: MascotasViewModel = hiltViewModel()
) {
    val error by vm.error.collectAsState()
    val cargando by vm.cargando.collectAsState()

    Column {
        Text(text = if (mascotaId == null) "Crear mascota (mínima)" else "Editar mascota (mínima)")
        Text(text = "mascotaId: $mascotaId")

        if (mascotaId == null) {
            Button(enabled = !cargando, onClick = {
                val nombre = "Demo-" + System.currentTimeMillis().toString().takeLast(5)
                vm.crear(
                    CrearMascota(
                        nombre = nombre,
                        especie = CrearMascota.Especie.PERRO
                    )
                )
            }) { Text("Crear demo") }
        } else {
            Button(enabled = !cargando, onClick = {
                val nuevo = "Actualizada-" + System.currentTimeMillis().toString().takeLast(5)
                vm.actualizar(
                    mascotaId,
                    ActualizarMascota(nombre = nuevo)
                )
            }) { Text("Actualizar demo") }
        }

        Button(onClick = onBack) { Text("Volver") }

        if (error != null) {
            Text(text = error!!)
            Button(onClick = { vm.limpiarError() }) { Text("Limpiar error") }
        }
    }
}