package cl.clinipets.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.HomeControllerApi
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.ServicioMedicoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val saludo: String,
        val mensajeIa: String,
        val mascotas: List<MascotaResponse>,
        val serviciosDestacados: List<ServicioMedicoDto>,
        val todosLosServicios: List<ServicioMedicoDto>
    ) : HomeUiState()
    data class Error(val mensaje: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeApi: HomeControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        cargarDatosIniciales()
    }

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val dashboardResponse = withContext(Dispatchers.IO) { homeApi.obtenerDashboard() }

                if (dashboardResponse.isSuccessful) {
                    dashboardResponse.body()?.let { dashboard ->
                        _uiState.value = HomeUiState.Success(
                            saludo = dashboard.saludo,
                            mensajeIa = dashboard.mensajeIa,
                            mascotas = dashboard.mascotas,
                            serviciosDestacados = dashboard.serviciosDestacados,
                            todosLosServicios = dashboard.todosLosServicios
                        )
                    } ?: run {
                        _uiState.value = HomeUiState.Error("Respuesta de dashboard vacía")
                    }
                } else {
                    val errorMsg = runCatching { dashboardResponse.errorBody()?.string() }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: "Error dashboard: ${dashboardResponse.code()}"
                    _uiState.value = HomeUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
