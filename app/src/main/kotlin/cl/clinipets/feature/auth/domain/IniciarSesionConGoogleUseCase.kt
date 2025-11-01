package cl.clinipets.feature.auth.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.LoginResponse
import javax.inject.Inject

class IniciarSesionConGoogleUseCase @Inject constructor(
    private val repositorio: AuthRepositorio,
) {
    suspend operator fun invoke(idToken: String): Resultado<LoginResponse> =
        repositorio.iniciarSesionConGoogle(idToken)
}
