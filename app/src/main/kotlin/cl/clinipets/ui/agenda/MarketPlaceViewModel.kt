package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.AgendaApi
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.CrearOfertaRequest
import cl.clinipets.openapi.models.CrearSolicitudRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.OfertaVeterinarioResponse
import cl.clinipets.openapi.models.Procedimiento
import cl.clinipets.openapi.models.ReservaDto
import cl.clinipets.openapi.models.SolicitudDisponibleDto
import cl.clinipets.openapi.models.SolicitudServicioResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MarketPlaceViewModel @Inject constructor(
    private val agendaApi: AgendaApi,
    private val mascotasApi: MascotasApi,
    private val veterinariosApi: VeterinariosApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val mascotas: List<Mascota> = emptyList(),
        val procedimientos: List<Procedimiento> = emptyList(),
        val solicitudesDisponibles: List<SolicitudDisponibleDto> = emptyList(),
        val reservasAceptadas: List<ReservaDto> = emptyList(),
        val misSolicitudes: List<SolicitudServicioResponse> = emptyList(),
        val misOfertas: List<OfertaVeterinarioResponse> = emptyList()
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /**
     * Carga la información necesaria para que el cliente pueda publicar una solicitud:
     * mascotas registradas y procedimientos disponibles.
     */
    fun loadFormData(forceRefresh: Boolean = false) {
        val current = _ui.value
        if (!forceRefresh && current.mascotas.isNotEmpty() && current.procedimientos.isNotEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val mascotasResp = mascotasApi.listarMisMascotas()
                val procedimientosResp = veterinariosApi.listarProcedimientos()

                val mascotas = mascotasResp.takeBodyOrThrow("Error listando mascotas")
                val procedimientos = procedimientosResp.takeBodyOrThrow("Error listando procedimientos")

                _ui.update { it.copy(isLoading = false, mascotas = mascotas, procedimientos = procedimientos) }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar datos") }
            }
        }
    }

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

    fun refrescarSolicitudes() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.getSolicitudesDisponibles()
                if (resp.isSuccessful) {
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            solicitudesDisponibles = resp.body().orEmpty(),
                            error = null
                        )
                    }
                } else {
                    val msg =
                        resp.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: resp.message()
                            .ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.message ?: "Error de red") }
            }
        }
    }

    fun enviarOferta(
        solicitudId: String, request: CrearOfertaRequest, onSuccess: (() -> Unit)? = null
    ) {
        val uuid = runCatching { UUID.fromString(solicitudId) }.getOrNull() ?: return _ui.update {
            it.copy(error = "ID de solicitud inválido")
        }

        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.crearOferta(uuid, request)
                if (resp.isSuccessful) {
                    _ui.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Oferta enviada",
                            solicitudesDisponibles = it.solicitudesDisponibles.filterNot { s -> s.id == uuid },
                            error = null
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: resp.message()
                        .ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isSubmitting = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isSubmitting = false, error = e.message ?: "No fue posible enviar la oferta"
                    )
                }
            }
        }
    }

    fun aceptarOferta(ofertaId: UUID, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.aceptarOferta(ofertaId)
                if (resp.isSuccessful) {
                    val reservas = resp.body().orEmpty()
                    _ui.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Oferta aceptada (${reservas.size} reserva(s))",
                            reservasAceptadas = reservas,
                            error = null
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: resp.message()
                        .ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isSubmitting = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isSubmitting = false, error = e.message ?: "No fue posible aceptar la oferta") }
            }
        }
    }

    fun refrescarMisSolicitudes() {
        viewModelScope.launch {
            try {
                val resp = agendaApi.getMisSolicitudes()
                if (resp.isSuccessful) {
                    _ui.update { it.copy(misSolicitudes = resp.body().orEmpty()) }
                } else {
                    _ui.update { it.copy(error = "Error ${resp.code()} listando mis solicitudes") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Error de red al listar mis solicitudes") }
            }
        }
    }

    fun refrescarMisOfertas() {
        viewModelScope.launch {
            try {
                val resp = agendaApi.getMisOfertas()
                if (resp.isSuccessful) {
                    _ui.update { it.copy(misOfertas = resp.body().orEmpty()) }
                } else {
                    _ui.update { it.copy(error = "Error ${resp.code()} listando mis ofertas") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Error de red al listar mis ofertas") }
            }
        }
    }

    fun clearStatus() { _ui.update { it.copy(error = null, successMessage = null) } }

    // Extensión usada en loadFormData
    private fun <T> Response<List<T>>.takeBodyOrThrow(defaultMessage: String): List<T> {
        return if (isSuccessful) body().orEmpty() else {
            val msg = errorBody()?.string()?.takeIf { it.isNotBlank() } ?: defaultMessage
            throw IllegalStateException(msg)
        }
    }
}
