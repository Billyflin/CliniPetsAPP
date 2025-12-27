package cl.clinipets.ui.staff.servicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.ServicioMedicoControllerApi
import cl.clinipets.openapi.models.ServicioMedicoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StaffServicioDetailViewModel @Inject constructor(
    private val api: ServicioMedicoControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var allServicios = emptyList<ServicioMedicoDto>()

    fun cargarServicio(id: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val listResponse = api.listarServicios()
                if (listResponse.isSuccessful) {
                    allServicios = listResponse.body() ?: emptyList()
                    val servicio = allServicios.find { it.id.toString() == id }
                    if (servicio != null) {
                        _uiState.value = DetailUiState.Success(servicio, allServicios.filter { it.id != servicio.id })
                    } else {
                        _uiState.value = DetailUiState.Error("Servicio no encontrado")
                    }
                } else {
                    _uiState.value = DetailUiState.Error("Error al cargar servicios")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun guardarDependencias(id: String, dependencias: Set<UUID>) {
        viewModelScope.launch {
            try {
                val response = api.actualizarDependencias(UUID.fromString(id), dependencias)
                if (response.isSuccessful) {
                    cargarServicio(id) // Recargar para confirmar cambios
                } else {
                    // Manejar error de guardado
                }
            } catch (e: Exception) {
                // Manejar excepción
            }
        }
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val servicio: ServicioMedicoDto, val disponibles: List<ServicioMedicoDto>) : DetailUiState()
    data class Error(val mensaje: String) : DetailUiState()
}
