package cl.clinipets.feature.auth.domain

import cl.clinipets.core.Resultado
import javax.inject.Inject

class CerrarSesionUseCase @Inject constructor(
    private val repositorio: AuthRepositorio,
) {
    suspend operator fun invoke(): Resultado<Unit> = repositorio.cerrarSesion()
}
