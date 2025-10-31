package cl.clinipets.feature.veterinario.presentation

import cl.clinipets.MainDispatcherRule
import cl.clinipets.core.Resultado
import cl.clinipets.feature.veterinario.domain.VeterinarioRepositorio
import cl.clinipets.feature.veterinario.domain.CrearExcepcionUseCase
import cl.clinipets.feature.veterinario.domain.CrearReglaSemanalUseCase
import cl.clinipets.feature.veterinario.domain.EliminarReglaSemanalUseCase
import cl.clinipets.feature.veterinario.domain.ObtenerDisponibilidadUseCase
import cl.clinipets.feature.veterinario.domain.ObtenerPerfilVeterinarioUseCase
import cl.clinipets.openapi.models.BloqueHorario
import cl.clinipets.openapi.models.CrearExcepcion
import cl.clinipets.openapi.models.CrearReglaSemanal
import cl.clinipets.openapi.models.ExcepcionDisponibilidad
import cl.clinipets.openapi.models.ReglaSemanal
import cl.clinipets.openapi.models.Veterinario
import cl.clinipets.openapi.models.VeterinarioPerfil
import java.time.OffsetDateTime
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VeterinarioAgendaViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `crear regla agrega entrada a la lista`() = runTest(dispatcherRule.testDispatcher) {
        val vetId = UUID.randomUUID()
        val repo = FakeVeterinarioRepositorio(
            perfil = Resultado.Exito(crearPerfil(vetId)),
            regla = Resultado.Exito(crearRegla(vetId)),
        )
        val viewModel = VeterinarioAgendaViewModel(
            obtenerPerfil = ObtenerPerfilVeterinarioUseCase(repo),
            crearReglaSemanal = CrearReglaSemanalUseCase(repo),
            eliminarReglaSemanal = EliminarReglaSemanalUseCase(repo),
            crearExcepcion = CrearExcepcionUseCase(repo),
            obtenerDisponibilidad = ObtenerDisponibilidadUseCase(repo),
        )

        advanceUntilIdle()
        viewModel.onReglaDiaChange(CrearReglaSemanal.DiaSemana.MONDAY)
        viewModel.onReglaHoraInicioChange("09:00")
        viewModel.onReglaHoraFinChange("10:00")
        viewModel.crearRegla()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(1, estado.reglas.size)
    }

    private fun crearPerfil(id: UUID): VeterinarioPerfil = VeterinarioPerfil(
        id = id,
        nombreCompleto = "Dr Vet",
        modosAtencion = emptySet(),
        verificado = true,
    )

    private fun crearRegla(vetId: UUID): ReglaSemanal {
        val ahora = OffsetDateTime.parse("2024-01-01T10:00:00Z")
        return ReglaSemanal(
            id = UUID.randomUUID(),
            veterinario = Veterinario(
                id = vetId,
                nombre = "Dr Vet",
                email = "vet@clinipets.cl",
                roles = emptySet(),
                creadoEn = ahora,
                modificadoEn = ahora,
            ),
            diaSemana = ReglaSemanal.DiaSemana.MONDAY,
            horaInicio = "09:00",
            horaFin = "10:00",
            activa = true,
            creadoEn = ahora,
            modificadoEn = ahora,
        )
    }

    private class FakeVeterinarioRepositorio(
        private val perfil: Resultado<VeterinarioPerfil> = Resultado.Error(Resultado.Tipo.DESCONOCIDO),
        private val regla: Resultado<ReglaSemanal> = Resultado.Error(Resultado.Tipo.DESCONOCIDO),
    ) : VeterinarioRepositorio {
        override suspend fun registrar(request: cl.clinipets.openapi.models.RegistrarVeterinarioRequest): Resultado<VeterinarioPerfil> = perfil

        override suspend fun obtenerMiPerfil(): Resultado<VeterinarioPerfil> = perfil

        override suspend fun actualizarPerfil(request: cl.clinipets.openapi.models.ActualizarPerfilRequest): Resultado<VeterinarioPerfil> = perfil

        override suspend fun crearReglaSemanal(veterinarioId: UUID, request: CrearReglaSemanal): Resultado<ReglaSemanal> = regla

        override suspend fun eliminarReglaSemanal(reglaId: UUID): Resultado<Unit> = Resultado.Exito(Unit)

        override suspend fun crearExcepcion(veterinarioId: UUID, request: CrearExcepcion): Resultado<ExcepcionDisponibilidad> =
            Resultado.Error(Resultado.Tipo.DESCONOCIDO)

        override suspend fun obtenerDisponibilidad(veterinarioId: UUID, fecha: LocalDate): Resultado<List<BloqueHorario>> =
            Resultado.Exito(emptyList())
    }
}
