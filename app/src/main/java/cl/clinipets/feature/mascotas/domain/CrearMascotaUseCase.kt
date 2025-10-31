package cl.clinipets.feature.mascotas.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.Mascota
import javax.inject.Inject

class CrearMascotaUseCase @Inject constructor(
    private val repositorio: MascotasRepositorio,
) {
    suspend operator fun invoke(datos: CrearMascota): Resultado<Mascota> =
        repositorio.crearMascota(datos)
}
