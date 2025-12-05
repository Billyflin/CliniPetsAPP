package cl.clinipets.ui.features.booking

import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.ServicioMedicoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val mascota: MascotaResponse,
    val servicio: ServicioMedicoDto,
    val precio: Int
)

data class CartState(
    val cart: List<CartItem> = emptyList(),
    val totalDuration: Int = 0,
    val totalPrice: Int = 0,
    val minDeposit: Int = 0
)

@Singleton
class CartManager @Inject constructor() {

    private val _cartState = MutableStateFlow(CartState())
    val cartState = _cartState.asStateFlow()

    fun addToCart(pet: MascotaResponse, service: ServicioMedicoDto) {
        // Final Stock Check (Security)
        if (service.stock != null && service.stock <= 0) {
            // This should ideally be communicated back to the ViewModel to show an error
            // For now, we just prevent adding it. A more robust solution could involve a sealed class for results.
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
        val newCart = _cartState.value.cart + item

        updateCartState(newCart)
    }

    fun removeFromCart(item: CartItem) {
        val newCart = _cartState.value.cart - item
        updateCartState(newCart)
    }

    fun clearCart() {
        updateCartState(emptyList())
    }

    private fun updateCartState(newCart: List<CartItem>) {
        val totalDuration = newCart.sumOf { it.servicio.duracionMinutos }
        val totalPrice = newCart.sumOf { it.precio }
        val minDeposit = newCart.mapNotNull { it.servicio.precioAbono }.maxOrNull() ?: 0

        _cartState.update {
            it.copy(
                cart = newCart,
                totalDuration = totalDuration,
                totalPrice = totalPrice,
                minDeposit = minDeposit
            )
        }
    }
}

