package cl.clinipets.core.common.location

import cl.clinipets.core.data.model.common.GeoPoint
import kotlinx.coroutines.flow.Flow


interface LocationService {
    suspend fun getLastKnownLocation(): GeoPoint?
    fun observeLocation(): Flow<GeoPoint>
}