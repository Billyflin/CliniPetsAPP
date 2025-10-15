package cl.clinipets.data.api

import cl.clinipets.data.dto.MiPerfilDto
import cl.clinipets.data.dto.PublicVetDto
import cl.clinipets.data.dto.UpsertPerfil
import cl.clinipets.data.dto.VerificarResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface VeterinariosApi {
    @PUT("/api/veterinarios/mi-perfil")
    suspend fun upsertMiPerfil(@Body body: UpsertPerfil): MiPerfilDto

    @GET("/api/veterinarios/publicos")
    suspend fun publicos(): List<PublicVetDto>

    @GET("/api/veterinarios/{vetId}")
    suspend fun publico(@Path("vetId") vetId: String): PublicVetDto

    @PUT("/api/veterinarios/{vetId}/verificar")
    suspend fun verificar(@Path("vetId") vetId: String): VerificarResponse
}
