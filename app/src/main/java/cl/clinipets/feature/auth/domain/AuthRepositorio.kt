package cl.clinipets.feature.auth.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.LoginResponse
import cl.clinipets.openapi.models.MeResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepositorio {
    val sesion: Flow<Sesion?>

    suspend fun iniciarSesionConGoogle(idToken: String): Resultado<LoginResponse>

    suspend fun obtenerPerfil(): Resultado<MeResponse>

    suspend fun cerrarSesion(): Resultado<Unit>
}
