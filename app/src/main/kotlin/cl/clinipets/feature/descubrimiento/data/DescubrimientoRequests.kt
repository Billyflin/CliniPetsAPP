package cl.clinipets.feature.descubrimiento.data

import cl.clinipets.openapi.apis.DescubrimientoApi

data class VeterinariosRequest(
    val lat: Double? = null,
    val lng: Double? = null,
    val radioKm: Double? = 10.0,
    val modo: DescubrimientoApi.ModoDescubrimientoBuscarVeterinarios? = null,
    val especie: DescubrimientoApi.EspecieDescubrimientoBuscarVeterinarios? = null,
    val procedimientoSku: String? = null,
    val abiertoAhora: Boolean? = null,
    val limit: Int? = 50,
    val offset: Int? = 0,
)

data class OfertasRequest(
    val especie: DescubrimientoApi.EspecieDescubrimientoBuscarOfertas? = null,
    val procedimientoSku: String? = null,
    val vetId: java.util.UUID? = null,
    val activo: Boolean? = true,
    val lat: Double? = null,
    val lng: Double? = null,
    val radioKm: Double? = null,
    val limit: Int? = 50,
    val offset: Int? = 0,
)

data class ProcedimientosRequest(
    val especie: DescubrimientoApi.EspecieDescubrimientoBuscarProcedimientos? = null,
    val q: String? = null,
    val limit: Int? = 50,
    val offset: Int? = 0,
)
