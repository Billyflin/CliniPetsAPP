package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MyReservationsViewModel @Inject constructor(
    private val reservaApi: ReservaControllerApi
) : ViewModel() {
    private val _reservas = MutableStateFlow<List<CitaDetalladaResponse>>(emptyList())
    val reservas = _reservas.asStateFlow()

    // Estado de carga general de la pantalla
    val isLoading = MutableStateFlow(false)

    // Estado de acción en progreso por item (por ahora un único ID)
    val actionInProgressId = MutableStateFlow<UUID?>(null)

    // Mensaje de error simple para mostrar en UI
    val errorMessage = MutableStateFlow<String?>(null)

    init {
        loadReservas()
    }

    fun loadReservas() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = reservaApi.listarReservas()
                if (response.isSuccessful) {
                    _reservas.value = response.body() ?: emptyList()
                } else {
                    errorMessage.value = "Error al cargar reservas: ${response.code()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage.value = e.message ?: "Error desconocido al cargar reservas"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadReservas()
    }

    fun cancelReservation(citaId: UUID) {
        viewModelScope.launch {
            // Marcamos la acción en progreso para este ID
            actionInProgressId.value = citaId
            try {
                val response = reservaApi.cancelarReserva(citaId)
                if (response.isSuccessful) {
                    // Tras cancelar, recargamos la lista para reflejar cambios
                    loadReservas()
                } else {
                    errorMessage.value = "Error al cancelar reserva: ${response.code()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage.value = e.message ?: "Error desconocido al cancelar"
            } finally {
                // Limpiamos la acción en progreso
                actionInProgressId.value = null
            }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }
}
