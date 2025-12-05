package cl.clinipets.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StaffAgendaViewModel @Inject constructor(
    private val reservaApi: ReservaControllerApi
) : ViewModel() {

    data class UiState(
        val date: LocalDate = LocalDate.now(),
        val appointments: List<CitaDetalladaResponse> = emptyList(),
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

    fun cargarAgenda(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, date = date) }
            try {
                val response = reservaApi.obtenerAgendaDiaria(date)
                if (response.isSuccessful) {
                    val allAppointments = response.body() ?: emptyList()
                    val filtered = allAppointments.filter {
                        it.estado in VALID_STATES
                    }
                    _uiState.update { it.copy(appointments = filtered, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar agenda: ${response.code()}") }
                }
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
}
