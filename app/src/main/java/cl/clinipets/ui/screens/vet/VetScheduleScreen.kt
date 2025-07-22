// ui/screens/vet/VetDashboardScreen.kt
package cl.clinipets.ui.screens.vet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetScheduleScreen(
    onNavigateBack: () -> Unit, viewModel: VetViewModel = hiltViewModel()
) {
    //TODO This screen allows veterinarians to manage their schedules, including adding, editing, and deleting time slots for appointments.

}