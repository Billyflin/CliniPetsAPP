package cl.clinipets.feature.veterinario.data

import cl.clinipets.core.Resultado
import cl.clinipets.feature.veterinario.domain.VeterinarioRepositorio
import cl.clinipets.openapi.models.ActualizarPerfilRequest
import cl.clinipets.openapi.models.CrearExcepcion
import cl.clinipets.openapi.models.CrearReglaSemanal
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import cl.clinipets.openapi.models.VeterinarioPerfil
import cl.clinipets.openapi.models.ReglaSemanal
import cl.clinipets.openapi.models.ExcepcionDisponibilidad
import cl.clinipets.openapi.models.BloqueHorario
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response
import java.time.LocalDate
import java.util.UUID

@Singleton
class VeterinarioRepositorioImpl @Inject constructor(
    private val remoto: VeterinarioRemoteDataSource,
    private val disponibilidadRemote: DisponibilidadRemoteDataSource,
) : VeterinarioRepositorio {

    override suspend fun registrar(request: RegistrarVeterinarioRequest): Resultado<VeterinarioPerfil> =
        ejecutar { remoto.registrar(request) }

    override suspend fun obtenerMiPerfil(): Resultado<VeterinarioPerfil> =
        ejecutar { remoto.obtenerMiPerfil() }

    override suspend fun actualizarPerfil(request: ActualizarPerfilRequest): Resultado<VeterinarioPerfil> =
        ejecutar { remoto.actualizarPerfil(request) }

    override suspend fun crearReglaSemanal(
        veterinarioId: UUID,
        request: CrearReglaSemanal,
    ): Resultado<ReglaSemanal> = ejecutar { disponibilidadRemote.crearRegla(veterinarioId, request) }

    override suspend fun eliminarReglaSemanal(reglaId: UUID): Resultado<Unit> =
        ejecutar(onEmptyBody = { Unit }) { disponibilidadRemote.eliminarRegla(reglaId) }

    override suspend fun crearExcepcion(
        veterinarioId: UUID,
        request: CrearExcepcion,
    ): Resultado<ExcepcionDisponibilidad> = ejecutar { disponibilidadRemote.crearExcepcion(veterinarioId, request) }

    override suspend fun obtenerDisponibilidad(
        veterinarioId: UUID,
        fecha: LocalDate,
    ): Resultado<List<BloqueHorario>> = ejecutar { disponibilidadRemote.obtenerDisponibilidad(veterinarioId, fecha) }

    private suspend fun <T> ejecutar(
        onEmptyBody: (() -> T)? = null,
        block: suspend () -> Response<T>,
    ): Resultado<T> {
        return try {
            val respuesta = block()
            if (respuesta.isSuccessful) {
                val body = respuesta.body()
                when {
                    body != null -> Resultado.Exito(body)
                    onEmptyBody != null -> Resultado.Exito(onEmptyBody())
                    else -> Resultado.Error(
                        tipo = Resultado.Tipo.DESCONOCIDO,
                        mensaje = "Respuesta vacía del servidor.",
                    )
                }
            } else {
                val codigo = respuesta.code()
                val tipo = when (codigo) {
                    in 400..499 -> Resultado.Tipo.CLIENTE
                    in 500..599 -> Resultado.Tipo.SERVIDOR
                    else -> Resultado.Tipo.DESCONOCIDO
                }
                Resultado.Error(
                    tipo = tipo,
                    mensaje = respuesta.errorBody()?.string(),
                    codigoHttp = codigo,
                )
            }
        } catch (io: IOException) {
            Resultado.Error(Resultado.Tipo.RED, io.message, io)
        } catch (ex: Throwable) {
            Resultado.Error(Resultado.Tipo.DESCONOCIDO, ex.message, ex)
        }
    }
}
