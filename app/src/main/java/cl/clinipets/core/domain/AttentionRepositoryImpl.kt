package cl.clinipets.core.domain

import cl.clinipets.attention.model.VetLite
import cl.clinipets.core.data.model.common.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import kotlin.random.Random

class AttentionRepositoryImpl : AttentionRepository {
    override fun observeNearbyVets(
        center: Flow<GeoPoint>, radiusMeters: Int
    ): Flow<List<VetLite>> {
        TODO("Not yet implemented")
    }

}

class AttentionRepositoryImplFake : AttentionRepository {


    override fun observeNearbyVets(center: Flow<GeoPoint>, radiusMeters: Int): Flow<List<VetLite>> {
        return center.map { c ->
            val count = Random.nextInt(3, 7)
            (1..count).map { idx ->
                // offsets pequeños (~30–60 m) para simular vets alrededor
                val dLat = Random.nextDouble(-0.0200, 0.0200) // ~±55 m
                val dLng = Random.nextDouble(-0.0200, 0.0200)
                val lat = c.lat + dLat
                val lng = c.lng + dLng
                val dist = haversineMeters(c.lat, c.lng, lat, lng).roundToInt()
                VetLite(
                    id = "vet-$idx",
                    name = listOf("Dra. Paula", "Dr. Mikasa", "Dra. Sofía", "Dr. Marco").random(),
                    rating = listOf(4.6, 4.7, 4.8, 4.9).random(),
                    distanceMeters = dist,
                    location = GeoPoint(lat, lng)
                )
            }.sortedBy { it.distanceMeters }
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) + kotlin.math.cos(
            Math.toRadians(lat1)
        ) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.asin(kotlin.math.min(1.0, kotlin.math.sqrt(a)))
        return R * c
    }
}