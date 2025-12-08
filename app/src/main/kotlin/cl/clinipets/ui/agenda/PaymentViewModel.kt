package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.notifications.PaymentStatusManager
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PaymentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cita: CitaDetalladaResponse? = null,
    val isPaymentConfirmed: Boolean = false,
    val isPollingActive: Boolean = false
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val reservaApi: ReservaControllerApi,
    private val paymentStatusManager: PaymentStatusManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var currentCitaId: UUID? = null

    init {
        viewModelScope.launch {
            paymentStatusManager.paymentConfirmedFlow.collect { confirmedId ->
                if (currentCitaId != null && currentCitaId.toString() == confirmedId) {
                    _uiState.update { it.copy(isPaymentConfirmed = true, isPollingActive = false) }
                    stopPolling()
                }
            }
        }
    }

    fun startPolling(citaId: UUID) {
        currentCitaId = citaId
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            _uiState.update { it.copy(isPollingActive = true) }
            while (isActive) {
                try {
                    val response = reservaApi.obtenerReserva(citaId)
                    if (response.isSuccessful) {
                        val cita = response.body()
                        _uiState.update { it.copy(cita = cita) }

                        if (cita?.estado == CitaDetalladaResponse.Estado.CONFIRMADA) {
                            _uiState.update { it.copy(isPaymentConfirmed = true, isPollingActive = false) }
                            stopPolling()
                        }
                    } else {
                        _uiState.update { it.copy(error = "Error al verificar el estado del pago.") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error de red al verificar el pago: ${e.message}") }
                }
                delay(5000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        _uiState.update { it.copy(isPollingActive = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}