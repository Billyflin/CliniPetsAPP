// navigation/ClinipetsDestinations.kt
package cl.clinipets.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable
    data object HomeRoute : NavKey, Routes
}


sealed interface ClinipetsDestination :
    Routes,
    NavKey {
    @Serializable
    data object Home : ClinipetsDestination

    @Serializable
    data object Appointments : ClinipetsDestination

    @Serializable
    data object Pets : ClinipetsDestination

    @Serializable
    data object Profile : ClinipetsDestination

    @Serializable
    data class PetDetail(val petId: String) : ClinipetsDestination

    @Serializable
    data class NewAppointment(val petId: String? = null) : ClinipetsDestination
}