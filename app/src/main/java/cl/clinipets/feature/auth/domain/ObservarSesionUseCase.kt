package cl.clinipets.feature.auth.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservarSesionUseCase @Inject constructor(
    private val repositorio: AuthRepositorio,
) {
    operator fun invoke(): Flow<Sesion?> = repositorio.sesion
}
