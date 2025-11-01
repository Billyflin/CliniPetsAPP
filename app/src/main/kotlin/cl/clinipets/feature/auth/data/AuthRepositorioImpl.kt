package cl.clinipets.feature.auth.data

import cl.clinipets.core.Resultado
import cl.clinipets.feature.auth.domain.AuthRepositorio
import cl.clinipets.feature.auth.domain.Sesion
import cl.clinipets.openapi.apis.AutenticacinApi
import cl.clinipets.openapi.models.GoogleLoginRequest
import cl.clinipets.openapi.models.LoginResponse
import cl.clinipets.openapi.models.MeResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AuthRepositorioImpl @Inject constructor(
    private val api: AutenticacinApi,
    private val sesionLocal: SesionLocalDataSource,
) : AuthRepositorio {

    override val sesion: Flow<Sesion?> = sesionLocal.sesion

    override suspend fun iniciarSesionConGoogle(idToken: String): Resultado<LoginResponse> {
        return ejecutarYGuardar { api.authLoginGoogle(GoogleLoginRequest(idToken)) }
    }

    override suspend fun obtenerPerfil(): Resultado<MeResponse> {
        return ejecutar { api.authMe() }
    }

    override suspend fun cerrarSesion(): Resultado<Unit> {
        return try {
            val respuesta = api.authLogout()
            if (!respuesta.isSuccessful) {
                val codigo = respuesta.code()
                val tipo = when (codigo) {
                    in 400..499 -> Resultado.Tipo.CLIENTE
                    in 500..599 -> Resultado.Tipo.SERVIDOR
                    else -> Resultado.Tipo.DESCONOCIDO
                }
                Resultado.Error(tipo, respuesta.errorBody()?.string(), codigoHttp = codigo)
            } else {
                sesionLocal.limpiarSesion()
                Resultado.Exito(Unit)
            }
        } catch (io: IOException) {
            Resultado.Error(Resultado.Tipo.RED, io.message, io)
        } catch (ex: Throwable) {
            Resultado.Error(Resultado.Tipo.DESCONOCIDO, ex.message, ex)
        }
    }

    private suspend fun ejecutarYGuardar(block: suspend () -> retrofit2.Response<LoginResponse>): Resultado<LoginResponse> {
        return try {
            val respuesta = block()
            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()
                if (cuerpo == null) {
                    Resultado.Error(Resultado.Tipo.DESCONOCIDO, "Respuesta vacía.")
                } else {
                    sesionLocal.guardarSesion(cuerpo)
                    Resultado.Exito(cuerpo)
                }
            } else {
                manejarError(respuesta.code(), respuesta.errorBody()?.string())
            }
        } catch (io: IOException) {
            Resultado.Error(Resultado.Tipo.RED, io.message, io)
        } catch (ex: Throwable) {
            Resultado.Error(Resultado.Tipo.DESCONOCIDO, ex.message, ex)
        }
    }

    private suspend fun <T> ejecutar(block: suspend () -> retrofit2.Response<T>): Resultado<T> {
        return try {
            val respuesta = block()
            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()
                if (cuerpo == null) {
                    Resultado.Error(Resultado.Tipo.DESCONOCIDO, "Respuesta vacía.")
                } else {
                    Resultado.Exito(cuerpo)
                }
            } else {
                manejarError(respuesta.code(), respuesta.errorBody()?.string())
            }
        } catch (io: IOException) {
            Resultado.Error(Resultado.Tipo.RED, io.message, io)
        } catch (ex: Throwable) {
            Resultado.Error(Resultado.Tipo.DESCONOCIDO, ex.message, ex)
        }
    }

    private fun <T> manejarError(codigo: Int, mensaje: String?): Resultado<T> {
        val tipo = when (codigo) {
            in 400..499 -> Resultado.Tipo.CLIENTE
            in 500..599 -> Resultado.Tipo.SERVIDOR
            else -> Resultado.Tipo.DESCONOCIDO
        }
        return Resultado.Error(tipo, mensaje, codigoHttp = codigo)
    }
}
