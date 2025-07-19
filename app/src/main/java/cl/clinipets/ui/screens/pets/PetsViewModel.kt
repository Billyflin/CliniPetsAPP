// ui/screens/pets/PetsViewModel.kt
package cl.clinipets.ui.screens.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.data.repository.ClinipetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetsViewModel @Inject constructor(
    private val repository: ClinipetsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetsUiState())
    val uiState: StateFlow<PetsUiState> = _uiState.asStateFlow()

    init {
        loadPets()
    }

    private fun loadPets() {
        viewModelScope.launch {
            repository.pets
                .map { pets ->
                    when (_uiState.value.selectedFilter) {
                        PetFilter.ALL -> pets
                        PetFilter.DOGS -> pets.filter { it.species == PetSpecies.DOG }
                        PetFilter.CATS -> pets.filter { it.species == PetSpecies.CAT }
                        PetFilter.OTHERS -> pets.filter {
                            it.species != PetSpecies.DOG && it.species != PetSpecies.CAT
                        }
                    }
                }
                .collect { filteredPets ->
                    _uiState.update {
                        it.copy(
                            pets = filteredPets,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun updateFilter(filter: PetFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        loadPets()
    }
}

data class PetsUiState(
    val pets: List<Pet> = emptyList(),
    val selectedFilter: PetFilter = PetFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)