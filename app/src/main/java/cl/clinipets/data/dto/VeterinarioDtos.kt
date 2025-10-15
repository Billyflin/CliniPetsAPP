package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpsertPerfil(
    val nombre: String? = null,
    val bio: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
)

@Serializable
data class MiPerfilDto(
    val vetId: String,
    val nombre: String,
    val bio: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
)

@Serializable
data class PublicVetDto(
    val vetId: String,
    val nombre: String,
    val verificado: Boolean,
    val latitud: Double,
    val longitud: Double,
)

@Serializable
data class VerificarResponse(
    val vetId: String,
    val verificado: Boolean,
)
