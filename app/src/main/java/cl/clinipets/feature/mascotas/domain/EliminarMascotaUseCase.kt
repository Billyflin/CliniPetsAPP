package cl.clinipets.feature.mascotas.domain

import cl.clinipets.core.Resultado
import java.util.UUID
import javax.inject.Inject

class EliminarMascotaUseCase @Inject constructor(
    private val repositorio: MascotasRepositorio,
) {
    suspend operator fun invoke(id: UUID): Resultado<Unit> = repositorio.eliminarMascota(id)
}
