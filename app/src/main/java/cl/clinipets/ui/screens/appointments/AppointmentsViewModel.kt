// ui/screens/appointments/AppointmentsViewModel.kt
package cl.clinipets.ui.screens.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.repository.ClinipetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val repository: ClinipetsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    init {
        loadAppointments()
    }

    private fun loadAppointments() {
        viewModelScope.launch {
            repository.appointments
                .map { appointments ->
                    val now = System.currentTimeMillis()
                    val upcoming = appointments.filter {
                        it.dateTime >= now &&
                                it.status != AppointmentStatus.COMPLETED &&
                                it.status != AppointmentStatus.CANCELLED
                    }.sortedBy { it.dateTime }

                    val past = appointments.filter {
                        it.dateTime < now ||
                                it.status == AppointmentStatus.COMPLETED ||
                                it.status == AppointmentStatus.CANCELLED
                    }.sortedByDescending { it.dateTime }

                    AppointmentsUiState(
                        upcomingAppointments = upcoming,
                        pastAppointments = past,
                        isLoading = false
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
}

data class AppointmentsUiState(
    val upcomingAppointments: List<Appointment> = emptyList(),
    val pastAppointments: List<Appointment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)