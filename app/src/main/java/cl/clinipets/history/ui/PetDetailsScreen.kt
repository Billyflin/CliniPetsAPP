package cl.clinipets.history.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PetDetailsScreen(
    modifier: Modifier = Modifier,
    petId: String = "test_pet_id"
) {
    Box(modifier) {
        Text(text = "Pet ID: $petId")
    }
}

@Preview(name = "PetDetailsScreen")
@Composable
private fun PreviewPetDetailsScreen() {
    PetDetailsScreen()
}