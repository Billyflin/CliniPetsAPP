package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DisponibilidadControllerApi
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaResponse
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.ReservaCreateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pets: List<MascotaResponse> = emptyList(),
    val selectedPet: MascotaResponse? = null,
    val selectedDate: LocalDate? = null,
    val availableSlots: List<String> = emptyList(),
    val selectedSlot: String? = null,
    val bookingResult: CitaResponse? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi,
    private val disponibilidadApi: DisponibilidadControllerApi,
    private val reservaApi: ReservaControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPets()
    }

    fun loadPets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = mascotaApi.listarMascotas()
                if (response.isSuccessful) {
                    val pets = response.body() ?: emptyList()
                    _uiState.update { it.copy(isLoading = false, pets = pets) }
                    if (pets.isNotEmpty()) {
                        selectPet(pets.first())
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar mascotas") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectPet(pet: MascotaResponse) {
        _uiState.update { it.copy(selectedPet = pet) }
    }

    fun selectDate(date: LocalDate, serviceId: String) {
        _uiState.update { it.copy(selectedDate = date, selectedSlot = null, availableSlots = emptyList()) }
        fetchAvailability(date, serviceId)
    }

    private fun fetchAvailability(date: LocalDate, serviceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Convert LocalDate to OffsetDateTime using system default zone
                val dateTime = date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()
                val response = disponibilidadApi.obtenerDisponibilidad(dateTime, UUID.fromString(serviceId))
                
                if (response.isSuccessful) {
                    val availability = response.body()
                    // Map OffsetDateTime slots to String (HH:mm)
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    val slots = availability?.slots?.map { it.format(formatter) } ?: emptyList()
                    
                    _uiState.update { it.copy(isLoading = false, availableSlots = slots) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al obtener disponibilidad") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectSlot(slot: String) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun createReservation(serviceId: String) {
        val currentState = _uiState.value
        val pet = currentState.selectedPet
        val date = currentState.selectedDate
        val slot = currentState.selectedSlot

        if (pet != null && date != null && slot != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    // Parse slot time (HH:mm) and combine with date using system default zone
                    val time = LocalTime.parse(slot)
                    val startDateTime = date.atTime(time).atZone(ZoneId.systemDefault()).toOffsetDateTime()

                    val request = ReservaCreateRequest(
                        servicioId = UUID.fromString(serviceId),
                        mascotaId = pet.id,
                        fechaHoraInicio = startDateTime,
                        origen = ReservaCreateRequest.Origen.APP
                    )

                    val response = reservaApi.crearReserva(request)
                    if (response.isSuccessful) {
                        val cita = response.body()
                        _uiState.update { it.copy(isLoading = false, bookingResult = cita) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Error al crear reserva: ${response.code()}") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetBookingState() {
         _uiState.update { it.copy(bookingResult = null) }
    }
}
