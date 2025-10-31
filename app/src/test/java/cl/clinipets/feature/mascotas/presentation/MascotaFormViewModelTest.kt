package cl.clinipets.feature.mascotas.presentation

import androidx.lifecycle.SavedStateHandle
import cl.clinipets.MainDispatcherRule
import cl.clinipets.core.Resultado
import cl.clinipets.feature.mascotas.domain.MascotasRepositorio
import cl.clinipets.feature.mascotas.domain.CrearMascotaUseCase
import cl.clinipets.feature.mascotas.domain.ActualizarMascotaUseCase
import cl.clinipets.feature.mascotas.domain.ObtenerMascotaUseCase
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.navigation.AppDestination
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MascotaFormViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `guardar crea mascota y emite evento`() = runTest(dispatcherRule.testDispatcher) {
        val idNuevo = UUID.randomUUID()
        val repo = FakeMascotasRepositorio(
            crear = Resultado.Exito(crearMascota(idNuevo)),
        )
        val viewModel = MascotaFormViewModel(
            savedStateHandle = SavedStateHandle(),
            crearMascota = CrearMascotaUseCase(repo),
            actualizarMascota = ActualizarMascotaUseCase(repo),
            obtenerMascota = ObtenerMascotaUseCase(repo),
        )

        viewModel.onNombreChange("Bobby")
        viewModel.onEspecieChange(CrearMascota.Especie.PERRO)
        viewModel.guardar()

        val evento = viewModel.eventos.first()
        assertTrue(evento is MascotaFormEvento.MascotaGuardada)
        assertEquals(idNuevo, (evento as MascotaFormEvento.MascotaGuardada).id)
        assertTrue(!evento.fueEdicion)
    }

    @Test
    fun `si falta especie marca error`() = runTest(dispatcherRule.testDispatcher) {
        val repo = FakeMascotasRepositorio()
        val viewModel = MascotaFormViewModel(
            savedStateHandle = SavedStateHandle(),
            crearMascota = CrearMascotaUseCase(repo),
            actualizarMascota = ActualizarMascotaUseCase(repo),
            obtenerMascota = ObtenerMascotaUseCase(repo),
        )

        viewModel.onNombreChange("Bobby")
        viewModel.guardar()

        val estado = viewModel.estado.value
        assertNotNull(estado.especieError)
    }

    @Test
    fun `al editar carga datos existentes`() = runTest(dispatcherRule.testDispatcher) {
        val id = UUID.randomUUID()
        val repo = FakeMascotasRepositorio(
            detalle = Resultado.Exito(crearMascota(id)),
            actualizar = Resultado.Exito(crearMascota(id)),
        )
        val viewModel = MascotaFormViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AppDestination.MascotaEditar.ARG_ID to id.toString())),
            crearMascota = CrearMascotaUseCase(repo),
            actualizarMascota = ActualizarMascotaUseCase(repo),
            obtenerMascota = ObtenerMascotaUseCase(repo),
        )

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals("Bobby", estado.nombre)
        assertEquals(CrearMascota.Especie.PERRO, estado.especie)

        viewModel.onNombreChange("Bobby 2")
        viewModel.guardar()
        val evento = viewModel.eventos.first()
        assertTrue((evento as MascotaFormEvento.MascotaGuardada).fueEdicion)
    }

    private fun crearMascota(id: UUID): Mascota {
        val ahora = OffsetDateTime.parse("2024-01-01T12:00:00Z")
        return Mascota(
            id = id,
            nombre = "Bobby",
            especie = Mascota.Especie.PERRO,
            creadoEn = ahora,
            modificadoEn = ahora,
        )
    }

    private class FakeMascotasRepositorio(
        private val crear: Resultado<Mascota> = Resultado.Error(Resultado.Tipo.DESCONOCIDO),
        private val actualizar: Resultado<Mascota> = Resultado.Error(Resultado.Tipo.DESCONOCIDO),
        private val detalle: Resultado<Mascota> = Resultado.Error(Resultado.Tipo.DESCONOCIDO),
    ) : MascotasRepositorio {
        override suspend fun obtenerMisMascotas(): Resultado<List<Mascota>> = Resultado.Exito(emptyList())

        override suspend fun obtenerMascota(id: UUID): Resultado<Mascota> = detalle

        override suspend fun crearMascota(datos: CrearMascota): Resultado<Mascota> = crear

        override suspend fun actualizarMascota(id: UUID, datos: ActualizarMascota): Resultado<Mascota> = actualizar

        override suspend fun eliminarMascota(id: UUID): Resultado<Unit> = Resultado.Exito(Unit)
    }
}
