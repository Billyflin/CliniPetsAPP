package cl.clinipets.ui.mascotas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.openapi.models.MascotaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PetDetailUiState(
    val isLoading: Boolean = true,
    val pet: MascotaResponse? = null,
    val history: List<CitaDetalladaResponse> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PetDetailViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi,
    private val reservaApi: ReservaControllerApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val petId: String? = savedStateHandle["petId"]

    private val _uiState = MutableStateFlow(PetDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        petId?.let { loadPet(it) } ?: _uiState.update { it.copy(isLoading = false, error = "Mascota no encontrada") }
    }

    fun refresh() {
        petId?.let { loadPet(it) }
    }

    private fun loadPet(id: String) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        if (uuid == null) {
            _uiState.update { it.copy(isLoading = false, error = "Identificador de mascota inválido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val mascotaResponse = mascotaApi.obtenerMascota(uuid)
                val historyResponse = reservaApi.listarReservasPorMascota(uuid)

                if (mascotaResponse.isSuccessful) {
                    val pet = mascotaResponse.body()
                    val history = if (historyResponse.isSuccessful) {
                        historyResponse.body().orEmpty().sortedByDescending { it.fechaHoraInicio }
                    } else emptyList()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pet = pet,
                            history = history
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No pudimos cargar la mascota") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }
}
