package cl.clinipets.core.data.model.common

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(val lat: Double, val lng: Double)