package cl.clinipets.data.api

import cl.clinipets.data.dto.UpsertPerfil
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface VeterinariosApi {
    @PUT("/api/veterinarios/mi-perfil")
    suspend fun upsertMiPerfil(@Body body: UpsertPerfil): String

    @GET("/api/veterinarios/publicos")
    suspend fun publicos(): String

    @PUT("/api/veterinarios/{vetId}/verificar")
    suspend fun verificar(@Path("vetId") vetId: String): String
}

