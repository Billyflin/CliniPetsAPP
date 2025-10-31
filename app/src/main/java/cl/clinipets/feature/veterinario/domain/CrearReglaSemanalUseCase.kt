package cl.clinipets.feature.veterinario.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.CrearReglaSemanal
import cl.clinipets.openapi.models.ReglaSemanal
import java.util.UUID
import javax.inject.Inject

class CrearReglaSemanalUseCase @Inject constructor(
    private val repositorio: VeterinarioRepositorio,
) {
    suspend operator fun invoke(veterinarioId: UUID, request: CrearReglaSemanal): Resultado<ReglaSemanal> =
        repositorio.crearReglaSemanal(veterinarioId, request)
}
