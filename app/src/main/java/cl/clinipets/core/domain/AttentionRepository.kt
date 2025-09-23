// core/domain/AttentionRepository.kt
package cl.clinipets.core.domain

import cl.clinipets.attention.model.VetLite
import cl.clinipets.core.data.model.common.GeoPoint
import kotlinx.coroutines.flow.Flow

interface AttentionRepository {
    /** Lista de veterinarios cercanos que cambia en tiempo real según la ubicación (center). */
    fun observeNearbyVets(center: Flow<GeoPoint>, radiusMeters: Int): Flow<List<VetLite>>
}
