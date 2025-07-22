// ui/screens/vet/MedicalConsultationScreen.kt
package cl.clinipets.ui.screens.vet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.ConsultationViewModel
import cl.clinipets.ui.viewmodels.InventoryViewModel
import cl.clinipets.ui.viewmodels.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalConsultationScreen(
    appointmentId: String,
    onConsultationFinished: () -> Unit,
    onNavigateBack: () -> Unit,
    consultationViewModel: ConsultationViewModel = hiltViewModel(),
    vetViewModel: VetViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {

}