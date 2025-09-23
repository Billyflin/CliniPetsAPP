package cl.clinipets.history.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PetListScreen(
    modifier: Modifier = Modifier, onPetSelected: ((String) -> Unit)? = null
) {
    Box(modifier) {
        Text(text = "PetListScreen")
        // Aquí iría la implementación real de la pantalla de lista de mascotas
        // pero de momento pondremos un boton para retornar un string de prueba
        Button(
            onClick = { onPetSelected?.invoke("pet_id_123") }
        ) {
            Text(text = "Seleccionar Mascota de Prueba")
        }
    }
}

@Preview(name = "PetListScreen")
@Composable
private fun PreviewPetListScreen() {
    PetListScreen()
}