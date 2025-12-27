package cl.clinipets.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.GestinDeAgendaApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StaffCitaDetailViewModel @Inject constructor(
    private val reservaApi: ReservaControllerApi,
    private val gestionAgendaApi: GestinDeAgendaApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val cita: CitaDetalladaResponse? = null,
        val error: String? = null,
        val isCancelled: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun cargarCita(citaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val uuid = UUID.fromString(citaId)
                val response = reservaApi.obtenerReserva(uuid)

                if (response.isSuccessful) {
                    val cita = response.body()
                    if (cita != null) {
                        _uiState.update { it.copy(isLoading = false, cita = cita) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Cita vacía") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun cancelarCita() {
        val cita = _uiState.value.cita ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Usamos el endpoint específico de gestión para Staff
                val response = reservaApi.cancelarReservaPorStaff(cita.id)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, isCancelled = true) }
                    // Recargamos para ver el estado actualizado (aunque isCancelled trigger navegación)
                    cargarCita(cita.id.toString())
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cancelar: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun iniciarTriaje(onNavigate: (String, String) -> Unit) {
        val cita = _uiState.value.cita ?: return
        val mascotaId = cita.detalles.firstOrNull()?.mascotaId?.toString() ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Iniciar atención (Nueva transición de estado simplificada)
                gestionAgendaApi.iniciarAtencion(cita.id)
                _uiState.update { it.copy(isLoading = false) }
                onNavigate(cita.id.toString(), mascotaId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun cambiarEstado(nuevoEstado: CitaDetalladaResponse.Estado) {
        val cita = _uiState.value.cita ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = when (nuevoEstado) {
                    CitaDetalladaResponse.Estado.CONFIRMADA -> reservaApi.confirmarReserva(cita.id)
                    CitaDetalladaResponse.Estado.EN_ATENCION -> gestionAgendaApi.iniciarAtencion(cita.id)
                    CitaDetalladaResponse.Estado.CANCELADA -> reservaApi.cancelarReservaPorStaff(cita.id)
                    CitaDetalladaResponse.Estado.FINALIZADA -> reservaApi.finalizarCita(cita.id)
                    CitaDetalladaResponse.Estado.NO_ASISTIO -> reservaApi.cancelarReservaPorStaff(cita.id)
                }
                if (response.isSuccessful) {
                    cargarCita(cita.id.toString())
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cambiar estado: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

