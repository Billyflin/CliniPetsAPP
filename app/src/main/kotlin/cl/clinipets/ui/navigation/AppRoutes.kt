package cl.clinipets.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object HomeRoute

@Serializable
object StaffAgendaRoute

@Serializable
data class StaffCitaDetailRoute(val citaId: String)

@Serializable
data class StaffAtencionRoute(val citaId: String, val mascotaId: String)

@Serializable
data class MascotaFormRoute(val petId: String? = null)

@Serializable
data class BookingRoute(val petId: String? = null)

@Serializable
data class PaymentRoute(val paymentUrl: String?, val price: Int)

@Serializable
data class PaymentResultRoute(val status: String? = null)

@Serializable
object ProfileRoute

@Serializable
object MyReservationsRoute

@Serializable
object MyPetsRoute

@Serializable
data class PetDetailRoute(val petId: String)

