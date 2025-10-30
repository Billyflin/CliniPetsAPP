package cl.clinipets.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DefaultApi
import cl.clinipets.openapi.models.CambiarEstadoReserva
import cl.clinipets.openapi.models.Reserva
import cl.clinipets.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MyReservationsViewModel @Inject constructor(
    private val defaultApi: DefaultApi
) : ViewModel() {

    private val _reservationsState = MutableStateFlow<Result<List<Reserva>>>(Result.Loading)
    val reservationsState: StateFlow<Result<List<Reserva>>> = _reservationsState

    private val _updateReservationState = MutableStateFlow<Result<Reserva>>(Result.Success(Reserva()))
    val updateReservationState: StateFlow<Result<Reserva>> = _updateReservationState

    init {
        fetchMyReservations()
    }

    fun fetchMyReservations() {
        viewModelScope.launch {
            _reservationsState.value = Result.Loading
            try {
                val response = defaultApi.apiReservasMiasGet()
                if (response.isSuccessful) {
                    response.body()?.let { _reservationsState.value = Result.Success(it) }
                        ?: run { _reservationsState.value = Result.Error(Exception("Reservations list is null")) }
                } else {
                    _reservationsState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _reservationsState.value = Result.Error(e)
            }
        }
    }

    fun cancelReservation(reservationId: UUID, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _updateReservationState.value = Result.Loading
            try {
                val response = defaultApi.cambiarEstadoReserva(reservationId, CambiarEstadoReserva(estado = CambiarEstadoReserva.Estado.CANCELADA_CLIENTE))
                if (response.isSuccessful) {
                    response.body()?.let { _updateReservationState.value = Result.Success(it) }
                        ?: run { _updateReservationState.value = Result.Error(Exception("Updated reservation is null")) }
                    onSuccess()
                } else {
                    _updateReservationState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _updateReservationState.value = Result.Error(e)
            }
        }
    }
}
