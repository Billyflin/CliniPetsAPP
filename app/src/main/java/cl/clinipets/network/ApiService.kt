package cl.clinipets.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// --- Auth ---
data class GoogleAuthRequest(val idToken: String)
data class UsuarioInfo(val id: String, val email: String, val nombre: String?, val fotoUrl: String?, val roles: List<String>)
data class TokenResponse(val token: String, val usuario: UsuarioInfo)
// Respuesta de /api/auth/me
data class MeResponse(val authenticated: Boolean, val id: String?, val email: String?, val roles: List<String>?)
// Respuesta de /api/auth/refresh
data class RefreshResponse(val token: String)

// --- Mascotas ---
data class Mascota(
    val id: String?,
    val nombre: String,
    val especie: String,
    val raza: String?,
    val sexo: String?,
    val fechaNacimiento: String?,
    val pesoKg: Double?,
    val tutor: Usuario?,
)
data class CrearMascotaRequest(
    val nombre: String,
    val especie: String,
    val raza: String? = null,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null,
)
data class ActualizarMascotaRequest(
    val nombre: String? = null,
    val raza: String? = null,
    val sexo: String? = null,
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null,
)

// --- Descubrimiento ---
data class VetItem(
    val id: String,
    val nombre: String,
    val fotoUrl: String?,
    val modos: List<String>,
    val verificado: Boolean,
    val distanciaKm: Double?,
    val lat: Double?,
    val lng: Double?
)
data class Procedimiento(val sku: String, val nombre: String, val duracionMinutos: Int, val especies: List<String>)
data class Oferta(val id: String, val procedimientoSku: String, val procedimientoNombre: String, val precio: Double, val duracionMinutos: Int, val compatibleEspecies: List<String>, val distanciaKm: Double?)

// --- Disponibilidad ---
data class Bloque(val inicio: String, val fin: String)
data class ReglaDisponibilidadRequest(val diaSemana: Int, val horaInicio: String, val horaFin: String)
data class ExcepcionDisponibilidadRequest(val fecha: String, val tipo: String, val horaInicio: String?, val horaFin: String?, val motivo: String?)

// --- Core tipos de usuario/veterinario para respuestas ---
data class Usuario(val id: String?, val email: String?, val nombre: String?)
data class Veterinario(val id: String?, val nombreCompleto: String?)

// --- Reservas ---
data class CrearReservaRequest(
    val mascotaId: String,
    val veterinarioId: String,
    val procedimientoSku: String,
    val inicio: String,
    val modo: String,
    val direccionAtencion: String?,
    val notas: String?
)
data class Reserva(
    val id: String,
    val cliente: Usuario,
    val mascota: Mascota,
    val veterinario: Veterinario,
    val procedimientoSku: String,
    val inicio: String,
    val fin: String,
    val modo: String,
    val estado: String,
    val direccionAtencion: String?,
    val notas: String?
)

data class CambioEstadoReservaRequest(val nuevoEstado: String, val motivo: String?)

// --- Veterinario ---
data class CreateVeterinarioRequest(
    val nombreCompleto: String,
    val numeroLicencia: String?,
    val modosAtencion: List<String>,
    val lat: Double?,
    val lng: Double?,
    val radioCoberturaKm: Int?
)
data class VeterinarioPerfil(val id: String, val nombreCompleto: String, val verificado: Boolean, val modosAtencion: List<String>, val lat: Double?, val lng: Double?, val radioCoberturaKm: Int?)

// --- Juntas ---
data class CrearJuntaRequest(val reservaId: String)
data class Junta(val id: String, val reserva: Reserva, val estado: String?)
data class CambioEstadoJuntaRequest(val estado: String)
data class ActualizarUbicacionJuntaRequest(val latitud: Double, val longitud: Double)
data class FinalizarJuntaRequest(val notas: String?)

// --- Clinica ---
data class SugerenciasDiagnosticoRequest(val sintomas: List<String>)
data class SugerenciasTratamientoRequest(val diagnostico: String)

// --- Catalogo ---
data class CatalogoProcedimientoRequest(val sku: String, val nombre: String, val duracionMinutos: Int, val especies: List<String>)
data class CatalogoOfertaRequest(val id: String?, val procedimientoSku: String, val precio: Double, val duracionMinutos: Int, val compatibleEspecies: List<String>, val activo: Boolean)

// --- Inventario ---
data class InventarioVerificarRequest(val items: List<String>)
data class InventarioReservarRequest(val items: List<String>)

