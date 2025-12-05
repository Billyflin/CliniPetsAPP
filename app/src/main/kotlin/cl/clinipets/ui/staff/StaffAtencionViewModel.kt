package cl.clinipets.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.FichaClinicaControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.FichaCreateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StaffAtencionViewModel @Inject constructor(
    private val fichaApi: FichaClinicaControllerApi,
    private val reservaApi: ReservaControllerApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
        
        // Campos del formulario
        val peso: String = "",
        val anamnesis: String = "",
        val examenFisico: String = "",
        val diagnostico: String = "",
        val tratamiento: String = "",
        
        // Seguimiento
        val agendarRecordatorio: Boolean = false,
        val fechaProximoControl: LocalDate? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun onPesoChanged(value: String) {
        _uiState.update { it.copy(peso = value) }
    }

    fun onAnamnesisChanged(value: String) {
        _uiState.update { it.copy(anamnesis = value) }
    }

    fun onExamenFisicoChanged(value: String) {
        _uiState.update { it.copy(examenFisico = value) }
    }

    fun onDiagnosticoChanged(value: String) {
        _uiState.update { it.copy(diagnostico = value) }
    }

    fun onTratamientoChanged(value: String) {
        _uiState.update { it.copy(tratamiento = value) }
    }

    fun onAgendarRecordatorioChanged(checked: Boolean) {
        _uiState.update { it.copy(agendarRecordatorio = checked) }
    }

    fun onFechaProximoControlChanged(date: LocalDate?) {
        _uiState.update { it.copy(fechaProximoControl = date) }
    }

    fun guardarFicha(citaId: String, mascotaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _uiState.value
                
                // Validaciones básicas
                if (state.peso.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, error = "El peso es obligatorio") }
                    return@launch
                }

                val pesoVal = state.peso.toDoubleOrNull()
                if (pesoVal == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Peso inválido") }
                    return@launch
                }

                val request = FichaCreateRequest(
                    mascotaId = UUID.fromString(mascotaId),
                    fechaAtencion = OffsetDateTime.now(),
                    motivoConsulta = "Atención Staff App", // Podríamos sacar esto de la cita si tuvieramos el detalle aquí
                    esVacuna = false, // Por ahora asumimos consulta general
                    pesoRegistrado = pesoVal,
                    anamnesis = state.anamnesis.takeIf { it.isNotBlank() },
                    examenFisico = state.examenFisico.takeIf { it.isNotBlank() },
                    diagnostico = state.diagnostico.takeIf { it.isNotBlank() },
                    tratamiento = state.tratamiento.takeIf { it.isNotBlank() },
                    fechaProximoControl = if (state.agendarRecordatorio) state.fechaProximoControl else null
                )

                val fichaResponse = fichaApi.crearFicha(request)
                
                if (fichaResponse.isSuccessful) {
                    // Si se creó la ficha, confirmamos/finalizamos la reserva
                    val reservaResponse = reservaApi.confirmarReserva(UUID.fromString(citaId))
                    if (reservaResponse.isSuccessful) {
                        _uiState.update { it.copy(isLoading = false, success = true) }
                    } else {
                         _uiState.update { it.copy(isLoading = false, error = "Ficha guardada, pero error al actualizar reserva: ${reservaResponse.code()}") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al guardar ficha: ${fichaResponse.code()}") }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }
}
