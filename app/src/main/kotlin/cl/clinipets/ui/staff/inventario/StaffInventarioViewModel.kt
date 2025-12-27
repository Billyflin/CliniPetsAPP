package cl.clinipets.ui.staff.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.InventarioControllerApi
import cl.clinipets.openapi.models.InsumoDetalladoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffInventarioViewModel @Inject constructor(
    private val api: InventarioControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventarioUiState>(InventarioUiState.Loading)
    val uiState: StateFlow<InventarioUiState> = _uiState.asStateFlow()

    init {
        cargarAlertas()
    }

    fun cargarAlertas() {
        viewModelScope.launch {
            _uiState.value = InventarioUiState.Loading
            try {
                val response = api.obtenerAlertasStock()
                if (response.isSuccessful) {
                    _uiState.value = InventarioUiState.Success(response.body() ?: emptyList())
                } else {
                    _uiState.value = InventarioUiState.Error("Error al cargar alertas: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = InventarioUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class InventarioUiState {
    object Loading : InventarioUiState()
    data class Success(val alertas: List<InsumoDetalladoDto>) : InventarioUiState()
    data class Error(val mensaje: String) : InventarioUiState()
}
