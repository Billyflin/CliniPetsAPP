package cl.clinipets.feature.descubrimiento.data

import cl.clinipets.openapi.apis.DescubrimientoApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DescubrimientoDataSource @Inject constructor(
    private val api: DescubrimientoApi,
) {

    suspend fun obtenerVeterinarios(params: VeterinariosRequest) =
        api.descubrimientoBuscarVeterinarios(
            lat = params.lat,
            lng = params.lng,
            radioKm = params.radioKm,
            modo = params.modo,
            especie = params.especie,
            procedimientoSku = params.procedimientoSku,
            abiertoAhora = params.abiertoAhora,
            limit = params.limit,
            offset = params.offset,
        )

    suspend fun obtenerOfertas(params: OfertasRequest) =
        api.descubrimientoBuscarOfertas(
            especie = params.especie,
            procedimientoSku = params.procedimientoSku,
            vetId = params.vetId,
            activo = params.activo,
            lat = params.lat,
            lng = params.lng,
            radioKm = params.radioKm,
            limit = params.limit,
            offset = params.offset,
        )

    suspend fun obtenerProcedimientos(params: ProcedimientosRequest) =
        api.descubrimientoBuscarProcedimientos(
            especie = params.especie,
            q = params.q,
            limit = params.limit,
            offset = params.offset,
        )
}
