package cl.clinipets.history.navigation

import kotlinx.serialization.Serializable

// Historial
sealed interface HistDest {
    @Serializable
    data object Graph : HistDest

    @Serializable
    data object PetList : HistDest

    @Serializable
    data class PetDetails(val petId: String) : HistDest

    @Serializable
    data class PdfPreview(val petId: String? = null) : HistDest
}