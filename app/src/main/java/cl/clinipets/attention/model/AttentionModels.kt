package cl.clinipets.attention.model


import cl.clinipets.core.data.model.common.GeoPoint
import kotlinx.serialization.Serializable


@Serializable
data class VetLite(
    val id: String,
    val name: String,
    val rating: Double = 0.0,               // Promedio de calificación (RF-17)
    val distanceMeters: Int = 0,            // Calculado contra la ubicación del tutor
    val location: GeoPoint? = null,         // Coordenadas en vivo
)
