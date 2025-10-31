package cl.clinipets.feature.mascotas.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.Mascota
import java.util.UUID
import javax.inject.Inject

class ObtenerMascotaUseCase @Inject constructor(
    private val repositorio: MascotasRepositorio,
) {
    suspend operator fun invoke(id: UUID): Resultado<Mascota> = repositorio.obtenerMascota(id)
}
