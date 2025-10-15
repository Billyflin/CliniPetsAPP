package cl.clinipets.data.repositories

import cl.clinipets.data.api.DiscoveryApi
import cl.clinipets.data.dto.discovery.VetNearbyDto
import cl.clinipets.data.dto.discovery.VetNearbyListWrapper
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.discovery.VetNearby
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class DiscoveryRepositoryImpl(private val api: DiscoveryApi) : DiscoveryRepository {
    override suspend fun buscar(lat: Double, lon: Double, radio: Int): String =
        api.buscar(lat, lon, radio)

    override suspend fun buscarVets(
        lat: Double,
        lon: Double,
        radio: Int,
        procedimientoId: String?,
        abiertoAhora: Boolean?,
        conStock: Boolean?,
        limit: Int,
        offset: Int
    ): List<VetNearby> {
        val raw = api.buscar(
            lat = lat,
            lon = lon,
            radio = radio,
            procId = procedimientoId,
            abierto = abiertoAhora,
            conStock = conStock,
            limit = limit,
            offset = offset
        )
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val dtos: List<VetNearbyDto> = runCatching {
            json.decodeFromString(ListSerializer(VetNearbyDto.serializer()), raw)
        }.getOrElse {
            // fallback si viene envuelto en { items: [...] }
            val wrapper = json.decodeFromString(VetNearbyListWrapper.serializer(), raw)
            wrapper.items
        }
        return dtos.map { dto ->
            val oferta = dto.ofertaPrincipal ?: dto.ofertas?.minByOrNull { it.precio ?: Double.MAX_VALUE }
            VetNearby(
                id = dto.id,
                nombre = dto.nombre,
                lat = dto.lat,
                lon = dto.lon,
                openNow = dto.openNow ?: false,
                ofertaNombre = oferta?.nombre,
                ofertaPrecioMin = oferta?.precio
            )
        }
    }
}
