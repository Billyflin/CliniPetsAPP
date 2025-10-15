package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val authenticated: Boolean,
    val id: String? = null,
    val email: String? = null,
    val roles: List<String>? = null
)
