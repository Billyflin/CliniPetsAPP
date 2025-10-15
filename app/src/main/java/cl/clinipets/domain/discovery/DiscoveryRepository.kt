package cl.clinipets.domain.discovery

interface DiscoveryRepository {
    suspend fun buscar(lat: Double, lon: Double, radio: Int): String
}

