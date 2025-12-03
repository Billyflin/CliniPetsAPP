package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DisponibilidadControllerApi
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.apis.ServicioMedicoControllerApi
import cl.clinipets.openapi.models.CitaResponse
import cl.clinipets.openapi.models.DetalleReservaRequest
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.ReservaCreateRequest
import cl.clinipets.openapi.models.ServicioMedicoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import java.time.OffsetDateTime

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val mascota: MascotaResponse,
    val servicio: ServicioMedicoDto,
    val precio: Int
)

data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pets: List<MascotaResponse> = emptyList(),
    val services: List<ServicioMedicoDto> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val totalDuration: Int = 0,
    val totalPrice: Int = 0,
    
    // Selection State for building order
    val selectedPet: MascotaResponse? = null,
    val selectedService: ServicioMedicoDto? = null,

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
    private val servicioApi: ServicioMedicoControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState = _uiState.asStateFlow()

    private var _allServices: List<ServicioMedicoDto> = emptyList()

    init {
        loadInitialData()
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
                    // Pre-select first pet if available
                    if (pets.isNotEmpty()) {
                        selectPet(pets.first())
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar datos iniciales") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectPet(pet: MascotaResponse) {
        _uiState.update { it.copy(selectedPet = pet) }
        recalculateAvailableServices(pet)
    }

    private fun recalculateAvailableServices(pet: MascotaResponse) {
        val filteredServices = _allServices.filter { service ->
            // Filter by Species
            val isSpeciesAllowed = service.especiesPermitidas.isNullOrEmpty() || 
                                   service.especiesPermitidas.any { it.name == pet.especie.name }
            
            // Filter by Stock (Hidden Rule: Exclude if stock <= 0)
            // If stock is null, it's unlimited/service (always true)
            val hasStock = service.stock == null || service.stock > 0

            isSpeciesAllowed && hasStock
        }
        _uiState.update { it.copy(services = filteredServices, selectedService = null) }
    }

    fun selectService(service: ServicioMedicoDto) {
        _uiState.update { it.copy(selectedService = service) }
    }

    fun addToCart() {
        val currentState = _uiState.value
        val pet = currentState.selectedPet ?: return
        val service = currentState.selectedService ?: return

        // Final Stock Check (Security)
        if (service.stock != null && service.stock <= 0) {
            _uiState.update { it.copy(error = "Lo sentimos, este servicio se ha agotado.") }
            // Refresh list just in case
            recalculateAvailableServices(pet)
            return
        }

        // Calculate Price
        var precio = service.precioBase
        service.reglas?.let { reglas ->
            val peso = pet.pesoActual
            val reglaAplicable = reglas.find { regla ->
                peso >= regla.pesoMin && peso <= regla.pesoMax
            }
            if (reglaAplicable != null) {
                precio = reglaAplicable.precio
            }
        }

        val item = CartItem(mascota = pet, servicio = service, precio = precio)
        val newCart = currentState.cart + item
        
        updateCartState(newCart)
    }

    fun removeFromCart(item: CartItem) {
        val newCart = _uiState.value.cart - item
        updateCartState(newCart)
    }

    private fun updateCartState(newCart: List<CartItem>) {
        val totalDuration = newCart.sumOf { it.servicio.duracionMinutos }
        val totalPrice = newCart.sumOf { it.precio }
        
        _uiState.update { 
            it.copy(
                cart = newCart,
                totalDuration = totalDuration,
                totalPrice = totalPrice,
                selectedSlot = null, // Reset slot if duration changes
                availableSlots = emptyList() // Force re-fetch or clear
            ) 
        }
        // Re-fetch availability if date is selected
        _uiState.value.selectedDate?.let { selectDate(it) }
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
                    // Store raw slots (OffsetDateTime) directly
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
                    val detalles = cart.map { item ->
                        DetalleReservaRequest(
                            servicioId = item.servicio.id,
                            mascotaId = item.mascota.id
                        )
                    }

                    val request = ReservaCreateRequest(
                        detalles = detalles,
                        fechaHoraInicio = selectedSlot,
                        origen = ReservaCreateRequest.Origen.APP
                    )

                    val response = reservaApi.crearReserva(request)
                    if (response.isSuccessful) {
                        _uiState.update { it.copy(isLoading = false, bookingResult = response.body()) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Error reserva: ${response.code()}") }
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