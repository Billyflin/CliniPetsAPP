package cl.clinipets.chat.navigation

import kotlinx.serialization.Serializable

sealed interface ChatDest {
    @Serializable
    data object Graph :ChatDest

    @Serializable
    data object ThreadList :ChatDest

    @Serializable
    data class Thread(val conversationId: String) :ChatDest
}