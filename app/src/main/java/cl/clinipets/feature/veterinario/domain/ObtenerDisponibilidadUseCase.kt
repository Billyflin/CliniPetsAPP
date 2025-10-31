package cl.clinipets.feature.veterinario.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.BloqueHorario
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class ObtenerDisponibilidadUseCase @Inject constructor(
    private val repositorio: VeterinarioRepositorio,
) {
    suspend operator fun invoke(veterinarioId: UUID, fecha: LocalDate): Resultado<List<BloqueHorario>> =
        repositorio.obtenerDisponibilidad(veterinarioId, fecha)
}