interface ApiService {
    // Auth
    @POST("/api/auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleAuthRequest): TokenResponse

    @GET("/api/auth/me")
    suspend fun me(): MeResponse

    @POST("/api/auth/refresh")
    suspend fun refresh(): RefreshResponse

    @POST("/api/auth/logout")
    suspend fun logout(): Map<String, Any>

    // Mascotas
    @GET("/api/mascotas/mias")
    suspend fun misMascotas(): List<Mascota>

    @POST("/api/mascotas")
    suspend fun crearMascota(@Body body: CrearMascotaRequest): Mascota

    @PUT("/api/mascotas/{id}")
    suspend fun editarMascota(@Path("id") id: String, @Body body: ActualizarMascotaRequest): Mascota

    @DELETE("/api/mascotas/{id}")
    suspend fun eliminarMascota(@Path("id") id: String)

    // Descubrimiento
    @GET("/api/descubrimiento/veterinarios")
    suspend fun veterinarios(
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
        @Query("radioKm") radioKm: Int?,
        @Query("modo") modo: String?,
        @Query("especie") especie: String?,
        @Query("procedimientoSku") procedimientoSku: String?,
        @Query("abiertoAhora") abiertoAhora: Boolean?,
        @Query("limit") limit: Int?,
        @Query("offset") offset: Int?
    ): List<VetItem>

    @GET("/api/descubrimiento/procedimientos")
    suspend fun procedimientos(
        @Query("especie") especie: String?,
        @Query("q") q: String?,
        @Query("limit") limit: Int?,
        @Query("offset") offset: Int?
    ): List<Procedimiento>

    @GET("/api/descubrimiento/ofertas")
    suspend fun ofertas(
        @Query("especie") especie: String?,
        @Query("procedimientoSku") procedimientoSku: String?,
        @Query("vetId") vetId: String?,
        @Query("activo") activo: Boolean?,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
        @Query("radioKm") radioKm: Int?,
        @Query("limit") limit: Int?,
        @Query("offset") offset: Int?
    ): List<Oferta>

    // Disponibilidad
    @GET("/api/disponibilidad/veterinario/{vId}")
    suspend fun disponibilidadVeterinario(@Path("vId") vId: String, @Query("fecha") fecha: String): List<Bloque>

    @POST("/api/disponibilidad/veterinario/{vId}/reglas")
    suspend fun crearReglaDisponibilidad(@Path("vId") vId: String, @Body body: ReglaDisponibilidadRequest)

    @DELETE("/api/disponibilidad/reglas/{reglaId}")
    suspend fun eliminarReglaDisponibilidad(@Path("reglaId") reglaId: String)

    @POST("/api/disponibilidad/veterinario/{vId}/excepciones")
    suspend fun crearExcepcionDisponibilidad(@Path("vId") vId: String, @Body body: ExcepcionDisponibilidadRequest)

    // Reservas
    @POST("/api/reservas")
    suspend fun crearReserva(@Body body: CrearReservaRequest): Reserva

    @GET("/api/reservas/mias")
    suspend fun misReservas(): List<Reserva>

    @GET("/api/reservas/veterinario/{vId}")
    suspend fun reservasPorVeterinario(@Path("vId") vId: String): List<Reserva>

    @PATCH("/api/reservas/{id}/estado")
    suspend fun cambiarEstadoReserva(@Path("id") id: String, @Body body: CambioEstadoReservaRequest)

    // Veterinario
    @POST("/api/veterinarios")
    suspend fun crearVeterinario(@Body body: CreateVeterinarioRequest): VeterinarioPerfil

    @GET("/api/veterinarios/mi-perfil")
    suspend fun miPerfilVeterinario(): VeterinarioPerfil

    @PUT("/api/veterinarios/mi-perfil")
    suspend fun actualizarMiPerfilVeterinario(@Body body: CreateVeterinarioRequest): VeterinarioPerfil

    @PUT("/api/veterinarios/{id}/verificar")
    suspend fun verificarVeterinario(@Path("id") id: String, @Body body: Map<String, Boolean>): VeterinarioPerfil

    // Juntas
    @POST("/api/juntas")
    suspend fun crearJunta(@Body body: CrearJuntaRequest): Junta

    @PATCH("/api/juntas/{id}/estado")
    suspend fun cambiarEstadoJunta(@Path("id") id: String, @Body body: CambioEstadoJuntaRequest)

    @PATCH("/api/juntas/{id}/ubicacion")
    suspend fun actualizarUbicacionJunta(@Path("id") id: String, @Body body: ActualizarUbicacionJuntaRequest)

    @POST("/api/juntas/{id}/finalizar")
    suspend fun finalizarJunta(@Path("id") id: String, @Body body: FinalizarJuntaRequest)

    @GET("/api/juntas/{id}")
    suspend fun obtenerJunta(@Path("id") id: String): Junta

    // Clinica
    @POST("/api/clinica/sugerencias/diagnostico")
    suspend fun sugerenciasDiagnostico(@Body body: SugerenciasDiagnosticoRequest): List<String>

    @POST("/api/clinica/sugerencias/tratamiento")
    suspend fun sugerenciasTratamiento(@Body body: SugerenciasTratamientoRequest): List<String>

    // Catalogo
    @POST("/api/catalogo/procedimientos")
    suspend fun crearProcedimiento(@Body body: CatalogoProcedimientoRequest)

    @PUT("/api/catalogo/procedimientos/{sku}")
    suspend fun editarProcedimiento(@Path("sku") sku: String, @Body body: CatalogoProcedimientoRequest)

    @DELETE("/api/catalogo/procedimientos/{sku}")
    suspend fun eliminarProcedimiento(@Path("sku") sku: String)

    @POST("/api/catalogo/ofertas")
    suspend fun crearOferta(@Body body: CatalogoOfertaRequest)

    @PUT("/api/catalogo/ofertas/{id}")
    suspend fun editarOferta(@Path("id") id: String, @Body body: CatalogoOfertaRequest)

    @DELETE("/api/catalogo/ofertas/{id}")
    suspend fun eliminarOferta(@Path("id") id: String)

    // Inventario (demo)
    @POST("/api/inventario/verificar")
    suspend fun verificarInventario(@Body body: InventarioVerificarRequest)

    @POST("/api/inventario/reservar")
    suspend fun reservarInventario(@Body body: InventarioReservarRequest)
}
