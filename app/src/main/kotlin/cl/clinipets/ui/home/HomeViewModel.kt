package cl.clinipets.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.ReservasApi
import cl.clinipets.openapi.models.Reserva
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reservasApi: ReservasApi
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val reservas: List<Reserva> = emptyList(),
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    fun refresh() = fetch()

    fun clear() {
        _ui.value = UiState(loading = false, reservas = emptyList(), error = null)
    }

    private fun fetch() {
        if (_ui.value.loading) return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val response = reservasApi.misReservas()
                if (response.isSuccessful) {
                    val body = response.body().orEmpty()
                    _ui.update {
                        it.copy(
                            loading = false,
                            reservas = body.filter(::isUpcoming).sortedBy { reserva -> reserva.inicio }
                        )
                    }
                } else {
                    _ui.update {
                        it.copy(
                            loading = false,
                            error = "HTTP ${response.code()}: ${response.errorBody()?.string()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Error inesperado") }
            }
        }
    }

    private fun isUpcoming(reserva: Reserva): Boolean {
        val now = OffsetDateTime.now()
        return reserva.fin.isAfter(now) &&
            reserva.estado != Reserva.Estado.CANCELADA_CLIENTE &&
            reserva.estado != Reserva.Estado.CANCELADA_VETERINARIO
    }
}
