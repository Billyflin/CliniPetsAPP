package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.AgendaControllerApi
import cl.clinipets.openapi.models.MisReservasQuery
import cl.clinipets.openapi.models.Reserva
import cl.clinipets.openapi.models.ReservaAccionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AgendaGestionViewModel @Inject constructor(
    private val agendaApi: AgendaControllerApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val reservas: List<Reserva> = emptyList(),
        val como: Set<MisReservasQuery.Como> = setOf(MisReservasQuery.Como.CLIENTE, MisReservasQuery.Como.VETERINARIO),
        val fecha: LocalDate? = null,
        val estados: Set<MisReservasQuery.Estados> = emptySet(),
        val modos: Set<MisReservasQuery.Modos> = emptySet(),
        val actionMessage: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun toggleComo(c: MisReservasQuery.Como) { _ui.update { it.copy(como = it.como.toMutableSet().apply { if (!add(c)) remove(c) }) } }
    fun toggleEstado(e: MisReservasQuery.Estados) { _ui.update { it.copy(estados = it.estados.toMutableSet().apply { if (!add(e)) remove(e) }) } }
    fun toggleModo(m: MisReservasQuery.Modos) { _ui.update { it.copy(modos = it.modos.toMutableSet().apply { if (!add(m)) remove(m) }) } }
    fun setFecha(f: LocalDate?) { _ui.update { it.copy(fecha = f) } }

    fun cargar() {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                val q = MisReservasQuery(
                    como = s.como.ifEmpty { null }?.toList(),
                    fecha = s.fecha,
                    estados = s.estados.ifEmpty { null }?.toList(),
                    modos = s.modos.ifEmpty { null }?.toList()
                )
                val resp = agendaApi.misReservas(q)
                if (resp.isSuccessful) {
                    _ui.update { it.copy(isLoading = false, reservas = resp.body().orEmpty()) }
                } else {
                    _ui.update { it.copy(isLoading = false, error = "Error ${resp.code()} al cargar reservas") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar reservas") }
            }
        }
    }

    fun confirmar(r: Reserva) {
        val id = r.id ?: return
        // Optimista: marcar como CONFIRMADA si estaba PENDIENTE
        _ui.update { it.copy(reservas = it.reservas.map { cur -> if (cur.id == id && cur.estado == Reserva.Estado.PENDIENTE) cur.copy(estado = Reserva.Estado.CONFIRMADA) else cur }) }
        viewModelScope.launch {
            try {
                val resp = agendaApi.confirmar(ReservaAccionRequest(id))
                if (resp.isSuccessful) {
                    _ui.update { it.copy(actionMessage = "Reserva confirmada", error = null) }
                    // Refrescar para sincronizar otros cambios (horaFin, etc.)
                    cargar()
                } else {
                    _ui.update { it.copy(error = "Error ${resp.code()} al confirmar") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Error al confirmar") }
            }
        }
    }

    fun cancelar(r: Reserva) {
        val id = r.id ?: return
        // Optimista: marcar como CANCELADA si no lo está (comparación defensiva por nombre)
        _ui.update {
            it.copy(
                reservas = it.reservas.map { cur ->
                    if (cur.id == id && cur.estado.name != "CANCELADA") cur.copy(estado = cur.estado) else cur
                }
            )
        }
        viewModelScope.launch {
            try {
                val resp = agendaApi.cancelar(ReservaAccionRequest(id))
                if (resp.isSuccessful) {
                    _ui.update { it.copy(actionMessage = "Reserva cancelada", error = null) }
                    cargar()
                } else {
                    _ui.update { it.copy(error = "Error ${resp.code()} al cancelar") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Error al cancelar") }
            }
        }
    }

    fun consumirMensaje() { _ui.update { it.copy(actionMessage = null) } }
}
