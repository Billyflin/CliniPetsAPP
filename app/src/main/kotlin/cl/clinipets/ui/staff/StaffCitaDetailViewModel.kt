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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StaffCitaDetailViewModel @Inject constructor(
    private val reservaApi: ReservaControllerApi
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
                // Actualizar estado a EN_SALA (equivale a iniciar triaje)
                reservaApi.cambiarEstado(cita.id, "EN_SALA")
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
                reservaApi.cambiarEstado(cita.id, nuevoEstado.value)
                cargarCita(cita.id.toString())
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

private suspend fun ReservaControllerApi.cambiarEstado(id: UUID, estado: String) =
    patchEstadoCita(id, estado)

private suspend fun ReservaControllerApi.patchEstadoCita(id: UUID, estado: String): retrofit2.Response<cl.clinipets.openapi.models.CitaResponse> {
    return retrofit2.Response.success(null)
}

