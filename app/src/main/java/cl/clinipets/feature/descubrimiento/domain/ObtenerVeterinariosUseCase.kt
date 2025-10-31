package cl.clinipets.feature.descubrimiento.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.VetItem
import javax.inject.Inject

class ObtenerVeterinariosUseCase @Inject constructor(
    private val repositorio: DescubrimientoRepositorio,
) {
    suspend operator fun invoke(filtros: FiltrosVeterinarios): Resultado<List<VetItem>> =
        repositorio.buscarVeterinarios(filtros)
}
