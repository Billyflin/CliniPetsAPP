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
    val selectedPet: MascotaResponse? = null,
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
                        // Reset slot selection if cart changes, forcing user to re-select
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

        // Delegate to CartManager
        cartManager.addToCart(pet, service)

        // The stock check is now inside CartManager. If it fails, it won't add the item.
        // We might want to expose an error state from CartManager in the future.
        // For now, we can check if the cart actually changed.
        val serviceInCart = cartManager.cartState.value.cart.any { it.servicio.id == service.id && it.mascota.id == pet.id }
        if (!serviceInCart && service.stock != null && service.stock <= 0) {
             _uiState.update { it.copy(error = "Lo sentimos, este servicio se ha agotado.") }
             recalculateAvailableServices(pet) // Refresh list
             return
        }

        // Check for required services (Client-side validation warning)
        if (service.serviciosRequeridosIds.isNotEmpty()) {
            val currentCart = cartManager.cartState.value.cart
            val missingRequirements = service.serviciosRequeridosIds.filter { reqId ->
                currentCart.none { it.servicio.id == reqId && it.mascota.id == pet.id }
            }

            if (missingRequirements.isNotEmpty()) {
                val missingNames = _allServices.filter { it.id in missingRequirements }.joinToString(", ") { it.nombre }
                if (missingNames.isNotEmpty()) {
                    _uiState.update { it.copy(error = "Este servicio requiere: $missingNames. Asegúrate de agregarlo(s) al carrito.") }
                }
            }
        }
    }

    fun removeFromCart(item: CartItem) {
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
                        origen = ReservaCreateRequest.Origen.APP,
                        tipoAtencion = currentState.tipoAtencion,
                        pagoTotal = false
                    )

                    val response = reservaApi.crearReserva(request)
                    if (response.isSuccessful) {
                        _uiState.update { it.copy(isLoading = false, bookingResult = response.body()) }
                        // El carrito y los slots se limpiarán explícitamente desde la UI
                    } else {
                        val errorMessage = if (response.code() == 400) {
                            response.errorBody()?.string() ?: "Error de validación (400)"
                        } else {
                            "Error reserva: ${response.code()}"
                        }
                        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    }
                } catch (e: HttpException) {
                    val errorMessage = if (e.code() == 400) {
                        e.response()?.errorBody()?.string() ?: "Error de validación (400)"
                    } else {
                        e.message() ?: "Error desconocido"
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                } catch (e: Exception) {
                    val errorMessage = if (e is HttpException && e.code() == 400) {
                        e.response()?.errorBody()?.string() ?: "Error de validación"
                    } else {
                        e.message ?: "Error desconocido"
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
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

    /**
     * Limpia por completo el estado del carrito y de la reserva.
     * Llamar cuando la UI ya consumió el evento de éxito (ej. después
     * de navegar a la pantalla de pago o mostrar confirmación).
     */
    fun clearCart() {
        cartManager.clearCart()
        _uiState.update {
            it.copy(
                selectedSlot = null,
                availableSlots = emptyList(),
                bookingResult = null
            )
        }
    }
}