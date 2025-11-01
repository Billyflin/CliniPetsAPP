package cl.clinipets.feature.veterinario.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.ActualizarPerfilRequest
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import cl.clinipets.openapi.models.VeterinarioPerfil
import cl.clinipets.openapi.models.CrearReglaSemanal
import cl.clinipets.openapi.models.ReglaSemanal
import cl.clinipets.openapi.models.CrearExcepcion
import cl.clinipets.openapi.models.ExcepcionDisponibilidad
import cl.clinipets.openapi.models.BloqueHorario
import java.time.LocalDate
import java.util.UUID

interface VeterinarioRepositorio {
    suspend fun registrar(request: RegistrarVeterinarioRequest): Resultado<VeterinarioPerfil>
    suspend fun obtenerMiPerfil(): Resultado<VeterinarioPerfil>
    suspend fun actualizarPerfil(request: ActualizarPerfilRequest): Resultado<VeterinarioPerfil>
    suspend fun crearReglaSemanal(veterinarioId: UUID, request: CrearReglaSemanal): Resultado<ReglaSemanal>
    suspend fun eliminarReglaSemanal(reglaId: UUID): Resultado<Unit>
    suspend fun crearExcepcion(veterinarioId: UUID, request: CrearExcepcion): Resultado<ExcepcionDisponibilidad>
    suspend fun obtenerDisponibilidad(veterinarioId: UUID, fecha: LocalDate): Resultado<List<BloqueHorario>>
}
