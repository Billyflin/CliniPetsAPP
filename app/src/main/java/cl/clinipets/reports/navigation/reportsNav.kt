package cl.clinipets.reports.navigation

import kotlinx.serialization.Serializable

// Reportes
sealed interface RepDest {
    @Serializable
    data object Graph : RepDest

    @Serializable
    data object Dashboard : RepDest
}