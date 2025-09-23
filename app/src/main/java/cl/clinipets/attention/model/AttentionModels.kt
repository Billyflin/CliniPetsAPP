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
