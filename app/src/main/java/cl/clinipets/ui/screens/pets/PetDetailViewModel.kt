// ui/screens/pets/PetDetailViewModel.kt
package cl.clinipets.ui.screens.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Pet
import cl.clinipets.data.repository.ClinipetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetDetailViewModel @Inject constructor(
    private val repository: ClinipetsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetDetailUiState())
    val uiState: StateFlow<PetDetailUiState> = _uiState.asStateFlow()

    fun loadPetDetail(petId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.pets
                .map { pets -> pets.find { it.id == petId } }
                .collect { pet ->
                    _uiState.update {
                        it.copy(
                            pet = pet,
                            isLoading = false
                        )
                    }
                }
        }
    }
}

data class PetDetailUiState(
    val pet: Pet? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)