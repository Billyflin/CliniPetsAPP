package cl.clinipets.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.ActualizarPerfilRequest
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import cl.clinipets.openapi.models.VeterinarioPerfil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val veterinariosApi: VeterinariosApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val perfil: VeterinarioPerfil? = null,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    fun clearError() { _ui.update { it.copy(error = null) } }

    fun loadMyProfile() = runPerfil { veterinariosApi.miPerfil() }

    fun submit(request: RegistrarVeterinarioRequest) = runPerfil { veterinariosApi.registrar(request) }

    fun updateMyProfile(request: ActualizarPerfilRequest) = runPerfil { veterinariosApi.actualizarMiPerfil(request) }

    private fun runPerfil(block: suspend () -> Response<VeterinarioPerfil>) {
        _ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val resp = block()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        _ui.update { it.copy(perfil = body, error = null) }
                    } else {
                        _ui.update { it.copy(error = "Respuesta vacía del servidor (${resp.code()})") }
                    }
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { !it.isNullOrBlank() }
                        ?: resp.message().ifBlank { "Solicitud fallida" }
                    _ui.update { it.copy(error = "Error ${resp.code()}: $msg") }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Error inesperado") }
            } finally {
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }
}