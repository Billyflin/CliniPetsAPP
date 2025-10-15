package cl.clinipets.data.api

import cl.clinipets.data.dto.CrearReserva
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
    suspend fun crearReserva(@Body body: CrearReserva)

    @GET("/api/agenda/reservas/mias")
    suspend fun reservasMias(): String
}

