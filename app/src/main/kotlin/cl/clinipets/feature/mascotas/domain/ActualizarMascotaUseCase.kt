package cl.clinipets.feature.mascotas.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.Mascota
import java.util.UUID
import javax.inject.Inject

class ActualizarMascotaUseCase @Inject constructor(
    private val repositorio: MascotasRepositorio,
) {
    suspend operator fun invoke(id: UUID, datos: ActualizarMascota): Resultado<Mascota> =
        repositorio.actualizarMascota(id, datos)
}
