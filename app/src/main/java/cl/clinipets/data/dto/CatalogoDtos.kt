package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class OfertaDto(
    val ofertaId: String? = null,
    val vetId: String,
    val procedimientoId: String? = null,
    val nombre: String,
    val precioCents: Int,
    val duracionMin: Int,
    val activo: Boolean
)

@Serializable
data class UpsertOferta(
    val procedimientoId: String,
    val precioCents: Int,
    val duracionMin: Int,
    val activo: Boolean
)

@Serializable
data class ProcedimientoDto(
    val id: String? = null,
    val nombre: String,
    val categoria: String
)

@Serializable
data class UpsertProcedimiento(
    val nombre: String,
    val categoria: String
)

@Serializable
enum class ReservaEstado { PENDIENTE, ACEPTADA, RECHAZADA, CANCELADA, COMPLETADA }

@Serializable
data class ReservaDto(
    val reservaId: String? = null,
    val mascotaId: String,
    val ofertaId: String,
    val inicio: String,
    val estado: String,
    val nota: String? = null
)
