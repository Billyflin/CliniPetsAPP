package cl.clinipets.onboarding.navigation

import kotlinx.serialization.Serializable

// Onboarding
sealed interface OnbDest {
    @Serializable
    data object Graph : OnbDest

    @Serializable
    data object RolePicker : OnbDest

    @Serializable
    data object BasicInfo : OnbDest
}
