package cl.clinipets.data.dto.discovery

import kotlinx.serialization.Serializable

@Serializable
data class OfertaItem(
    val ofertaId: String,
    val procedimientoId: String,
    val nombre: String,
    val precioCents: Int,
    val conStock: Boolean
)

@Serializable
data class VetItem(
    val vetId: String,
    val nombre: String,
    val distanciaM: Int,
    val openNow: Boolean,
    val lat: Double,
    val lon: Double,
    val ofertas: List<OfertaItem>
)

@Serializable
data class DiscoveryResult(
    val items: List<VetItem>,
    val nextOffset: Int
)
