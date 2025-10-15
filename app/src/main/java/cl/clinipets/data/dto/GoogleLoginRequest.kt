package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoogleLoginRequest(val idToken: String)

