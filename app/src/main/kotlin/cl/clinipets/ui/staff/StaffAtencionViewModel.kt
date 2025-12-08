package cl.clinipets.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.FichaClinicaControllerApi
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.openapi.models.FichaCreateRequest
import cl.clinipets.openapi.models.FinalizarCitaRequest
import cl.clinipets.openapi.models.MascotaClinicalUpdateRequest
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
    private val reservaApi: ReservaControllerApi,
    private val mascotaApi: MascotaControllerApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
        val cita: CitaDetalladaResponse? = null,
        val showPaymentDialog: Boolean = false,
        val paymentLinkToShare: String? = null,
        
        // Campos del formulario
        val peso: String = "",
        val anamnesis: String = "",
        val examenFisico: String = "",
        val diagnostico: String = "",
        val tratamiento: String = "",
        
        // Seguimiento
        val agendarRecordatorio: Boolean = false,
        val fechaProximoControl: LocalDate? = null,
        
        // Actualización Carnet
        val testRetroviralNegativo: Boolean = false,
        val esterilizado: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun cargarCita(citaId: String) {
        viewModelScope.launch {
            try {
                val response = reservaApi.obtenerReserva(UUID.fromString(citaId))
                if (response.isSuccessful) {
                    val cita = response.body()
                    val mascotaId = cita?.detalles?.firstOrNull()?.mascotaId
                    
                    var testNegativo = false
                    var esEsterilizado = false

                    if (mascotaId != null) {
                        try {
                            val mascotaResponse = mascotaApi.obtenerMascota(mascotaId)
                            if (mascotaResponse.isSuccessful) {
                                val mascota = mascotaResponse.body()
                                testNegativo = mascota?.testRetroviralNegativo ?: false
                                esEsterilizado = mascota?.esterilizado ?: false
                            }
                        } catch (e: Exception) {
                            // Si falla la carga de mascota, seguimos con defaults
                        }
                    }

                    _uiState.update { 
                        it.copy(
                            cita = cita,
                            testRetroviralNegativo = testNegativo,
                            esterilizado = esEsterilizado
                        ) 
                    }
                }
            } catch (e: Exception) {
                // Silently fail or log
            }
        }
    }

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

    fun onTestRetroviralChanged(checked: Boolean) {
        _uiState.update { it.copy(testRetroviralNegativo = checked) }
    }

    fun onEsterilizadoChanged(checked: Boolean) {
        _uiState.update { it.copy(esterilizado = checked) }
    }

    fun iniciarFinalizacion() {
        val saldo = _uiState.value.cita?.saldoPendiente ?: 0
        if (saldo > 0) {
            _uiState.update { it.copy(showPaymentDialog = true) }
        } else {
            // Si no hay deuda, finalizamos directo
            val citaId = _uiState.value.cita?.id?.toString()
            val mascotaId = _uiState.value.cita?.detalles?.firstOrNull()?.mascotaId?.toString()
            
            if (citaId != null && mascotaId != null) {
                guardarFicha(citaId, mascotaId, null)
            }
        }
    }
    
    fun onPaymentMethodSelected(method: FinalizarCitaRequest.MetodoPago, citaId: String, mascotaId: String) {
        _uiState.update { it.copy(showPaymentDialog = false) }
        guardarFicha(citaId, mascotaId, method)
    }

    fun guardarFicha(citaId: String, mascotaId: String, metodoPago: FinalizarCitaRequest.MetodoPago? = null) {
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

                // Actualizar datos clínicos de la mascota (Test Retroviral, Esterilizado)
                try {
                    val clinicalUpdate = MascotaClinicalUpdateRequest(
                        testRetroviralNegativo = state.testRetroviralNegativo,
                        esterilizado = state.esterilizado
                    )
                    mascotaApi.actualizarDatosClinicos(UUID.fromString(mascotaId), clinicalUpdate)
                } catch (e: Exception) {
                    // Log error but continue with ficha creation? Or stop?
                    // For now, we continue but log implicitely via structure
                }

                val fichaResponse = fichaApi.crearFicha(request)
                
                if (fichaResponse.isSuccessful) {
                    // Si se creó la ficha, finalizamos la reserva con el método de pago
                    // Usamos EFECTIVO como fallback si es nulo (caso deuda 0 o cierre simple)
                    val metodoPagoSeguro = metodoPago ?: FinalizarCitaRequest.MetodoPago.EFECTIVO
                    val finalizarRequest = FinalizarCitaRequest(metodoPago = metodoPagoSeguro)
                    
                    try {
                        val reservaResponse = reservaApi.finalizarCita(UUID.fromString(citaId), finalizarRequest)
                        
                        if (reservaResponse.isSuccessful) {
                            val citaUpdated = reservaResponse.body()
                            if (metodoPago == FinalizarCitaRequest.MetodoPago.MERCADO_PAGO_LINK && !citaUpdated?.paymentUrl.isNullOrBlank()) {
                                // Caso especial: Mostrar link
                                _uiState.update { it.copy(isLoading = false, paymentLinkToShare = citaUpdated?.paymentUrl, success = false) }
                            } else {
                                // Caso normal: Cerrar
                                _uiState.update { it.copy(isLoading = false, success = true) }
                            }
                        } else {
                             _uiState.update { it.copy(isLoading = false, error = "Ficha guardada, pero error al finalizar cita: ${reservaResponse.code()}") }
                        }
                    } catch (e: IllegalArgumentException) {
                        _uiState.update { it.copy(isLoading = false, error = "Error de sincronización con el servidor: ${e.message}") }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false, error = "Error al finalizar cita: ${e.message}") }
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
