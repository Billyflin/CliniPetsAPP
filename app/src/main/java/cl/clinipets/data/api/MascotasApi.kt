package cl.clinipets.data.api

import cl.clinipets.data.dto.ActualizarMascota
import cl.clinipets.data.dto.CrearMascota
import cl.clinipets.data.dto.Mascota
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MascotasApi {
    @GET("/api/mascotas/mias")
    suspend fun mias(): List<Mascota>

    @POST("/api/mascotas")
    suspend fun crear(@Body body: CrearMascota): Mascota

    @PUT("/api/mascotas/{id}")
    suspend fun actualizar(@Path("id") id: String, @Body body: ActualizarMascota): Mascota

    @DELETE("/api/mascotas/{id}")
    suspend fun eliminar(@Path("id") id: String)
}

