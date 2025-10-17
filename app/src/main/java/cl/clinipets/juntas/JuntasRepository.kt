package cl.clinipets.juntas

import android.content.Context
import cl.clinipets.network.CrearJuntaRequest
import cl.clinipets.network.FinalizarJuntaRequest
import cl.clinipets.network.Junta
import cl.clinipets.network.NetworkModule

class JuntasRepository(context: Context) {
    private val api = NetworkModule.provideApiService(context)

    suspend fun crearJunta(reservaId: String): Result<Junta> = try {
        val req = CrearJuntaRequest(reservaId)
        Result.success(api.crearJunta(req))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cambiarEstado(juntaId: String, estado: String): Result<Unit> = try {
        api.cambiarEstadoJunta(juntaId, cl.clinipets.network.CambioEstadoJuntaRequest(estado))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarUbicacion(juntaId: String, lat: Double, lng: Double): Result<Unit> = try {
        api.actualizarUbicacionJunta(juntaId, cl.clinipets.network.ActualizarUbicacionJuntaRequest(latitud = lat, longitud = lng))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun finalizarJunta(juntaId: String, notas: String?): Result<Unit> = try {
        api.finalizarJunta(juntaId, FinalizarJuntaRequest(notas))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerJunta(juntaId: String): Result<Junta> = try {
        Result.success(api.obtenerJunta(juntaId))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
