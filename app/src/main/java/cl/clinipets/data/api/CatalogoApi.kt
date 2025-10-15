package cl.clinipets.data.api

import cl.clinipets.data.dto.OfertaDto
import cl.clinipets.data.dto.ProcedimientoDto
import cl.clinipets.data.dto.UpsertOferta
import cl.clinipets.data.dto.UpsertProcedimiento
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogoApi {
    @GET("/api/catalogo/ofertas")
    suspend fun ofertas(
        @Query("vetId") vetId: String? = null,
        @Query("procedimientoId") procedimientoId: String? = null,
        @Query("activo") activo: Boolean? = true
    ): List<OfertaDto>

    @POST("/api/catalogo/ofertas")
    suspend fun crearOferta(@Body body: UpsertOferta): OfertaDto

    @PUT("/api/catalogo/ofertas/{ofertaId}")
    suspend fun editarOferta(
        @Path("ofertaId") ofertaId: String,
        @Body body: UpsertOferta
    ): OfertaDto

    @DELETE("/api/catalogo/ofertas/{ofertaId}")
    suspend fun borrarOferta(@Path("ofertaId") ofertaId: String)

    @GET("/api/catalogo/procedimientos")
    suspend fun procedimientos(
        @Query("q") q: String? = null,
        @Query("categoria") categoria: String? = null
    ): List<ProcedimientoDto>

    @POST("/api/catalogo/procedimientos")
    suspend fun crearProcedimiento(@Body body: UpsertProcedimiento): ProcedimientoDto

    @PUT("/api/catalogo/procedimientos/{id}")
    suspend fun editarProcedimiento(
        @Path("id") id: String,
        @Body body: UpsertProcedimiento
    ): ProcedimientoDto

    @DELETE("/api/catalogo/procedimientos/{id}")
    suspend fun borrarProcedimiento(@Path("id") id: String)
}
