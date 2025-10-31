package cl.clinipets.feature.veterinario.presentation

import cl.clinipets.MainDispatcherRule
import cl.clinipets.core.Resultado
import cl.clinipets.feature.veterinario.domain.VeterinarioRepositorio
import cl.clinipets.feature.veterinario.domain.RegistrarVeterinarioUseCase
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import cl.clinipets.openapi.models.VeterinarioPerfil
import cl.clinipets.openapi.models.ReglaSemanal
import cl.clinipets.openapi.models.ExcepcionDisponibilidad
import cl.clinipets.openapi.models.BloqueHorario
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VeterinarioOnboardingViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `registro exitoso emite evento`() = runTest(dispatcherRule.testDispatcher) {
        val perfil = VeterinarioPerfil(
            id = UUID.randomUUID(),
            nombreCompleto = "Dr Vet",
            modosAtencion = emptySet(),
            verificado = false,
        )
        val repo = FakeVeterinarioRepositorio(Resultado.Exito(perfil))
        val viewModel = VeterinarioOnboardingViewModel(RegistrarVeterinarioUseCase(repo))

        val eventoDeferred = async { viewModel.eventos.first() }

        viewModel.onNombreChange("Dr Vet")
        viewModel.registrar()

        val evento = eventoDeferred.await()

        assertTrue(evento is VeterinarioOnboardingEvento.RegistroCompleto)
        assertEquals(perfil.id, (evento as VeterinarioOnboardingEvento.RegistroCompleto).perfilId)
    }

    @Test
    fun `falta nombre marca error`() = runTest(dispatcherRule.testDispatcher) {
        val repo = FakeVeterinarioRepositorio(Resultado.Error(Resultado.Tipo.CLIENTE))
        val viewModel = VeterinarioOnboardingViewModel(RegistrarVeterinarioUseCase(repo))

        viewModel.registrar()
        val estado = viewModel.estado.value
        assertNotNull(estado.nombreError)
    }

    private class FakeVeterinarioRepositorio(
        private val resultado: Resultado<VeterinarioPerfil>,
    ) : VeterinarioRepositorio {
        override suspend fun registrar(request: RegistrarVeterinarioRequest): Resultado<VeterinarioPerfil> = resultado

        override suspend fun obtenerMiPerfil(): Resultado<VeterinarioPerfil> =
            throw UnsupportedOperationException("No se usa en esta prueba")

        override suspend fun actualizarPerfil(request: cl.clinipets.openapi.models.ActualizarPerfilRequest): Resultado<VeterinarioPerfil> =
            throw UnsupportedOperationException("No se usa en esta prueba")

        override suspend fun crearReglaSemanal(
            veterinarioId: UUID,
            request: cl.clinipets.openapi.models.CrearReglaSemanal,
        ): Resultado<ReglaSemanal> = throw UnsupportedOperationException("No se usa en esta prueba")

        override suspend fun eliminarReglaSemanal(reglaId: UUID): Resultado<Unit> = throw UnsupportedOperationException("No se usa en esta prueba")

        override suspend fun crearExcepcion(
            veterinarioId: UUID,
            request: cl.clinipets.openapi.models.CrearExcepcion,
        ): Resultado<ExcepcionDisponibilidad> = throw UnsupportedOperationException("No se usa en esta prueba")

        override suspend fun obtenerDisponibilidad(
            veterinarioId: UUID,
            fecha: LocalDate,
        ): Resultado<List<BloqueHorario>> = throw UnsupportedOperationException("No se usa en esta prueba")
    }

}
