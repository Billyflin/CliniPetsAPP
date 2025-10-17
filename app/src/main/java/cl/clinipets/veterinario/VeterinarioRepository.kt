package cl.clinipets.veterinario

import android.content.Context
import cl.clinipets.network.CreateVeterinarioRequest
import cl.clinipets.network.NetworkModule
import cl.clinipets.network.VeterinarioPerfil

class VeterinarioRepository(context: Context) {
    private val api = NetworkModule.provideApiService(context)

    suspend fun crearVeterinario(req: CreateVeterinarioRequest): Result<VeterinarioPerfil> = try {
        Result.success(api.crearVeterinario(req))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun miPerfil(): Result<VeterinarioPerfil> = try {
        Result.success(api.miPerfilVeterinario())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarMiPerfil(req: CreateVeterinarioRequest): Result<VeterinarioPerfil> = try {
        Result.success(api.actualizarMiPerfilVeterinario(req))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun verificarVet(id: String, verificado: Boolean): Result<VeterinarioPerfil> = try {
        Result.success(api.verificarVeterinario(id, mapOf("verificado" to verificado)))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

