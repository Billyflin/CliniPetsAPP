package cl.clinipets.feature.veterinario.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.CrearExcepcion
import cl.clinipets.openapi.models.ExcepcionDisponibilidad
import java.util.UUID
import javax.inject.Inject

class CrearExcepcionUseCase @Inject constructor(
    private val repositorio: VeterinarioRepositorio,
) {
    suspend operator fun invoke(veterinarioId: UUID, request: CrearExcepcion): Resultado<ExcepcionDisponibilidad> =
        repositorio.crearExcepcion(veterinarioId, request)
}
