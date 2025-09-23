package cl.clinipets.attention.model


import cl.clinipets.core.data.model.common.GeoPoint
import kotlinx.serialization.Serializable

@Serializable
data class AttentionRequest(
    val tutorUid: String,
    val petId: String?,
    val desiredLocation: GeoPoint,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
enum class VisitType { HOME, CLINIPETS }

data class RequestForm(
    val petId: String? = null,
    val visitType: VisitType = VisitType.HOME,
    val location: GeoPoint? = null,             // requerido si HOME
    val dateMillisUtc: Long? = null,            // 00:00 del día
    val timeMinutes: Int? = null,               // minutos desde 00:00
    val reason: String = ""
)


@Serializable
data class VetLite(
    val id: String,
    val name: String,
    val rating: Double = 0.0,               // Promedio de calificación (RF-17)
    val distanceMeters: Int = 0,            // Calculado contra la ubicación del tutor
    val location: GeoPoint? = null,         // Coordenadas en vivo
)
