package cl.clinipets.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DefaultApi
import cl.clinipets.openapi.models.BloqueHorario
import cl.clinipets.openapi.models.CrearReserva
import cl.clinipets.openapi.models.Reserva
import cl.clinipets.openapi.models.VetItem
import cl.clinipets.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VetDetailViewModel @Inject constructor(
    private val defaultApi: DefaultApi
) : ViewModel() {

    private val _vetDetailsState = MutableStateFlow<Result<VetItem>>(Result.Loading)
    val vetDetailsState: StateFlow<Result<VetItem>> = _vetDetailsState

    private val _availabilityState = MutableStateFlow<Result<List<BloqueHorario>>>(Result.Loading)
    val availabilityState: StateFlow<Result<List<BloqueHorario>>> = _availabilityState

    private val _createReservationState = MutableStateFlow<Result<Reserva>>(Result.Success(Reserva())) // Default empty Reserva
    val createReservationState: StateFlow<Result<Reserva>> = _createReservationState

    fun fetchVetDetails(vetId: UUID) {
        viewModelScope.launch {
            _vetDetailsState.value = Result.Loading
            try {
                // The API does not have a direct endpoint to get a single VetItem by ID.
                // We'll have to re-use the search endpoint and filter.
                val response = defaultApi.apiDescubrimientoVeterinariosGet(vetId = vetId)
                if (response.isSuccessful) {
                    val vetItem = response.body()?.firstOrNull()
                    vetItem?.let { _vetDetailsState.value = Result.Success(it) }
                        ?: run { _vetDetailsState.value = Result.Error(Exception("Veterinarian not found")) }
                } else {
                    _vetDetailsState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _vetDetailsState.value = Result.Error(e)
            }
        }
    }

    fun fetchAvailability(vetId: UUID, date: LocalDate) {
        viewModelScope.launch {
            _availabilityState.value = Result.Loading
            try {
                val response = defaultApi.apiDisponibilidadVeterinarioVeterinarioIdGet(vetId, date)
                if (response.isSuccessful) {
                    response.body()?.let { _availabilityState.value = Result.Success(it) }
                        ?: run { _availabilityState.value = Result.Error(Exception("Availability data is null")) }
                } else {
                    _availabilityState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _availabilityState.value = Result.Error(e)
            }
        }
    }

    fun createReservation(crearReserva: CrearReserva, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _createReservationState.value = Result.Loading
            try {
                val response = defaultApi.crearReserva(crearReserva)
                if (response.isSuccessful) {
                    response.body()?.let { _createReservationState.value = Result.Success(it) }
                        ?: run { _createReservationState.value = Result.Error(Exception("Reservation response is null")) }
                    onSuccess()
                } else {
                    _createReservationState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _createReservationState.value = Result.Error(e)
            }
        }
    }
}
