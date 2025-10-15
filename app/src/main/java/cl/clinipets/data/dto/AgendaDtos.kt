package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CrearReserva(
    val mascotaId: String,
    val ofertaId: String,
    val inicio: String,
    val nota: String? = null
)

@Serializable
data class SlotDto(
    val inicio: String,
    val fin: String
)
