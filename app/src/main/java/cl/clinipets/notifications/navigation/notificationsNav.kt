package cl.clinipets.notifications.navigation

import kotlinx.serialization.Serializable

// Notificaciones
sealed interface NotifDest {
    @Serializable
    data object Graph : NotifDest

    @Serializable
    data object Center : NotifDest
}
