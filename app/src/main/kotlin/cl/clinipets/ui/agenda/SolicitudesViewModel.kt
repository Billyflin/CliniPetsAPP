package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.AgendaApi
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.CrearSolicitudRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Procedimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class SolicitudesViewModel @Inject constructor(
    private val agendaApi: AgendaApi,
    private val mascotasApi: MascotasApi,
    private val veterinariosApi: VeterinariosApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val mascotas: List<Mascota> = emptyList(),
        val procedimientos: List<Procedimiento> = emptyList(),
        val error: String? = null,
        val successMessage: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /**
     * Carga la información necesaria para que el cliente pueda publicar una solicitud:
     * mascotas registradas y procedimientos disponibles.
     */
    fun loadFormData(forceRefresh: Boolean = false) {
        val current = _ui.value
        if (!forceRefresh &&
            current.mascotas.isNotEmpty() &&
            current.procedimientos.isNotEmpty()
        ) return

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val mascotasResp = mascotasApi.listarMisMascotas()
                val procedimientosResp = veterinariosApi.listarProcedimientos()

                val mascotas = mascotasResp.takeBodyOrThrow("Error listando mascotas")
                val procedimientos = procedimientosResp.takeBodyOrThrow("Error listando procedimientos")

                _ui.update {
                    it.copy(
                        isLoading = false,
                        mascotas = mascotas,
                        procedimientos = procedimientos,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar datos") }
            }
        }
    }

    /**
     * Publica una solicitud (Flujo 2) usando los datos ingresados en la pantalla.
     */
    fun submitSolicitud(request: CrearSolicitudRequest, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.crearSolicitudServicio(request)
                if (resp.isSuccessful) {
                    _ui.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Solicitud enviada correctamente",
                            error = null
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: resp.message().ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isSubmitting = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isSubmitting = false, error = e.message ?: "Error al enviar solicitud") }
            }
        }
    }

    fun clearStatus() {
        _ui.update { it.copy(error = null, successMessage = null) }
    }

    private fun <T> Response<List<T>>.takeBodyOrThrow(defaultMessage: String): List<T> {
        if (this.isSuccessful) {
            return this.body().orEmpty()
        } else {
            val msg = this.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: defaultMessage
            throw IllegalStateException(msg)
        }
    }
}
