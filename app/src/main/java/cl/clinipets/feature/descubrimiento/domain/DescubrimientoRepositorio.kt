package cl.clinipets.feature.descubrimiento.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.OfertaItem
import cl.clinipets.openapi.models.ProcedimientoItem
import cl.clinipets.openapi.models.VetItem

interface DescubrimientoRepositorio {
    suspend fun buscarVeterinarios(filtros: FiltrosVeterinarios): Resultado<List<VetItem>>
    suspend fun buscarOfertas(filtros: FiltrosOfertas): Resultado<List<OfertaItem>>
    suspend fun buscarProcedimientos(filtros: FiltrosProcedimientos): Resultado<List<ProcedimientoItem>>
}

data class FiltrosVeterinarios(
    val lat: Double? = null,
    val lng: Double? = null,
    val radioKm: Double? = 10.0,
    val modo: String? = null,
    val especie: String? = null,
    val procedimientoSku: String? = null,
    val abiertoAhora: Boolean? = null,
    val limit: Int? = 50,
    val offset: Int? = 0,
)

data class FiltrosOfertas(
    val especie: String? = null,
    val procedimientoSku: String? = null,
    val vetId: java.util.UUID? = null,
    val activo: Boolean? = true,
    val lat: Double? = null,
    val lng: Double? = null,
    val radioKm: Double? = null,
    val limit: Int? = 50,
    val offset: Int? = 0,
)

data class FiltrosProcedimientos(
    val especie: String? = null,
    val q: String? = null,
    val limit: Int? = 50,
    val offset: Int? = 0,
)
