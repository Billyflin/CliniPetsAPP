package cl.clinipets.feature.veterinario.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.VeterinarioPerfil
import javax.inject.Inject

class ObtenerPerfilVeterinarioUseCase @Inject constructor(
    private val repositorio: VeterinarioRepositorio,
) {
    suspend operator fun invoke(): Resultado<VeterinarioPerfil> = repositorio.obtenerMiPerfil()
}
