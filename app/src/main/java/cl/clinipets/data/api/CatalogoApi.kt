package cl.clinipets.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface CatalogoApi {
    @GET("/api/catalogo/ofertas")
    suspend fun ofertas(
        @Query("vetId") vetId: String? = null,
        @Query("procedimientoId") procedimientoId: String? = null,
        @Query("activo") activo: Boolean? = true
    ): String
}

