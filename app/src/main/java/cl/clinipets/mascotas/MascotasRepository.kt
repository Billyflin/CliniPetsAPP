package cl.clinipets.mascotas

import android.content.Context
import cl.clinipets.network.ApiService
import cl.clinipets.network.CrearMascotaRequest
import cl.clinipets.network.ActualizarMascotaRequest
import cl.clinipets.network.Mascota
import cl.clinipets.network.NetworkModule

class MascotasRepository(context: Context) {
    private val api: ApiService = NetworkModule.provideApiService(context)

    suspend fun misMascotas(): Result<List<Mascota>> = try {
        Result.success(api.misMascotas())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun crearMascota(nombre: String, especie: String): Result<Mascota> = try {
        val req = CrearMascotaRequest(nombre = nombre, especie = especie)
        Result.success(api.crearMascota(req))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun editarMascota(id: String, nombre: String, especie: String): Result<Mascota> = try {
        val req = ActualizarMascotaRequest(nombre = nombre)
        Result.success(api.editarMascota(id, req))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun eliminarMascota(id: String): Result<Unit> = try {
        api.eliminarMascota(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
