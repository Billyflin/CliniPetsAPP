// navigation/ClinipetsDestination.kt
package cl.clinipets.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Routes : NavKey {
    @Serializable
    data object HomeRoute : Routes

    @Serializable
    data object AppointmentsRoute : Routes

    @Serializable
    data object PetsRoute : Routes

    @Serializable
    data object ProfileRoute : Routes

    @Serializable
    data class PetDetailRoute(val petId: String) : Routes

    @Serializable
    data class NewAppointmentRoute(val petId: String? = null) : Routes
}