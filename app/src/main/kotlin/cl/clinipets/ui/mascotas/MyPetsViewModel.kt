package cl.clinipets.ui.mascotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.models.MascotaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyPetsUiState(
    val isLoading: Boolean = false,
    val pets: List<MascotaResponse> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MyPetsViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPetsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadPets()
    }

    fun loadPets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = mascotaApi.listarMascotas()
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(isLoading = false, pets = response.body() ?: emptyList())
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudieron cargar tus mascotas") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }
}
