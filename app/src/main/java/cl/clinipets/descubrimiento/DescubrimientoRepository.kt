package cl.clinipets.descubrimiento

import android.content.Context
import cl.clinipets.network.ApiService
import cl.clinipets.network.NetworkModule
import cl.clinipets.network.Oferta
import cl.clinipets.network.Procedimiento
import cl.clinipets.network.VetItem

class DescubrimientoRepository(context: Context) {
    private val api: ApiService = NetworkModule.provideApiService(context)

    suspend fun veterinarios(
        lat: Double?, lng: Double?, radioKm: Int?, modo: String?, especie: String?, procedimientoSku: String?, abiertoAhora: Boolean?, limit: Int? = 50, offset: Int? = 0
    ): Result<List<VetItem>> = try {
        Result.success(api.veterinarios(lat, lng, radioKm, modo, especie, procedimientoSku, abiertoAhora, limit, offset))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun procedimientos(especie: String?, q: String?, limit: Int? = 50, offset: Int? = 0): Result<List<Procedimiento>> = try {
        Result.success(api.procedimientos(especie, q, limit, offset))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun ofertas(especie: String?, procedimientoSku: String?, vetId: String?, activo: Boolean? = true, lat: Double? = null, lng: Double? = null, radioKm: Int? = null, limit: Int? = 50, offset: Int? = 0): Result<List<Oferta>> = try {
        Result.success(api.ofertas(especie, procedimientoSku, vetId, activo, lat, lng, radioKm, limit, offset))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

