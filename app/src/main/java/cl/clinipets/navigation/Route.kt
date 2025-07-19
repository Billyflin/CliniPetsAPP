// navigation/Route.kt
package cl.clinipets.navigation

import kotlinx.serialization.Serializable

// Definir todas las rutas de la aplicación usando Type-Safe Navigation
sealed interface Route {
    @Serializable
    data object Splash : Route

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object Register : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Appointments : Route

    @Serializable
    data object Pets : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data class PetDetail(val petId: String) : Route

    @Serializable
    data class NewAppointment(val petId: String? = null) : Route

    @Serializable
    data class AppointmentDetail(val appointmentId: String) : Route

    @Serializable
    data object NewPet : Route

    @Serializable
    data class EditPet(val petId: String) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Emergency : Route
}