package cl.clinipets.feature.descubrimiento.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.OfertaItem
import javax.inject.Inject

class ObtenerOfertasUseCase @Inject constructor(
    private val repositorio: DescubrimientoRepositorio,
) {
    suspend operator fun invoke(filtros: FiltrosOfertas): Resultado<List<OfertaItem>> =
        repositorio.buscarOfertas(filtros)
}
