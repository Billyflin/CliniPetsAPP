package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpsertPerfil(
    val nombre: String? = null,
    val bio: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
)

