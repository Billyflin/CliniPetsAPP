package cl.clinipets.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.ActualizarPerfilRequest
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import cl.clinipets.openapi.models.Veterinario
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
        val perfil: Veterinario? = null,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    fun clearError() { _ui.update { it.copy(error = null) } }

    /**
     * Carga el perfil. Llama a runPerfil permitiendo una respuesta null
     * (significa que el perfil aún no existe).
     */
    fun loadMyProfile() = runPerfil(allowNullBodyOnSuccess = true) { // --- CAMBIO ---
        veterinariosApi.miPerfil()
    }



    /**
     * Envía el registro. Esto SÍ debe devolver un cuerpo,
     * por lo que allowNullBodyOnSuccess es false (por defecto).
     */
    fun submit(request: RegistrarVeterinarioRequest) = runPerfil {
        veterinariosApi.registrar(request)
    }

    /**
     * Actualiza el perfil. Esto SÍ debe devolver un cuerpo,
     * por lo que allowNullBodyOnSuccess es false (por defecto).
     */
    fun updateMyProfile(request: ActualizarPerfilRequest) = runPerfil {
        veterinariosApi.actualizarMiPerfil(request)
    }

    /**
     * Ejecutador genérico de llamadas de perfil.
     * @param allowNullBodyOnSuccess Si es true, un 200 OK con body null
     * se tratará como éxito (perfil = null), no como error.
     */
    private fun runPerfil(
        allowNullBodyOnSuccess: Boolean = false, // --- CAMBIO ---
        block: suspend () -> Response<Veterinario>
    ) {
        _ui.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val resp = block()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        // Éxito estándar, se recibe un perfil
                        _ui.update {
                            it.copy(perfil = body, error = null)
                        }
                    } else {
                        // El cuerpo es NULL
                        if (allowNullBodyOnSuccess) {
                            // --- CAMBIO ---
                            // Es el caso de loadMyProfile: 200 OK con null.
                            // Esto es un éxito, significa que no hay perfil.
                            _ui.update { it.copy(perfil = null, error = null) }
                        } else {
                            // Es el caso de submit/update: 200 OK con null.
                            // Esto es inesperado, lo tratamos como error.
                            _ui.update { it.copy(error = "Respuesta vacía del servidor (${resp.code()})") }
                        }
                    }
                } else {
                    // La respuesta no fue 2xx
                    val msg = resp.errorBody()?.string()?.takeIf { !it.isNullOrBlank() }
                        ?: resp.message().ifBlank { "Solicitud fallida" }
                    _ui.update { it.copy(error = "Error ${resp.code()}: $msg") }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                // Error de red u otro
                _ui.update { it.copy(error = e.message ?: "Error inesperado") }
            } finally {
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }
}