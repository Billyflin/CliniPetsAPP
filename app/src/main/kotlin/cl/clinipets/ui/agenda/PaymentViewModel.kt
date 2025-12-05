package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.openapi.models.CitaResponse
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
    val cita: CitaDetalladaResponse? = null, // Changed to CitaDetalladaResponse
    val isPaymentConfirmed: Boolean = false,
    val isPollingActive: Boolean = false
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val reservaApi: ReservaControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling(citaId: UUID) {
        if (pollingJob?.isActive == true) return // Evita iniciar múltiples trabajos de sondeo

        pollingJob = viewModelScope.launch {
            _uiState.update { it.copy(isPollingActive = true) }
            while (isActive) {
                try {
                    // Use the correct API method 'obtenerReserva'
                    val response = reservaApi.obtenerReserva(citaId)
                    if (response.isSuccessful) {
                        val cita = response.body()
                        _uiState.update { it.copy(cita = cita) }

                        // Use the correct Enum 'CitaDetalladaResponse.Estado.CONFIRMADA'
                        if (cita?.estado == CitaDetalladaResponse.Estado.CONFIRMADA) {
                            _uiState.update { it.copy(isPaymentConfirmed = true, isPollingActive = false) }
                            stopPolling() // Detener el sondeo una vez confirmado
                        }
                    } else {
                        // Manejar error de la API, quizás con un backoff exponencial o simplemente registrándolo
                        _uiState.update { it.copy(error = "Error al verificar el estado del pago.") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error de red al verificar el pago: ${e.message}") }
                    // Podrías querer detener el sondeo aquí si el error es irrecuperable
                }
                delay(3000) // Esperar 3 segundos antes de la siguiente verificación
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

