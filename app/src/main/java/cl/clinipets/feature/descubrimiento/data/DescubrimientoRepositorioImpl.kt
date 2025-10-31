package cl.clinipets.feature.descubrimiento.data

import cl.clinipets.core.Resultado
import cl.clinipets.feature.descubrimiento.domain.DescubrimientoRepositorio
import cl.clinipets.feature.descubrimiento.domain.FiltrosOfertas
import cl.clinipets.feature.descubrimiento.domain.FiltrosProcedimientos
import cl.clinipets.feature.descubrimiento.domain.FiltrosVeterinarios
import cl.clinipets.openapi.apis.DescubrimientoApi
import cl.clinipets.openapi.models.OfertaItem
import cl.clinipets.openapi.models.ProcedimientoItem
import cl.clinipets.openapi.models.VetItem
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DescubrimientoRepositorioImpl @Inject constructor(
    private val dataSource: DescubrimientoDataSource,
) : DescubrimientoRepositorio {

    override suspend fun buscarVeterinarios(filtros: FiltrosVeterinarios): Resultado<List<VetItem>> {
        return ejecutar {
            val request = VeterinariosRequest(
                lat = filtros.lat,
                lng = filtros.lng,
                radioKm = filtros.radioKm,
                modo = filtros.modo?.let { runCatching { DescubrimientoApi.ModoDescubrimientoBuscarVeterinarios.valueOf(it) }.getOrNull() },
                especie = filtros.especie?.let { runCatching { DescubrimientoApi.EspecieDescubrimientoBuscarVeterinarios.valueOf(it) }.getOrNull() },
                procedimientoSku = filtros.procedimientoSku,
                abiertoAhora = filtros.abiertoAhora,
                limit = filtros.limit,
                offset = filtros.offset,
            )
            dataSource.obtenerVeterinarios(request)
        }
    }

    override suspend fun buscarOfertas(filtros: FiltrosOfertas): Resultado<List<OfertaItem>> {
        return ejecutar {
            val request = OfertasRequest(
                especie = filtros.especie?.let { runCatching { DescubrimientoApi.EspecieDescubrimientoBuscarOfertas.valueOf(it) }.getOrNull() },
                procedimientoSku = filtros.procedimientoSku,
                vetId = filtros.vetId,
                activo = filtros.activo,
                lat = filtros.lat,
                lng = filtros.lng,
                radioKm = filtros.radioKm,
                limit = filtros.limit,
                offset = filtros.offset,
            )
            dataSource.obtenerOfertas(request)
        }
    }

    override suspend fun buscarProcedimientos(filtros: FiltrosProcedimientos): Resultado<List<ProcedimientoItem>> {
        return ejecutar {
            val request = ProcedimientosRequest(
                especie = filtros.especie?.let { runCatching { DescubrimientoApi.EspecieDescubrimientoBuscarProcedimientos.valueOf(it) }.getOrNull() },
                q = filtros.q,
                limit = filtros.limit,
                offset = filtros.offset,
            )
            dataSource.obtenerProcedimientos(request)
        }
    }

    private suspend fun <T> ejecutar(block: suspend () -> retrofit2.Response<T>): Resultado<T> {
        return try {
            val respuesta = block()
            if (respuesta.isSuccessful) {
                respuesta.body()?.let { Resultado.Exito(it) }
                    ?: Resultado.Error(Resultado.Tipo.DESCONOCIDO, "Respuesta vacía del servidor.")
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
