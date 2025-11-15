package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.AgendaControllerApi
import cl.clinipets.openapi.apis.HorariosControllerApi
import cl.clinipets.openapi.models.DiscoveryRequest
import cl.clinipets.openapi.models.Intervalo
import cl.clinipets.openapi.models.ReservaCreateDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReservaViewModel @Inject constructor(
    private val agendaApi: AgendaControllerApi,
    private val horariosApi: HorariosControllerApi
) : ViewModel() {
    data class Ui(
        val isSubmitting: Boolean = false,
        val isLoadingSlots: Boolean = false,
        val error: String? = null,
        val fecha: String = LocalDate.now().toString(),
        val horaInicio: String = "09:00",
        val direccion: String = "",
        val referencias: String = "",
        val modo: DiscoveryRequest.ModoAtencion = DiscoveryRequest.ModoAtencion.DOMICILIO,
        val procedimientoSku: String = "",
        val lat: Double? = null,
        val lng: Double? = null,
        val veterinarioId: UUID? = null,
        val slots: List<Intervalo> = emptyList(),
        val usarMiUbicacion: Boolean = true
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui

    fun init(
        mascotaId: UUID,
        procedimientoSku: String,
        modo: DiscoveryRequest.ModoAtencion,
        lat: Double?,
        lng: Double?,
        veterinarioId: UUID?,
    ) {
        _ui.value = _ui.value.copy(
            procedimientoSku = procedimientoSku,
            modo = modo,
            lat = lat,
            lng = lng,
            veterinarioId = veterinarioId
        )
        cargarSlots() // carga inicial de slots para la fecha por defecto
    }

    fun setFecha(v: String) {
        _ui.value = _ui.value.copy(fecha = v)
        cargarSlots()
    }
    fun setHoraInicio(v: String) { _ui.value = _ui.value.copy(horaInicio = v) }
    fun setDireccion(v: String) { _ui.value = _ui.value.copy(direccion = v) }
    fun setReferencias(v: String) { _ui.value = _ui.value.copy(referencias = v) }
    fun setUsarMiUbicacion(v: Boolean) {
        _ui.value = _ui.value.copy(usarMiUbicacion = v)
    }

    fun setLatLng(lat: Double?, lng: Double?) {
        _ui.value = _ui.value.copy(lat = lat, lng = lng)
    }

    fun cargarSlots() {
        val s = _ui.value
        val fecha = runCatching { LocalDate.parse(s.fecha) }.getOrNull() ?: return
        viewModelScope.launch {
            _ui.value = s.copy(isLoadingSlots = true, error = null)
            try {
                val (tipo, ownerId) = when (s.modo) {
                    DiscoveryRequest.ModoAtencion.CLINICA ->
                        HorariosControllerApi.TipoSlots.CLINICA to null
                    DiscoveryRequest.ModoAtencion.DOMICILIO, DiscoveryRequest.ModoAtencion.URGENCIA -> {
                        val vetId = s.veterinarioId
                        if (vetId == null) {
                            _ui.value = s.copy(
                                isLoadingSlots = false,
                                error = "Selecciona un veterinario desde el descubrimiento para ${s.modo}"
                            )
                            return@launch
                        }
                        HorariosControllerApi.TipoSlots.VETERINARIO to vetId
                    }
                }

                val resp = horariosApi.slots(tipo, fecha, s.procedimientoSku, ownerId)
                if (resp.isSuccessful) {
                    val slots = resp.body().orEmpty()
                    _ui.value = _ui.value.copy(isLoadingSlots = false, slots = slots)
                    // Si la hora seleccionada ya no está, limpiar selección
                    if (slots.none { it.inicio == _ui.value.horaInicio }) {
                        _ui.value = _ui.value.copy(
                            horaInicio = slots.firstOrNull()?.inicio ?: _ui.value.horaInicio
                        )
                    }
                } else {
                    _ui.value = s.copy(
                        isLoadingSlots = false,
                        error = "No se pudieron cargar slots (${resp.code()})"
                    )
                }
            } catch (e: Exception) {
                _ui.value = s.copy(isLoadingSlots = false, error = e.message)
            }
        }
    }

    fun seleccionarSlot(slot: Intervalo) {
        _ui.value = _ui.value.copy(horaInicio = slot.inicio)
    }

    fun crearReserva(onDone: () -> Unit) {
        val s = _ui.value

        val modoDto = try {
            ReservaCreateDTO.ModoAtencion.valueOf(s.modo.name)
        } catch (_: IllegalArgumentException) {
            ReservaCreateDTO.ModoAtencion.DOMICILIO
        }

        val dto = ReservaCreateDTO(
            procedimientoSku = s.procedimientoSku,
            modoAtencion = modoDto,
            fecha = LocalDate.parse(s.fecha),
            horaInicio = s.horaInicio,
            veterinarioId = s.veterinarioId,
            direccionTexto = s.direccion.ifBlank { null },
            lat = s.lat,
            lng = s.lng,
            referencias = s.referencias.ifBlank { null }
        )

        viewModelScope.launch {
            _ui.value = s.copy(isSubmitting = true, error = null)
            try {
                val resp = agendaApi.crearReserva(dto)
                if (resp.isSuccessful) {
                    _ui.value = s.copy(isSubmitting = false)
                    onDone()
                } else {
                    _ui.value = s.copy(
                        isSubmitting = false,
                        error = "Error creando reserva (${resp.code()})"
                    )
                }
            } catch (e: Exception) {
                _ui.value = s.copy(isSubmitting = false, error = e.message)
            }
        }
    }
}