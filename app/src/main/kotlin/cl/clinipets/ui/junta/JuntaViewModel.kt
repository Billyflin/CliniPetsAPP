package cl.clinipets.ui.junta

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.service.LocationShareService
import cl.clinipets.core.session.SessionManager
import cl.clinipets.core.ws.StompClient
import cl.clinipets.openapi.apis.AgendaControllerApi
import cl.clinipets.openapi.models.Reserva
import cl.clinipets.openapi.models.ReservaAccionRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class JuntaViewModel @Inject constructor(
    private val stomp: StompClient,
    private val session: SessionManager,
    private val fused: FusedLocationProviderClient,
    private val api : AgendaControllerApi
) : ViewModel() {

    data class VetPos(val lat: Double, val lng: Double, val speed: Double?, val heading: Double?, val ts: Long?)

    data class UiState(
        val reservaId: UUID? = null,
        val conectado: Boolean = false,
        val ultimasPosiciones: List<VetPos> = emptyList(),
        val error: String? = null,
        val isVet: Boolean = false,
        val shareEnabled: Boolean = false,
        val reserva: Reserva? = null,
        val loadingReserva: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var locationCallback: LocationCallback? = null

    fun iniciar(reservaId: UUID) {
        if (_ui.value.reservaId == reservaId && _ui.value.conectado && _ui.value.reserva != null) return
        viewModelScope.launch {
            val roles = session.rolesFlow.first()
            val isVet = roles.contains("VETERINARIO")
            val token = session.tokenFlow.first()
            val headers = token?.takeIf { it.isNotBlank() }?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
            _ui.value = UiState(reservaId = reservaId, isVet = isVet, shareEnabled = isVet, loadingReserva = true)
            cargarReserva(reservaId)
            stomp.connect(headers)
            subscribe(reservaId)
            observarEventos()
            if (isVet && _ui.value.shareEnabled) startLocationUpdates()
        }
    }

    fun refreshReserva() {
        val id = _ui.value.reservaId ?: return
        cargarReserva(id)
    }

    private fun cargarReserva(id: UUID) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loadingReserva = true, error = null)
            runCatching {
                api.obtenerInfo(ReservaAccionRequest(id))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _ui.value = _ui.value.copy(reserva = resp.body(), loadingReserva = false)
                } else {
                    _ui.value = _ui.value.copy(error = "Error ${resp.code()} obteniendo reserva", loadingReserva = false)
                }
            }.onFailure { e ->
                _ui.value = _ui.value.copy(error = e.message, loadingReserva = false)
            }
        }
    }

    fun setShareLocation(enabled: Boolean) {
        val prev = _ui.value.shareEnabled
        if (prev == enabled) return
        _ui.value = _ui.value.copy(shareEnabled = enabled)
        if (_ui.value.isVet) {
            if (enabled) startLocationUpdates() else stopLocationUpdates()
        }
    }

    private fun subscribe(reservaId: UUID) { stomp.subscribe("/topic/junta/${reservaId}") }

    private fun observarEventos() {
        viewModelScope.launch {
            stomp.events().collect { ev ->
                when (ev) {
                    is StompClient.StompEvent.Open -> _ui.value = _ui.value.copy(conectado = true)
                    is StompClient.StompEvent.Message -> handleMessage(ev.body)
                    is StompClient.StompEvent.Error -> _ui.value = _ui.value.copy(error = ev.t.message)
                    is StompClient.StompEvent.Closed -> _ui.value = _ui.value.copy(conectado = false)
                }
            }
        }
    }

    private fun handleMessage(body: String) {
        runCatching {
            val json = JSONObject(body)
            val pos = VetPos(
                lat = json.optDouble("lat"),
                lng = json.optDouble("lng"),
                speed = json.optDouble("speed").takeIf { json.has("speed") },
                heading = json.optDouble("heading").takeIf { json.has("heading") },
                ts = json.optLong("ts").takeIf { json.has("ts") }
            )
            _ui.value = _ui.value.copy(
                ultimasPosiciones = (listOf(pos) + _ui.value.ultimasPosiciones).take(50)
            )
        }.onFailure { e ->
            _ui.value = _ui.value.copy(error = e.message)
        }
    }

    private fun startLocationUpdates() {
        if (!_ui.value.isVet || !_ui.value.shareEnabled) return
        val req = LocationRequest.Builder(1000L)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val speedMs = loc.speed
                val speedKmh: Double? = if (speedMs.isNaN()) null else (speedMs * 3.6)
                val bearing = loc.bearing
                val heading: Double? = if (bearing.isNaN()) null else bearing.toDouble()
                enviarPosicion(loc.latitude, loc.longitude, speed = speedKmh, heading = heading)
            }
        }
        locationCallback = cb
        try {
            fused.requestLocationUpdates(req, cb, android.os.Looper.getMainLooper())
        } catch (_: SecurityException) {
            _ui.value = _ui.value.copy(error = "Permiso de ubicación no concedido")
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
    }

    fun enviarPosicion(lat: Double, lng: Double, speed: Double? = null, heading: Double? = null) {
        val reservaId = _ui.value.reservaId ?: return
        val payload = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            speed?.let { put("speed", it) }
            heading?.let { put("heading", it) }
            put("ts", System.currentTimeMillis())
        }.toString()
        Log.d("JuntaViewModel", "Payload: $payload")
        stomp.send("/app/junta/${reservaId}/pos", payload)
        // Actualizar localmente para reflejar inmediatamente la posición propia
        val selfPos = VetPos(lat = lat, lng = lng, speed = speed, heading = heading, ts = System.currentTimeMillis())
        _ui.update { it.copy(ultimasPosiciones = (listOf(selfPos) + it.ultimasPosiciones).take(50)) }
    }

    fun obtenerMiUbicacion(onResult: (Double, Double) -> Unit) {
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    onResult(loc.latitude, loc.longitude)
                } else {
                    _ui.update { it.copy(error = "No se pudo obtener ubicación actual") }
                }
            }.addOnFailureListener { t ->
                _ui.update { it.copy(error = t.message ?: "Error obteniendo ubicación") }
            }
        } catch (_: SecurityException) {
            _ui.update { it.copy(error = "Permisos de ubicación no concedidos") }
        }
    }

    fun iniciarServicioFondo(context: android.content.Context) {
        val id = _ui.value.reservaId ?: return
        viewModelScope.launch {
            val token = session.tokenFlow.first()
            val intent = Intent(context, LocationShareService::class.java).apply {
                putExtra(LocationShareService.EXTRA_RESERVA_ID, id.toString())
                putExtra(LocationShareService.EXTRA_TOKEN, token)
            }
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "No se pudo iniciar servicio") }
            }
        }
    }

    fun detenerServicioFondo(context: android.content.Context) {
        val intent = Intent(context, LocationShareService::class.java)
        try {
            context.stopService(intent)
        } catch (e: Exception) {
            _ui.update { it.copy(error = e.message ?: "No se pudo detener servicio") }
        }
    }

    override fun onCleared() {
        stopLocationUpdates()
        stomp.disconnect()
        super.onCleared()
    }
}
