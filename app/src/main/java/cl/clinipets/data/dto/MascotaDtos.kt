package cl.clinipets.data.dto

import kotlinx.serialization.Serializable

@Serializable
enum class Especie { PERRO, GATO }

@Serializable
data class Mascota(
    val id: String? = null,
    val nombre: String,
    val especie: Especie,
    val raza: String? = null,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null,
    val tutorId: String? = null
)

@Serializable
data class CrearMascota(
    val nombre: String,
    val especie: Especie,
    val raza: String? = null,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null
)

@Serializable
data class ActualizarMascota(
    val nombre: String? = null,
    val especie: Especie? = null,
    val raza: String? = null,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null
)

