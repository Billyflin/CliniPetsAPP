package cl.clinipets.feature.veterinario.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import cl.clinipets.openapi.models.VeterinarioPerfil
import javax.inject.Inject

class RegistrarVeterinarioUseCase @Inject constructor(
    private val repositorio: VeterinarioRepositorio,
) {
    suspend operator fun invoke(request: RegistrarVeterinarioRequest): Resultado<VeterinarioPerfil> =
        repositorio.registrar(request)
}
