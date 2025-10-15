package cl.clinipets.data.api

import cl.clinipets.data.dto.CrearReserva
import cl.clinipets.data.dto.ReservaDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AgendaApi {
    @GET("/api/agenda/disponibilidad/slots")
    suspend fun slots(
        @Query("vetId") vetId: String,
        @Query("from") fromIso: String,
        @Query("to") toIso: String,
        @Query("ofertaId") ofertaId: String? = null
    ): String

    @POST("/api/agenda/reservas")
    suspend fun crearReserva(@Body body: CrearReserva): ReservaDto

    @GET("/api/agenda/reservas/mias")
    suspend fun reservasMias(): List<ReservaDto>

    @GET("/api/agenda/reservas/{reservaId}")
    suspend fun detalle(@Path("reservaId") reservaId: String): ReservaDto

    @PUT("/api/agenda/reservas/{reservaId}/aceptar")
    suspend fun aceptar(@Path("reservaId") reservaId: String): ReservaDto

    @PUT("/api/agenda/reservas/{reservaId}/rechazar")
    suspend fun rechazar(@Path("reservaId") reservaId: String): ReservaDto

    @PUT("/api/agenda/reservas/{reservaId}/cancelar")
    suspend fun cancelar(@Path("reservaId") reservaId: String): ReservaDto

    @PUT("/api/agenda/reservas/{reservaId}/completar")
    suspend fun completar(@Path("reservaId") reservaId: String): ReservaDto
}
