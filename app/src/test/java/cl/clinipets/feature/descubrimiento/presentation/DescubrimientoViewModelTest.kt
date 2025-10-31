package cl.clinipets.feature.descubrimiento.presentation

import cl.clinipets.MainDispatcherRule
import cl.clinipets.core.Resultado
import cl.clinipets.feature.descubrimiento.domain.DescubrimientoRepositorio
import cl.clinipets.feature.descubrimiento.domain.FiltrosOfertas
import cl.clinipets.feature.descubrimiento.domain.FiltrosProcedimientos
import cl.clinipets.feature.descubrimiento.domain.FiltrosVeterinarios
import cl.clinipets.feature.descubrimiento.domain.ObtenerProcedimientosUseCase
import cl.clinipets.feature.descubrimiento.domain.ObtenerVeterinariosUseCase
import cl.clinipets.feature.mascotas.domain.MascotasRepositorio
import cl.clinipets.feature.mascotas.domain.ObtenerMisMascotasUseCase
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.ProcedimientoItem
import cl.clinipets.openapi.models.VetItem
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DescubrimientoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cuando carga mascotas selecciona la primera y llena veterinarios y procedimientos`() = runTest(mainDispatcherRule.testDispatcher) {
        val mascota = crearMascota("Bobby", Mascota.Especie.PERRO)
        val vetItem = crearVetItem("Clínica Central")
        val procedimiento = crearProcedimiento("Bañar perro")
        val mascotasRepositorio = FakeMascotasRepositorio(Resultado.Exito(listOf(mascota)))
        val descubrimientoRepositorio = FakeDescubrimientoRepositorio(
            veterinariosResultado = Resultado.Exito(listOf(vetItem)),
            procedimientosResultado = Resultado.Exito(listOf(procedimiento)),
        )

        val viewModel = DescubrimientoViewModel(
            obtenerMisMascotas = ObtenerMisMascotasUseCase(mascotasRepositorio),
            obtenerVeterinarios = ObtenerVeterinariosUseCase(descubrimientoRepositorio),
            obtenerProcedimientos = ObtenerProcedimientosUseCase(descubrimientoRepositorio),
        )

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(1, estado.mascotas.size)
        assertEquals("Bobby", estado.mascotaSeleccionada?.nombre)
        assertEquals(1, estado.veterinarios.size)
        assertEquals("Clínica Central", estado.veterinarios.first().nombre)
        assertEquals(1, estado.procedimientos.size)
        assertNull(estado.error)
    }

    @Test
    fun `cuando cargar mascotas falla se expone error`() = runTest(mainDispatcherRule.testDispatcher) {
        val error = Resultado.Error(Resultado.Tipo.RED, mensaje = "Sin conexión")
        val mascotasRepositorio = FakeMascotasRepositorio(error)
        val descubrimientoRepositorio = FakeDescubrimientoRepositorio()

        val viewModel = DescubrimientoViewModel(
            obtenerMisMascotas = ObtenerMisMascotasUseCase(mascotasRepositorio),
            obtenerVeterinarios = ObtenerVeterinariosUseCase(descubrimientoRepositorio),
            obtenerProcedimientos = ObtenerProcedimientosUseCase(descubrimientoRepositorio),
        )

        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertNotNull(estado.error)
        assertEquals(error, estado.error)
        assertTrue(estado.mascotas.isEmpty())
    }

    private fun crearMascota(nombre: String, especie: Mascota.Especie): Mascota {
        val ahora = OffsetDateTime.parse("2024-01-01T10:00:00Z")
        return Mascota(
            id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffffffff"),
            nombre = nombre,
            especie = especie,
            creadoEn = ahora,
            modificadoEn = ahora,
        )
    }

    private fun crearVetItem(nombre: String): VetItem = VetItem(
        id = UUID.randomUUID(),
        nombre = nombre,
        estado = "VERIFICADO",
        modosAtencion = setOf(VetItem.ModosAtencion.CLINICA),
        distanciaKm = 2.0,
    )

    private fun crearProcedimiento(nombre: String): ProcedimientoItem = ProcedimientoItem(
        sku = "SKU-1",
        nombre = nombre,
        compatibleCon = ProcedimientoItem.CompatibleCon.PERRO,
        duracionMinutos = 30,
    )

    private class FakeMascotasRepositorio(
        private val resultado: Resultado<List<Mascota>>,
    ) : MascotasRepositorio {
        override suspend fun obtenerMisMascotas(): Resultado<List<Mascota>> = resultado
    }

    private class FakeDescubrimientoRepositorio(
        private val veterinariosResultado: Resultado<List<VetItem>> = Resultado.Exito(emptyList()),
        private val ofertasResultado: Resultado<List<cl.clinipets.openapi.models.OfertaItem>> = Resultado.Exito(emptyList()),
        private val procedimientosResultado: Resultado<List<ProcedimientoItem>> = Resultado.Exito(emptyList()),
    ) : DescubrimientoRepositorio {
        override suspend fun buscarVeterinarios(filtros: FiltrosVeterinarios): Resultado<List<VetItem>> = veterinariosResultado

        override suspend fun buscarOfertas(filtros: FiltrosOfertas): Resultado<List<cl.clinipets.openapi.models.OfertaItem>> = ofertasResultado

        override suspend fun buscarProcedimientos(filtros: FiltrosProcedimientos): Resultado<List<ProcedimientoItem>> = procedimientosResultado
    }
}
