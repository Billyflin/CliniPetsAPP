package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val authenticated: Boolean,
    val id: String,
    val email: String,
    val roles: List<String>
)

