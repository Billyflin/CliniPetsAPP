// ui/screens/PetDetailScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.PetsViewModel

@Composable
fun PetDetailScreen(
    petId: String,
    onNavigateBack: () -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    val petsState by viewModel.petsState.collectAsState()

    LaunchedEffect(petId) {
        viewModel.loadPetDetail(petId)
    }

    LaunchedEffect(petsState.isPetDeleted) {
        if (petsState.isPetDeleted) {
            viewModel.clearState()
            onNavigateBack()
        }
    }
    Column {
        petsState.selectedPet?.let { pet ->
            Text("${pet.name}")
            Text("Especie: ${pet.species}")
            Text("Raza: ${pet.breed}")
            Text("Edad: ${pet.age} años")
            Text("Peso: ${pet.weight} kg")
            Button(onClick = onNavigateBack) { Text("Volver") }
            OutlinedButton(onClick = { viewModel.deletePet(petId) }) { Text("Eliminar") }
        }
    }
}