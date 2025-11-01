package cl.clinipets.feature.mascotas.data

import cl.clinipets.core.Resultado
import cl.clinipets.feature.mascotas.domain.MascotasRepositorio
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.Mascota
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class MascotasRepositorioImpl @Inject constructor(
    private val remoto: MascotasRemotoDataSource,
) : MascotasRepositorio {

    override suspend fun obtenerMisMascotas(): Resultado<List<Mascota>> =
        ejecutar { remoto.obtenerMisMascotas() }

    override suspend fun obtenerMascota(id: UUID): Resultado<Mascota> =
        ejecutar { remoto.obtenerMascota(id) }

    override suspend fun crearMascota(datos: CrearMascota): Resultado<Mascota> =
        ejecutar { remoto.crearMascota(datos) }

    override suspend fun actualizarMascota(
        id: UUID,
        datos: ActualizarMascota,
    ): Resultado<Mascota> = ejecutar { remoto.actualizarMascota(id, datos) }

    override suspend fun eliminarMascota(id: UUID): Resultado<Unit> =
        ejecutar(onEmptyBody = { Unit }) { remoto.eliminarMascota(id) }

    private suspend fun <T> ejecutar(
        onEmptyBody: (() -> T)? = null,
        block: suspend () -> Response<T>,
    ): Resultado<T> {
        return try {
            val respuesta = block()
            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()
                when {
                    cuerpo != null -> Resultado.Exito(cuerpo)
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
            Resultado.Error(
                tipo = Resultado.Tipo.RED,
                mensaje = io.message,
                causa = io,
            )
        } catch (ex: Throwable) {
            Resultado.Error(
                tipo = Resultado.Tipo.DESCONOCIDO,
                mensaje = ex.message,
                causa = ex,
            )
        }
    }
}
