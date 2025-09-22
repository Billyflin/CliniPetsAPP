package cl.clinipets.vetverification.navigation

import kotlinx.serialization.Serializable

// Verificación Vet
sealed interface VetVerDest {
    @Serializable
    data object Graph : VetVerDest

    @Serializable
    data object UploadDocs : VetVerDest

    @Serializable
    data object Status : VetVerDest
}