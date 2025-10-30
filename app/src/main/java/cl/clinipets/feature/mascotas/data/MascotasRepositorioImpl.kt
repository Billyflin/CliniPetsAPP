package cl.clinipets.feature.mascotas.data

import cl.clinipets.core.Resultado
import cl.clinipets.feature.mascotas.domain.MascotasRepositorio
import cl.clinipets.openapi.models.Mascota
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MascotasRepositorioImpl @Inject constructor(
    private val remoto: MascotasRemotoDataSource,
) : MascotasRepositorio {

    override suspend fun obtenerMisMascotas(): Resultado<List<Mascota>> {
        return try {
            val respuesta = remoto.obtenerMisMascotas()
            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()
                if (cuerpo == null) {
                    Resultado.Error(
                        tipo = Resultado.Tipo.DESCONOCIDO,
                        mensaje = "Respuesta vacía del servidor.",
                    )
                } else {
                    Resultado.Exito(cuerpo)
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
