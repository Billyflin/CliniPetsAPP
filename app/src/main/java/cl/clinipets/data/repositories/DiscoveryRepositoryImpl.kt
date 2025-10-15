package cl.clinipets.data.repositories

import cl.clinipets.data.api.DiscoveryApi
import cl.clinipets.data.dto.discovery.DiscoveryResult
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.discovery.VetNearby
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DiscoveryRepositoryImpl(private val api: DiscoveryApi) : DiscoveryRepository {
    override suspend fun buscar(lat: Double, lon: Double, radio: Int): String {
        val result: DiscoveryResult = api.buscar(lat = lat, lon = lon, radio = radio)
        val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
        return json.encodeToString(result)
    }

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
        val result: DiscoveryResult = api.buscar(
            lat = lat,
            lon = lon,
            radio = radio,
            procId = procedimientoId,
            abierto = abiertoAhora,
            conStock = conStock,
            limit = limit,
            offset = offset
        )
        return result.items.map { item ->
            val minOferta = item.ofertas.minByOrNull { it.precioCents }
            VetNearby(
                id = item.vetId,
                nombre = item.nombre,
                lat = item.lat,
                lon = item.lon,
                openNow = item.openNow,
                ofertaNombre = minOferta?.nombre,
                ofertaPrecioMin = minOferta?.let { it.precioCents / 100.0 }
            )
        }
    }
}
