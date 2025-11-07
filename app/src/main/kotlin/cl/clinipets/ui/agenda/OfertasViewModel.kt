package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.AgendaApi
import cl.clinipets.openapi.models.CrearOfertaRequest
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OfertasViewModel @Inject constructor(
    private val agendaApi: AgendaApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val solicitudesDisponibles: List<SolicitudDisponibleUi> = emptyList(),
        val error: String? = null,
        val successMessage: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val gson = Gson()

    fun clearStatus() {
        _ui.update { it.copy(error = null, successMessage = null) }
    }

    fun refrescarSolicitudes() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.getSolicitudesDisponibles()
                if (resp.isSuccessful) {
                    val parsed = parseSolicitudes(resp.body())
                    _ui.update { it.copy(isLoading = false, solicitudesDisponibles = parsed) }
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: resp.message().ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.message ?: "Error de red") }
            }
        }
    }

    fun enviarOferta(
        solicitudId: String,
        request: CrearOfertaRequest,
        onSuccess: (() -> Unit)? = null
    ) {
        val uuid = runCatching { UUID.fromString(solicitudId) }.getOrNull()
            ?: return _ui.update { it.copy(error = "ID de solicitud inválido") }

        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, error = null, successMessage = null) }
            try {
                val resp = agendaApi.crearOferta(uuid, request)
                if (resp.isSuccessful) {
                    _ui.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Oferta enviada",
                            error = null
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: resp.message().ifBlank { "Error ${resp.code()}" }
                    _ui.update { it.copy(isSubmitting = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isSubmitting = false, error = e.message ?: "No fue posible enviar la oferta") }
            }
        }
    }

    private fun parseSolicitudes(body: Any?): List<SolicitudDisponibleUi> {
        if (body == null) return emptyList()
        val element = gson.toJsonTree(body)
        return when {
            element.isJsonArray -> mapArray(element.asJsonArray)
            element.isJsonObject -> {
                val obj = element.asJsonObject
                when {
                    obj.get("items")?.isJsonArray == true -> mapArray(obj.getAsJsonArray("items"))
                    else -> listOfNotNull(mapSolicitud(obj))
                }
            }
            else -> emptyList()
        }
    }

    private fun mapArray(array: JsonArray): List<SolicitudDisponibleUi> =
        array.mapNotNull { it.asJsonObjectOrNull()?.let(::mapSolicitud) }

    private fun mapSolicitud(obj: JsonObject): SolicitudDisponibleUi? {
        val id = obj.get("id")?.asString ?: return null
        val mascota = obj.getAsJsonObject("mascota")
        val procedimiento = obj.getAsJsonObject("procedimiento")
        val cliente = obj.getAsJsonObject("cliente")
        return SolicitudDisponibleUi(
            id = id,
            mascotaNombre = mascota?.get("nombre")?.asString,
            mascotaEspecie = mascota?.get("especie")?.asString,
            clienteNombre = cliente?.get("nombre")?.asString,
            procedimientoNombre = procedimiento?.get("nombre")?.asString,
            procedimientoSku = procedimiento?.get("sku")?.asString,
            modoAtencion = obj.get("modoAtencion")?.asString,
            preferenciaLogistica = obj.get("preferenciaLogistica")?.asString,
            bloqueSolicitado = obj.get("bloqueSolicitado")?.asString,
            fecha = obj.get("fecha")?.asString,
            latitud = obj.get("latitud")?.asDoubleOrNull(),
            longitud = obj.get("longitud")?.asDoubleOrNull(),
            raw = obj
        )
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        if (this.isJsonObject) this.asJsonObject else null

    private fun JsonElement.asDoubleOrNull(): Double? =
        runCatching { if (this.isJsonPrimitive) this.asDouble else null }.getOrNull()
}

data class SolicitudDisponibleUi(
    val id: String,
    val mascotaNombre: String?,
    val mascotaEspecie: String?,
    val clienteNombre: String?,
    val procedimientoNombre: String?,
    val procedimientoSku: String?,
    val modoAtencion: String?,
    val preferenciaLogistica: String?,
    val bloqueSolicitado: String?,
    val fecha: String?,
    val latitud: Double?,
    val longitud: Double?,
    val raw: JsonObject
)
