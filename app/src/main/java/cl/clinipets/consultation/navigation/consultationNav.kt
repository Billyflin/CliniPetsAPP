package cl.clinipets.consultation.navigation

import kotlinx.serialization.Serializable

// Consulta
sealed interface ConsDest {
    @Serializable
    data object Graph : ConsDest

    @Serializable
    data class Form(val appointmentId: String) : ConsDest

    @Serializable
    data class Checkout(val consultationId: String) : ConsDest

    @Serializable
    data class Derivation(val consultationId: String) : ConsDest
}
