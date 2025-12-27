package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DisponibilidadControllerApi
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.apis.ServicioMedicoControllerApi
import cl.clinipets.openapi.models.ReservaItemRequest
import cl.clinipets.openapi.models.ReservaCreateRequest
import cl.clinipets.openapi.models.CitaResponse
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.ServicioMedicoDto
import cl.clinipets.ui.features.booking.CartItem
import cl.clinipets.ui.features.booking.CartManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pets: List<MascotaResponse> = emptyList(),
    val services: List<ServicioMedicoDto> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val totalDuration: Int = 0,
    val totalPrice: Int = 0,
    
    // Selection State for building order
    val selectedPets: Set<UUID> = emptySet(),
    val selectedService: ServicioMedicoDto? = null,
    val tipoAtencion: ReservaCreateRequest.TipoAtencion = ReservaCreateRequest.TipoAtencion.CLINICA,

    // Agenda State
    val selectedDate: LocalDate? = null,
    val availableSlots: List<OffsetDateTime> = emptyList(),
    val selectedSlot: OffsetDateTime? = null,
    val bookingResult: CitaResponse? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi,
    private val disponibilidadApi: DisponibilidadControllerApi,
    private val reservaApi: ReservaControllerApi,
    private val servicioApi: ServicioMedicoControllerApi,
    private val cartManager: CartManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState = _uiState.asStateFlow()

    private var _allServices: List<ServicioMedicoDto> = emptyList()

    init {
        loadInitialData()
        observeCart()
    }

    private fun observeCart() {
        cartManager.cartState
            .onEach { cartState ->
                val needsAvailabilityUpdate = _uiState.value.totalDuration != cartState.totalDuration && _uiState.value.selectedDate != null

                _uiState.update {
                    it.copy(
                        cart = cartState.cart,
                        totalDuration = cartState.totalDuration,
                        totalPrice = cartState.totalPrice,
                        selectedSlot = if (needsAvailabilityUpdate) null else it.selectedSlot,
                        availableSlots = if (needsAvailabilityUpdate) emptyList() else it.availableSlots
                    )
                }

                if (needsAvailabilityUpdate) {
                    _uiState.value.selectedDate?.let { fetchAvailability(it) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val petsResponse = mascotaApi.listarMascotas()
                val servicesResponse = servicioApi.listarServicios()

                if (petsResponse.isSuccessful && servicesResponse.isSuccessful) {
                    _allServices = servicesResponse.body() ?: emptyList()
                    val pets = petsResponse.body() ?: emptyList()
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            pets = pets
                        ) 
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar datos iniciales") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun togglePetSelection(petId: UUID) {
        _uiState.update { state ->
            val newSelected = if (state.selectedPets.contains(petId)) {
                state.selectedPets - petId
            } else {
                state.selectedPets + petId
            }
            state.copy(selectedPets = newSelected)
        }
    }

    fun getFilteredServicesForPet(pet: MascotaResponse): List<ServicioMedicoDto> {
        return _allServices.filter { service ->
            val isSpeciesAllowed = service.especiesPermitidas.isNullOrEmpty() || 
                                   service.especiesPermitidas.any { it.name == pet.especie.name }
            val hasStock = service.stock == null || service.stock > 0
            isSpeciesAllowed && hasStock
        }
    }

    fun addServiceToPet(pet: MascotaResponse, service: ServicioMedicoDto) {
        cartManager.addToCart(pet, service)
        
        // Validation for required services
        if (service.serviciosRequeridosIds.isNotEmpty()) {
            val currentCart = cartManager.cartState.value.cart
            val missingRequirements = service.serviciosRequeridosIds.filter { reqId ->
                currentCart.none { it.servicio.id == reqId && it.mascota.id == pet.id }
            }

            if (missingRequirements.isNotEmpty()) {
                val missingNames = _allServices.filter { it.id in missingRequirements }.joinToString(", ") { it.nombre }
                if (missingNames.isNotEmpty()) {
                    _uiState.update { it.copy(error = "Mascota ${pet.nombre} requiere adicionalmente: $missingNames") }
                }
            }
        }
    }

    fun removeServiceFromPet(item: CartItem) {
        cartManager.removeFromCart(item)
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, selectedSlot = null, availableSlots = emptyList()) }
        if (_uiState.value.cart.isEmpty()) return
        fetchAvailability(date)
    }

    private fun fetchAvailability(date: LocalDate) {
        val duration = _uiState.value.totalDuration
        if (duration == 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = disponibilidadApi.obtenerDisponibilidad(date, duration)
                if (response.isSuccessful) {
                    val availability = response.body()
                    val slots = availability?.slots ?: emptyList()
                    _uiState.update { it.copy(isLoading = false, availableSlots = slots) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error disponibilidad: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectSlot(slot: OffsetDateTime) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun createReservation() {
        val currentState = _uiState.value
        val selectedSlot = currentState.selectedSlot
        val cart = currentState.cart

        if (selectedSlot != null && cart.isNotEmpty()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    // Flatten cart items to ReservaItemRequest
                    val detalles = cart.map {
                        ReservaItemRequest(
                            mascotaId = it.mascota.id!!,
                            servicioId = it.servicio.id,
                            cantidad = 1
                        )
                    }

                    val request = ReservaCreateRequest(
                        detalles = detalles,
                        fechaHoraInicio = selectedSlot,
                        origen = ReservaCreateRequest.Origen.APP,
                        tipoAtencion = currentState.tipoAtencion,
                        pagoTotal = false
                    )

                    val response = reservaApi.crearReserva(request)
                    if (response.isSuccessful) {
                        _uiState.update { it.copy(isLoading = false, bookingResult = response.body()) }
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Error reserva: ${response.code()}"
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun resetBookingState() = _uiState.update { it.copy(bookingResult = null) }
    fun clearCart() {
        cartManager.clearCart()
        _uiState.update { it.copy(selectedSlot = null, availableSlots = emptyList(), bookingResult = null) }
    }
}