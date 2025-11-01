package cl.clinipets.feature.veterinario.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.ActualizarPerfilRequest
import cl.clinipets.openapi.models.VeterinarioPerfil
import javax.inject.Inject

class ActualizarPerfilVeterinarioUseCase @Inject constructor(
    private val repositorio: VeterinarioRepositorio,
) {
    suspend operator fun invoke(request: ActualizarPerfilRequest): Resultado<VeterinarioPerfil> =
        repositorio.actualizarPerfil(request)
}
