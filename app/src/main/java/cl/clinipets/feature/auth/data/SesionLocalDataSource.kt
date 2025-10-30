package cl.clinipets.feature.auth.data

import cl.clinipets.feature.auth.domain.Sesion
import cl.clinipets.openapi.models.LoginResponse
import kotlinx.coroutines.flow.Flow

interface SesionLocalDataSource {
    val sesion: Flow<Sesion?>

    suspend fun guardarSesion(login: LoginResponse)

    suspend fun limpiarSesion()
}
