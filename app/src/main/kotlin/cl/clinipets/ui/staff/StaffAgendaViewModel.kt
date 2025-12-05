package cl.clinipets.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.BloqueoControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.BloqueoAgenda
import cl.clinipets.openapi.models.BloqueoCreateRequest
import cl.clinipets.openapi.models.CitaDetalladaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

sealed interface AgendaItem {
    val hora: OffsetDateTime
}

data class ItemCita(val data: CitaDetalladaResponse) : AgendaItem {
    override val hora: OffsetDateTime = data.fechaHoraInicio
}

data class ItemBloqueo(val data: BloqueoAgenda) : AgendaItem {
    override val hora: OffsetDateTime = data.fechaHoraInicio
}

@HiltViewModel
class StaffAgendaViewModel @Inject constructor(
    private val reservaApi: ReservaControllerApi,
    private val bloqueoApi: BloqueoControllerApi
) : ViewModel() {

    data class UiState(
        val date: LocalDate = LocalDate.now(),
        val agendaItems: List<AgendaItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        val VALID_STATES = setOf(
            CitaDetalladaResponse.Estado.EN_SALA,
            CitaDetalladaResponse.Estado.EN_ATENCION,
            CitaDetalladaResponse.Estado.PENDIENTE_PAGO,
            CitaDetalladaResponse.Estado.CONFIRMADA,
            CitaDetalladaResponse.Estado.FINALIZADA,
            CitaDetalladaResponse.Estado.CANCELADA
        )
    }

    init {
        cargarAgenda(_uiState.value.date)
    }

    fun refresh() {
        cargarAgenda(_uiState.value.date)
    }

    fun cargarAgenda(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, date = date) }
            try {
                val citasDeferred = async { reservaApi.obtenerAgendaDiaria(date) }
                val bloqueosDeferred = async { bloqueoApi.listarBloqueos(date) }

                val citasResponse = citasDeferred.await()
                val bloqueosResponse = bloqueosDeferred.await()

                val citas = if (citasResponse.isSuccessful) {
                    citasResponse.body().orEmpty().filter { it.estado in VALID_STATES }
                } else emptyList()

                val bloqueos = if (bloqueosResponse.isSuccessful) {
                    bloqueosResponse.body().orEmpty()
                } else emptyList()

                val items = (citas.map { ItemCita(it) } + bloqueos.map { ItemBloqueo(it) })
                    .sortedBy { it.hora }

                val errorMsg = when {
                    !citasResponse.isSuccessful -> "Error al cargar citas: ${citasResponse.code()}"
                    !bloqueosResponse.isSuccessful -> "Error al cargar bloqueos: ${bloqueosResponse.code()}"
                    else -> null
                }

                _uiState.update { it.copy(agendaItems = items, isLoading = false, error = errorMsg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun cambiarFecha(newDate: LocalDate) {
        cargarAgenda(newDate)
    }

    fun cancelarCita(uuid: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = reservaApi.cancelarReserva(uuid)
                if (response.isSuccessful) {
                    // Recargar para actualizar estado
                    cargarAgenda(_uiState.value.date)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cancelar: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun crearBloqueo(inicio: LocalTime, fin: LocalTime, motivo: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val date = _uiState.value.date
                val startDateTime = date.atTime(inicio).atZone(ZoneId.systemDefault()).toOffsetDateTime()
                val endDateTime = date.atTime(fin).atZone(ZoneId.systemDefault()).toOffsetDateTime()

                val request = BloqueoCreateRequest(
                    fechaHoraInicio = startDateTime,
                    fechaHoraFin = endDateTime,
                    motivo = motivo.takeUnless { it.isNullOrBlank() }
                )

                val response = bloqueoApi.crearBloqueo(request)
                if (response.isSuccessful) {
                    cargarAgenda(_uiState.value.date)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No pudimos crear el bloqueo: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al crear bloqueo") }
            }
        }
    }

    fun eliminarBloqueo(id: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = bloqueoApi.eliminarBloqueo(id)
                if (response.isSuccessful) {
                    cargarAgenda(_uiState.value.date)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No pudimos eliminar el bloqueo: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al eliminar bloqueo") }
            }
        }
    }
}
