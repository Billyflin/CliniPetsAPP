package cl.clinipets.data.repositories

import cl.clinipets.data.api.DiscoveryApi
import cl.clinipets.domain.discovery.DiscoveryRepository

class DiscoveryRepositoryImpl(private val api: DiscoveryApi) : DiscoveryRepository {
    override suspend fun buscar(lat: Double, lon: Double, radio: Int): String =
        api.buscar(lat, lon, radio)
}

