// ui/screens/PetsScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.PetsViewModel

@Composable
fun PetsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddPet: () -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    val petsState by viewModel.petsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPets()
    }
    Column {
        Text("Mascotas")
        Button(onClick = onNavigateToAddPet) { Text("Agregar") }

        petsState.pets.forEach { pet ->
            Card(modifier = Modifier.clickable { onNavigateToPetDetail(pet.id) }) {
                Text("${pet.name} - ${pet.species}")
            }
        }

        Button(onClick = onNavigateBack) { Text("Volver") }
    }
}