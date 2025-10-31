package cl.clinipets.feature.veterinario.data

import cl.clinipets.openapi.apis.DisponibilidadApi
import cl.clinipets.openapi.models.CrearExcepcion
import cl.clinipets.openapi.models.CrearReglaSemanal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisponibilidadRemoteDataSource @Inject constructor(
    private val api: DisponibilidadApi,
) {
    suspend fun crearRegla(veterinarioId: UUID, request: CrearReglaSemanal) =
        api.crearReglaSemanal(veterinarioId, request)

    suspend fun eliminarRegla(reglaId: UUID) = api.eliminarReglaSemanal(reglaId)

    suspend fun crearExcepcion(veterinarioId: UUID, request: CrearExcepcion) =
        api.crearExcepcion(veterinarioId, request)

    suspend fun obtenerDisponibilidad(veterinarioId: UUID, fecha: LocalDate) =
        api.obtenerDisponibilidad(veterinarioId, fecha)
}
