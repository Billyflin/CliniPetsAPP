package cl.clinipets.core.common.location

import cl.clinipets.core.data.model.common.GeoPoint


interface LocationService {
    suspend fun getLastKnownLocation(): GeoPoint?
}
