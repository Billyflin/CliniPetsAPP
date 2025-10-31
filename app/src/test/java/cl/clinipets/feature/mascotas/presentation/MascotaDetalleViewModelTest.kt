package cl.clinipets.feature.mascotas.presentation

import androidx.lifecycle.SavedStateHandle
import cl.clinipets.MainDispatcherRule
import cl.clinipets.core.Resultado
import cl.clinipets.feature.mascotas.domain.MascotasRepositorio
import cl.clinipets.feature.mascotas.domain.ObtenerMascotaUseCase
import cl.clinipets.feature.mascotas.domain.EliminarMascotaUseCase
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.navigation.AppDestination
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MascotaDetalleViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `al iniciar carga la mascota`() = runTest(dispatcherRule.testDispatcher) {
        val id = UUID.randomUUID()
        val mascota = crearMascota(id)
        val repo = FakeMascotasRepositorio(
            detalle = Resultado.Exito(mascota),
        )
        val viewModel = MascotaDetalleViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AppDestination.MascotaDetalle.ARG_ID to id.toString())),
            obtenerMascota = ObtenerMascotaUseCase(repo),
            eliminarMascota = EliminarMascotaUseCase(repo),
        )

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(mascota, estado.mascota)
        assertTrue(!estado.cargando)
    }

    @Test
    fun `al eliminar emite evento`() = runTest(dispatcherRule.testDispatcher) {
        val id = UUID.randomUUID()
        val mascota = crearMascota(id)
        val repo = FakeMascotasRepositorio(
            detalle = Resultado.Exito(mascota),
            eliminar = Resultado.Exito(Unit),
        )
        val viewModel = MascotaDetalleViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AppDestination.MascotaDetalle.ARG_ID to id.toString())),
            obtenerMascota = ObtenerMascotaUseCase(repo),
            eliminarMascota = EliminarMascotaUseCase(repo),
        )

        advanceUntilIdle()

        viewModel.eliminarMascota()
        val evento = viewModel.eventos.first()

        assertTrue(evento is MascotaDetalleEvento.MascotaEliminada)
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
        private val detalle: Resultado<Mascota>,
        private val eliminar: Resultado<Unit> = Resultado.Exito(Unit),
    ) : MascotasRepositorio {
        override suspend fun obtenerMisMascotas(): Resultado<List<Mascota>> = Resultado.Exito(emptyList())

        override suspend fun obtenerMascota(id: UUID): Resultado<Mascota> = detalle

        override suspend fun crearMascota(datos: cl.clinipets.openapi.models.CrearMascota): Resultado<Mascota> =
            throw NotImplementedError()

        override suspend fun actualizarMascota(
            id: UUID,
            datos: cl.clinipets.openapi.models.ActualizarMascota,
        ): Resultado<Mascota> = throw NotImplementedError()

        override suspend fun eliminarMascota(id: UUID): Resultado<Unit> = eliminar
    }
}
