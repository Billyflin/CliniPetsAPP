package cl.clinipets.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.AuthControllerApi
import cl.clinipets.openapi.apis.ServicioMedicoControllerApi
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
        val nombreUsuario: String,
        val servicios: List<ServicioMedicoDto>
    ) : HomeUiState()
    data class Error(val mensaje: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authApi: AuthControllerApi,
    private val serviciosApi: ServicioMedicoControllerApi
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
                // Fetch user profile and services in parallel or sequentially
                // Sequential for simplicity and safety first
                val profileResponse = withContext(Dispatchers.IO) { authApi.me() }
                val serviciosResponse = withContext(Dispatchers.IO) { serviciosApi.listarServicios() }

                if (profileResponse.isSuccessful && serviciosResponse.isSuccessful) {
                    val profile = profileResponse.body()
                    val servicios = serviciosResponse.body() ?: emptyList()
                    
                    _uiState.value = HomeUiState.Success(
                        nombreUsuario = profile?.name ?: "Usuario",
                        servicios = servicios
                    )
                } else {
                    val errorMsg = if (!profileResponse.isSuccessful) {
                        "Error perfil: ${profileResponse.code()}"
                    } else {
                        "Error servicios: ${serviciosResponse.code()}"
                    }
                    _uiState.value = HomeUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
