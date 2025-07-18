// ui/screens/home/HomeViewModel.kt
package cl.clinipets.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.Pet
import cl.clinipets.data.repository.ClinipetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ClinipetsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.pets,
                repository.appointments
            ) { pets, appointments ->
                HomeUiState(
                    pets = pets.take(2), // Mostrar solo 2 mascotas en home
                    nextAppointment = appointments
                        .filter { it.status != cl.clinipets.data.model.AppointmentStatus.COMPLETED }
                        .minByOrNull { it.dateTime }
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}

data class HomeUiState(
    val pets: List<Pet> = emptyList(),
    val nextAppointment: Appointment? = null,
    val isLoading: Boolean = false
)