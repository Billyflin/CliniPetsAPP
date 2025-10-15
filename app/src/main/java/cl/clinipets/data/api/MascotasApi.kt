package cl.clinipets.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MascotasApi {
    @GET("/api/mascotas/mias")
    suspend fun mias(): List<cl.clinipets.data.dto.Mascota>

    @POST("/api/mascotas")
    suspend fun crear(@Body body: cl.clinipets.data.dto.CrearMascota): cl.clinipets.data.dto.Mascota

    @PUT("/api/mascotas/{id}")
    suspend fun actualizar(@Path("id") id: String, @Body body: cl.clinipets.data.dto.ActualizarMascota): cl.clinipets.data.dto.Mascota

    @DELETE("/api/mascotas/{id}")
    suspend fun eliminar(@Path("id") id: String)
}
