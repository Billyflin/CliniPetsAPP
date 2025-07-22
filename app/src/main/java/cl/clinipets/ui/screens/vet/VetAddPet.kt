package cl.clinipets.ui.screens.vet

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.PetsViewModel

@Composable
fun VetAddPetScreen(
    onPetAdded: () -> Boolean,
    onNavigateBack: () -> Boolean,
    petViewModel: PetsViewModel = hiltViewModel()
) {


}
