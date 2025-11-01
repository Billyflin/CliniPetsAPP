package cl.clinipets.feature.auth.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.MeResponse
import javax.inject.Inject

class ObtenerPerfilUseCase @Inject constructor(
    private val repositorio: AuthRepositorio,
) {
    suspend operator fun invoke(): Resultado<MeResponse> = repositorio.obtenerPerfil()
}
