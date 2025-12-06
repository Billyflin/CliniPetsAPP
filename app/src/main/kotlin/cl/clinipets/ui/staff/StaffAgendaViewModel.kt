package cl.clinipets.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.BloqueoControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.BloqueoAgenda
import cl.clinipets.openapi.models.BloqueoCreateRequest
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.openapi.models.ResumenDiarioResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
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
        val isRefreshingSilently: Boolean = false,
        val error: String? = null,
        val resumen: ResumenDiarioResponse? = null,
        val lastUpdated: LocalTime? = null
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
        startAutoRefresh()
    }

    fun refresh() {
        cargarAgenda(_uiState.value.date)
    }

    fun cargarAgenda(date: LocalDate, isSilent: Boolean = false) {
        viewModelScope.launch {
            fetchAgenda(date, isSilent)
        }
    }

    private suspend fun fetchAgenda(date: LocalDate, isSilent: Boolean) {
        if (isSilent) {
            _uiState.update { it.copy(date = date, error = null, isRefreshingSilently = true) }
        } else {
            _uiState.update { it.copy(date = date, error = null, isLoading = true, isRefreshingSilently = false) }
        }
        try {
            val (citasResponse, bloqueosResponse, resumenResponse) = coroutineScope {
                val citasDeferred = async { reservaApi.obtenerAgendaDiaria(date) }
                val bloqueosDeferred = async { bloqueoApi.listarBloqueos(date) }
                val resumenDeferred = async { reservaApi.obtenerResumenDiario(date) }
                Triple(citasDeferred.await(), bloqueosDeferred.await(), resumenDeferred.await())
            }

            val citas = if (citasResponse.isSuccessful) {
                citasResponse.body().orEmpty().filter { it.estado in VALID_STATES }
            } else emptyList()

            val bloqueos = if (bloqueosResponse.isSuccessful) {
                bloqueosResponse.body().orEmpty()
            } else emptyList()

            val items = (citas.map { ItemCita(it) } + bloqueos.map { ItemBloqueo(it) })
                .sortedBy { it.hora }

            val resumen = resumenResponse.takeIf { it.isSuccessful }?.body()

            val errorMsg = when {
                !citasResponse.isSuccessful -> "Error al cargar citas: ${citasResponse.code()}"
                !bloqueosResponse.isSuccessful -> "Error al cargar bloqueos: ${bloqueosResponse.code()}"
                !resumenResponse.isSuccessful -> "Error al cargar resumen diario: ${resumenResponse.code()}"
                else -> null
            }

            val updatedTime = LocalTime.now()

            _uiState.update {
                it.copy(
                    agendaItems = items,
                    isLoading = if (isSilent) it.isLoading else false,
                    isRefreshingSilently = false,
                    error = errorMsg,
                    resumen = resumen,
                    lastUpdated = if (errorMsg == null) updatedTime else it.lastUpdated
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = if (isSilent) it.isLoading else false,
                    isRefreshingSilently = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                fetchAgenda(_uiState.value.date, isSilent = true)
                delay(15_000)
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
