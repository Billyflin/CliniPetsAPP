// ui/screens/appointments/AppointmentsViewModel.kt
package cl.clinipets.ui.screens.appointments

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()
}

data class AppointmentsUiState(
    val isLoading: Boolean = false
)