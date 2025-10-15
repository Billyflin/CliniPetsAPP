package cl.clinipets.domain.discovery

data class VetNearby(
    val id: String,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val openNow: Boolean = false,
    val ofertaNombre: String? = null,
    val ofertaPrecioMin: Double? = null
)

