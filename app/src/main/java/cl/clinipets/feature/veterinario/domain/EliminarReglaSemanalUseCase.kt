package cl.clinipets.feature.veterinario.domain

import cl.clinipets.core.Resultado
import java.util.UUID
import javax.inject.Inject

class EliminarReglaSemanalUseCase @Inject constructor(
    private val repositorio: VeterinarioRepositorio,
) {
    suspend operator fun invoke(reglaId: UUID): Resultado<Unit> =
        repositorio.eliminarReglaSemanal(reglaId)
}
