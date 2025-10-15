package cl.clinipets.domain.discovery

interface DiscoveryRepository {
    suspend fun buscar(lat: Double, lon: Double, radio: Int): String

    // Nuevo: búsqueda tipada para el mapa con filtros opcionales
    suspend fun buscarVets(
        lat: Double,
        lon: Double,
        radio: Int,
        procedimientoId: String? = null,
        abiertoAhora: Boolean? = null,
        conStock: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<cl.clinipets.domain.discovery.VetNearby>
}
