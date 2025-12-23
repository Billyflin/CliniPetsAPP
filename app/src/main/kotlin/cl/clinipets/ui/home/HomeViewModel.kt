package cl.clinipets.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.ServicioMedicoControllerApi
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.ServicioMedicoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val saludo: String,
        val mascotas: List<MascotaResponse>,
        val serviciosDestacados: List<ServicioMedicoDto>,
        val todosLosServicios: List<ServicioMedicoDto>
    ) : HomeUiState()
    data class Error(val mensaje: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi,
    private val servicioApi: ServicioMedicoControllerApi
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
                val mascotasDeferred = async(Dispatchers.IO) { mascotaApi.listarMascotas() }
                val serviciosDeferred = async(Dispatchers.IO) { servicioApi.listarServicios() }

                val mascotasResponse = mascotasDeferred.await()
                val serviciosResponse = serviciosDeferred.await()

                if (mascotasResponse.isSuccessful && serviciosResponse.isSuccessful) {
                    val mascotas = mascotasResponse.body() ?: emptyList()
                    val servicios = serviciosResponse.body() ?: emptyList()
                    
                    _uiState.value = HomeUiState.Success(
                        saludo = "Bienvenido a CliniPets",
                        mascotas = mascotas,
                        serviciosDestacados = servicios.take(3),
                        todosLosServicios = servicios
                    )
                } else {
                    _uiState.value = HomeUiState.Error("Error al cargar datos")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
