package cl.clinipets.appointments.navigation

import kotlinx.serialization.Serializable

// Citas
sealed interface ApptDest {
    @Serializable
    data object Graph : ApptDest

    @Serializable
    data object Calendar : ApptDest

    @Serializable
    data class Details(val appointmentId: String) : ApptDest
}
