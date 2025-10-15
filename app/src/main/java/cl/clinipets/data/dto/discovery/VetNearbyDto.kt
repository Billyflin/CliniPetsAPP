package cl.clinipets.data.dto.discovery

import kotlinx.serialization.Serializable

@Serializable
data class OfertaDto(
    val id: String? = null,
    val nombre: String? = null,
    val precio: Double? = null
)

@Serializable
data class VetNearbyDto(
    val id: String,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val openNow: Boolean? = null,
    val ofertas: List<OfertaDto>? = null,
    val ofertaPrincipal: OfertaDto? = null
)

@Serializable
data class VetNearbyListWrapper(
    val items: List<VetNearbyDto> = emptyList()
)

