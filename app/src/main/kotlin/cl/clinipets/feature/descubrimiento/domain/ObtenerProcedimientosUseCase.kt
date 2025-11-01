package cl.clinipets.feature.descubrimiento.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.ProcedimientoItem
import javax.inject.Inject

class ObtenerProcedimientosUseCase @Inject constructor(
    private val repositorio: DescubrimientoRepositorio,
) {
    suspend operator fun invoke(filtros: FiltrosProcedimientos): Resultado<List<ProcedimientoItem>> =
        repositorio.buscarProcedimientos(filtros)
}
