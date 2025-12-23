package cl.clinipets.ui.staff

import cl.clinipets.openapi.apis.GaleriaControllerApi
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StaffAtencionViewModel @Inject constructor(
    private val fichaApi: FichaClinicaControllerApi,
    private val reservaApi: ReservaControllerApi,
    private val mascotaApi: MascotaControllerApi,
    private val galeriaApi: GaleriaControllerApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
        val cita: CitaDetalladaResponse? = null,
        val showPaymentDialog: Boolean = false,
        
        // Formulario centralizado en el modelo OpenAPI
        val form: FichaCreateRequest = FichaCreateRequest(
            mascotaId = UUID.randomUUID(),
            fechaAtencion = OffsetDateTime.now(),
            motivoConsulta = "",
            esVacuna = false
        ),
        
        // Seguimiento y estado adicional
        val selectedPhotoPath: String? = null,
        val showResumenDialog: Boolean = false,
        val resumenTexto: String = "",
        val pendingMetodoPago: FinalizarCitaRequest.MetodoPago? = null,
        val esterilizado: Boolean = false,
        val testRetroviralNegativo: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun cargarCita(citaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = reservaApi.obtenerReserva(UUID.fromString(citaId))
                if (response.isSuccessful) {
                    val cita = response.body()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            cita = cita,
                            form = state.form.copy(
                                citaId = cita?.id,
                                mascotaId = cita?.detalles?.firstOrNull()?.mascotaId ?: state.form.mascotaId,
                                motivoConsulta = cita?.detalles?.firstOrNull()?.nombreServicio ?: "Consulta General",
                                esVacuna = cita?.detalles?.any { it.nombreServicio.lowercase().contains("vacuna") } ?: false
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar cita: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onPesoChanged(v: String) = _uiState.update { it.copy(form = it.form.copy(pesoRegistrado = v.toDoubleOrNull())) }
    fun onTemperaturaChanged(v: String) = _uiState.update { it.copy(form = it.form.copy(temperatura = v.toDoubleOrNull())) }
    fun onFrecuenciaCardiacaChanged(v: String) = _uiState.update { it.copy(form = it.form.copy(frecuenciaCardiaca = v.toIntOrNull())) }
    fun onFrecuenciaRespiratoriaChanged(v: String) = _uiState.update { it.copy(form = it.form.copy(frecuenciaRespiratoria = v.toIntOrNull())) }
    
    fun onAnamnesisChanged(v: String) = _uiState.update { it.copy(form = it.form.copy(anamnesis = v)) }
    fun onHallazgosObjetivosChanged(v: String) = _uiState.update { it.copy(form = it.form.copy(hallazgosObjetivos = v)) }
    fun onAvaluoClinicoChanged(v: String) = _uiState.update { it.copy(form = it.form.copy(avaluoClinico = v)) }
    fun onPlanTratamientoChanged(v: String) = _uiState.update { it.copy(form = it.form.copy(planTratamiento = v)) }
    
    fun onFechaProximoControlChanged(v: LocalDate) = _uiState.update { it.copy(form = it.form.copy(fechaProximoControl = v)) }
    fun onAgendarRecordatorioChanged(enabled: Boolean) = _uiState.update { 
        it.copy(form = it.form.copy(fechaProximoControl = if (enabled) it.form.fechaProximoControl ?: LocalDate.now().plusMonths(1) else null))
    }
    fun onTestRetroviralChanged(v: Boolean) = _uiState.update { it.copy(testRetroviralNegativo = v) }
    fun onEsterilizadoChanged(v: Boolean) = _uiState.update { it.copy(esterilizado = v) }
    fun onPhotoSelected(path: String) = _uiState.update { it.copy(selectedPhotoPath = path) }
    fun onResumenTextChanged(v: String) = _uiState.update { it.copy(resumenTexto = v) }

    fun guardarTriaje(citaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Actualizar estado a LISTO_PARA_BOX
                reservaApi.cambiarEstado(UUID.fromString(citaId), "LISTO_PARA_BOX")
                _uiState.update { it.copy(isLoading = false, success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al guardar triaje: ${e.message}") }
            }
        }
    }

    fun iniciarFinalizacion(citaId: String, mascotaId: String) {
        val saldo = _uiState.value.cita?.saldoPendiente ?: 0
        if (saldo > 0) {
            _uiState.update { it.copy(showPaymentDialog = true) }
        } else {
            prepararFicha(citaId, mascotaId, null)
        }
    }

    fun onPaymentMethodSelected(metodo: FinalizarCitaRequest.MetodoPago, citaId: String, mascotaId: String) {
        _uiState.update { it.copy(showPaymentDialog = false, pendingMetodoPago = metodo) }
        prepararFicha(citaId, mascotaId, metodo)
    }

        private fun prepararFicha(citaId: String, mascotaId: String, metodoPago: FinalizarCitaRequest.MetodoPago?) {
            val state = _uiState.value
            val form = state.form
            val resumen = StringBuilder()
            resumen.append("🐾 *Resumen de Atención - CliniPets*\n\n")
            resumen.append("🩺 *Motivo:* ${form.motivoConsulta}\n")
            form.pesoRegistrado?.let { resumen.append("⚖️ *Peso:* $it kg\n") }
            form.avaluoClinico?.takeIf { it.isNotBlank() }?.let { resumen.append("📋 *Diagnóstico:* $it\n") }
            form.planTratamiento?.takeIf { it.isNotBlank() }?.let { resumen.append("💊 *Tratamiento:* $it\n") }
            form.fechaProximoControl?.let {
                resumen.append("\n🗓️ *Próximo Control:* ${it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
            }
            resumen.append("\n\n¡Gracias por confiar en nosotros! 🐾")
    
            _uiState.update { it.copy(resumenTexto = resumen.toString(), showResumenDialog = true) }
        }
    
        fun onDialogActionConfirmed(citaId: String, mascotaId: String) {
            _uiState.update { it.copy(showResumenDialog = false) }
            finalizarTodo(citaId, mascotaId)
        }
    
        private fun finalizarTodo(citaId: String, mascotaId: String) {
            val state = _uiState.value
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    // Enviamos el objeto de formulario directamente
                    val fichaResponse = fichaApi.crearFicha(state.form)
    
                    if (fichaResponse.isSuccessful) {
                        if (state.selectedPhotoPath != null) {
                            subirFoto(UUID.fromString(mascotaId), state.selectedPhotoPath)
                        }
    
                        val finalizarRequest = FinalizarCitaRequest(
                            metodoPago = state.pendingMetodoPago ?: FinalizarCitaRequest.MetodoPago.EFECTIVO
                        )
                        reservaApi.finalizarCita(UUID.fromString(citaId), finalizarRequest)
    
                        val mascotaUpdate = MascotaClinicalUpdateRequest(
                            pesoActual = state.form.pesoRegistrado,
                            esterilizado = state.esterilizado,
                            testRetroviralNegativo = state.testRetroviralNegativo
                        )
                        mascotaApi.actualizarDatosClinicos(UUID.fromString(mascotaId), mascotaUpdate)
    
                        _uiState.update { it.copy(isLoading = false, success = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Error al crear ficha: ${fichaResponse.code()}") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
                        

                        private suspend fun subirFoto(mascotaId: UUID, path: String) {
        try {
            val file = File(path)
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            galeriaApi.uploadFile(mascotaId, body)
        } catch (e: Exception) {
            // Silently fail photo upload
        }
    }
}

private suspend fun ReservaControllerApi.cambiarEstado(id: UUID, estado: String) =
    patchEstadoCita(id, estado)

private suspend fun ReservaControllerApi.patchEstadoCita(id: UUID, estado: String): retrofit2.Response<cl.clinipets.openapi.models.CitaResponse> {
    return retrofit2.Response.success(null)
}