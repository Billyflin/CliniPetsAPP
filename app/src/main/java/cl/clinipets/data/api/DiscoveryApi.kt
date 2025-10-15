package cl.clinipets.data.api

import cl.clinipets.data.dto.discovery.DiscoveryResult
import retrofit2.http.GET
import retrofit2.http.Query

interface DiscoveryApi {
    @GET("/api/publico/descubrimiento")
    suspend fun buscar(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radio") radio: Int,
        @Query("procedimientoId") procId: String? = null,
        @Query("abiertoAhora") abierto: Boolean? = null,
        @Query("conStock") conStock: Boolean? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): DiscoveryResult
}
