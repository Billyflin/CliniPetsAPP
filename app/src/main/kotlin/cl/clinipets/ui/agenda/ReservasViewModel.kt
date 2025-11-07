package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.AgendaApi
import cl.clinipets.openapi.models.ReservaDto
import cl.clinipets.openapi.models.ReservarSlotClinicaRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReservasViewModel @Inject constructor(
    private val agendaApi: AgendaApi
) : ViewModel() {

    data class UiState(
        val isWorking: Boolean = false,
        val reservasRecientes: List<ReservaDto> = emptyList(),
        val ultimaReserva: ReservaDto? = null,
        val successMessage: String? = null,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun clearStatus() {
        _ui.update { it.copy(successMessage = null, error = null) }
    }

    fun aceptarOferta(ofertaId: UUID, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _ui.update { it.copy(isWorking = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.aceptarOferta(ofertaId)
                if (resp.isSuccessful) {
                    val reservas = resp.body().orEmpty()
                    _ui.update {
                        it.copy(
                            isWorking = false,
                            reservasRecientes = reservas,
                            ultimaReserva = reservas.lastOrNull(),
                            successMessage = "Oferta aceptada"
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: resp.message().ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isWorking = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isWorking = false, error = e.message ?: "No fue posible aceptar la oferta") }
            }
        }
    }

    fun reservarSlotClinica(request: ReservarSlotClinicaRequest, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _ui.update { it.copy(isWorking = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.crearReservaClinica(request)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _ui.update {
                        it.copy(
                            isWorking = false,
                            ultimaReserva = body,
                            reservasRecientes = listOfNotNull(body),
                            successMessage = "Reserva creada"
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: resp.message().ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isWorking = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isWorking = false, error = e.message ?: "No fue posible reservar") }
            }
        }
    }

    fun confirmarReserva(reservaId: UUID, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _ui.update { it.copy(isWorking = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.confirmarReservaClinica(reservaId)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _ui.update {
                        it.copy(
                            isWorking = false,
                            ultimaReserva = body,
                            successMessage = "Reserva confirmada"
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: resp.message().ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isWorking = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isWorking = false, error = e.message ?: "No fue posible confirmar la reserva") }
            }
        }
    }
}
