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
import javax.inject.Inject

@HiltViewModel
class StaffServiciosViewModel @Inject constructor(
    private val api: ServicioMedicoControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServiciosUiState>(ServiciosUiState.Loading)
    val uiState: StateFlow<ServiciosUiState> = _uiState.asStateFlow()

    init {
        cargarServicios()
    }

    fun cargarServicios() {
        viewModelScope.launch {
            _uiState.value = ServiciosUiState.Loading
            try {
                val response = api.listarServicios()
                if (response.isSuccessful) {
                    _uiState.value = ServiciosUiState.Success(response.body() ?: emptyList())
                } else {
                    _uiState.value = ServiciosUiState.Error("Error al cargar servicios: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = ServiciosUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class ServiciosUiState {
    object Loading : ServiciosUiState()
    data class Success(val servicios: List<ServicioMedicoDto>) : ServiciosUiState()
    data class Error(val mensaje: String) : ServiciosUiState()
}
