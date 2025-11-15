package cl.clinipets.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DescubrimientoApi
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.DiscoveryRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Procedimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val mascotasApi: MascotasApi,
    private val veterinariosApi: VeterinariosApi,
    private val descubrimientoApi: DescubrimientoApi
) : ViewModel() {

    data class VeterinarioEncontrado(
        val id: UUID,
        val nombreCompleto: String,
        val latitud: Double,
        val longitud: Double,
        val distanciaKm: Double?,
        val precio: Int?,
        val modosAtencion: List<DiscoveryRequest.ModoAtencion>
    )

    data class UiState(
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val mascotas: List<Mascota> = emptyList(),
        val procedimientos: List<Procedimiento> = emptyList(),
        val resultadosRaw: String? = null,
        val resultados: List<VeterinarioEncontrado> = emptyList(),
        val mascotaSeleccionadaId: UUID? = null,
        val procedimientoSeleccionadoSku: String? = null,
        val modoAtencion: DiscoveryRequest.ModoAtencion = DiscoveryRequest.ModoAtencion.DOMICILIO,
        val latitud: Double? = null,
        val longitud: Double? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun setMascotaSeleccionada(id: UUID?) { _ui.update { it.copy(mascotaSeleccionadaId = id) } }
    fun setProcedimientoSeleccionado(sku: String?) { _ui.update { it.copy(procedimientoSeleccionadoSku = sku) } }
    fun setModoAtencion(modo: DiscoveryRequest.ModoAtencion) { _ui.update { it.copy(modoAtencion = modo) } }
    fun setUbicacionUsuario(lat: Double?, lng: Double?) {
        _ui.update { it.copy(latitud = lat, longitud = lng) }
    }

    /** Carga mascotas y procedimientos. */
    fun loadFormData(forceRefresh: Boolean = false) {
        val current = _ui.value
        if (!forceRefresh && current.mascotas.isNotEmpty() && current.procedimientos.isNotEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val mascotasResp = mascotasApi.listarMisMascotas()
                val procedimientosResp = veterinariosApi.listarProcedimientos()

                val mascotas = mascotasResp.takeBodyOrThrow("Error listando mascotas")
                val procedimientos = procedimientosResp.takeBodyOrThrow("Error listando procedimientos")

                _ui.update { it.copy(isLoading = false, mascotas = mascotas, procedimientos = procedimientos) }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar datos") }
            }
        }
    }

    private fun mapAnyToResultados(any: Any?): List<VeterinarioEncontrado> {
        if (any !is List<*>) return emptyList()
        return any.mapNotNull { elem ->
            val m = elem as? Map<*, *> ?: return@mapNotNull null
            try {
                val id = (m["id"] as? String)?.let(UUID::fromString) ?: return@mapNotNull null
                val nombre = (m["nombreCompleto"] as? String) ?: ""
                val lat = when (val v = m["latitud"]) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull()
                    else -> null
                } ?: return@mapNotNull null
                val lng = when (val v = m["longitud"]) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull()
                    else -> null
                } ?: return@mapNotNull null
                val dist = when (val v = m["distanciaKm"]) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull()
                    else -> null
                }
                val precio = when (val v = m["precio"]) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull()
                    else -> null
                }
                val modos = (m["modosAtencion"] as? List<*>)?.mapNotNull { s ->
                    (s as? String)?.let {
                        runCatching { DiscoveryRequest.ModoAtencion.valueOf(it) }.getOrNull()
                    }
                } ?: emptyList()
                VeterinarioEncontrado(
                    id = id,
                    nombreCompleto = nombre,
                    latitud = lat,
                    longitud = lng,
                    distanciaKm = dist,
                    precio = precio,
                    modosAtencion = modos
                )
            } catch (_: Throwable) { null }
        }
    }

    fun buscar() {
        val s = _ui.value
        val mascotaId = s.mascotaSeleccionadaId
        val sku = s.procedimientoSeleccionadoSku
        val lat = s.latitud
        val lng = s.longitud

        if (mascotaId == null || sku.isNullOrBlank()) {
            _ui.update { it.copy(error = "Selecciona mascota y procedimiento") }
            return
        }

        if (lat == null || lng == null) {
            _ui.update { it.copy(error = "No se pudo obtener tu ubicación. Inténtalo nuevamente.") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true, error = null, successMessage = null) }
            try {
                val req = DiscoveryRequest(
                    mascotaId = mascotaId,
                    servicioSku = sku,
                    modoAtencion = s.modoAtencion,
                    latitud = lat,
                    longitud = lng
                )
                val resp = descubrimientoApi.buscarDescubrimiento(req)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val list = mapAnyToResultados(body)
                    val text = when (body) {
                        null -> "Sin resultados"
                        is String -> body
                        else -> body.toString()
                    }
                    _ui.update { it.copy(isSubmitting = false, resultados = list, resultadosRaw = text, successMessage = "Búsqueda completada") }
                } else {
                    _ui.update { it.copy(isSubmitting = false, error = "Error en búsqueda (${resp.code()})") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isSubmitting = false, error = e.message ?: "Error en búsqueda") }
            }
        }
    }

    fun clearStatus() { _ui.update { it.copy(error = null, successMessage = null) } }

    // Extensiones para responses
    private fun <T> Response<List<T>>.takeBodyOrThrow(defaultMessage: String): List<T> {
        return if (isSuccessful) body().orEmpty() else {
            val msg = errorBody()?.string()?.takeIf { it.isNotBlank() } ?: defaultMessage
            throw IllegalStateException(msg)
        }
    }
}
