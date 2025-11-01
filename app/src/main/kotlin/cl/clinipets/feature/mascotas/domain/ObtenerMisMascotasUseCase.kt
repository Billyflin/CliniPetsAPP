package cl.clinipets.feature.mascotas.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.Mascota
import javax.inject.Inject

class ObtenerMisMascotasUseCase @Inject constructor(
    private val repositorio: MascotasRepositorio,
) {
    suspend operator fun invoke(): Resultado<List<Mascota>> = repositorio.obtenerMisMascotas()
}
