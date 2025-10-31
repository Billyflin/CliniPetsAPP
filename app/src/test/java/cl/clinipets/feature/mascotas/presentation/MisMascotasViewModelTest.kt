package cl.clinipets.feature.mascotas.presentation

import cl.clinipets.MainDispatcherRule
import cl.clinipets.core.Resultado
import cl.clinipets.feature.auth.domain.AuthRepositorio
import cl.clinipets.feature.auth.domain.ObservarSesionUseCase
import cl.clinipets.feature.auth.domain.Sesion
import cl.clinipets.feature.mascotas.domain.MascotasRepositorio
import cl.clinipets.feature.mascotas.domain.ObtenerMisMascotasUseCase
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Rol
import cl.clinipets.openapi.models.Usuario
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.ActualizarMascota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID

class MisMascotasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cuando hay sesion activa carga mascotas y expone exito`() = runTest(mainDispatcherRule.testDispatcher) {
        val mascota = crearMascota()
        val repositorio = FakeMascotasRepositorio(Resultado.Exito(listOf(mascota)))
        val sesionFlow = MutableStateFlow<Sesion?>(null)
        val viewModel = MisMascotasViewModel(
            obtenerMisMascotas = ObtenerMisMascotasUseCase(repositorio),
            observarSesion = ObservarSesionUseCase(FakeAuthRepositorio(sesionFlow)),
        )

        advanceUntilIdle()
        assertTrue(viewModel.estado.value.requiereSesion)

        sesionFlow.value = Sesion(token = "token", expiraEn = null)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertEquals(1, estado.mascotas.size)
        assertEquals("Bobby", estado.mascotas.first().nombre)
        assertNull(estado.error)
        assertFalse(estado.requiereSesion)
    }

    @Test
    fun `cuando el servicio falla expone error`() = runTest(mainDispatcherRule.testDispatcher) {
        val error = Resultado.Error(Resultado.Tipo.CLIENTE, mensaje = "No autorizado")
        val repositorio = FakeMascotasRepositorio(error)
        val sesionFlow = MutableStateFlow<Sesion?>(null)
        val viewModel = MisMascotasViewModel(
            obtenerMisMascotas = ObtenerMisMascotasUseCase(repositorio),
            observarSesion = ObservarSesionUseCase(FakeAuthRepositorio(sesionFlow)),
        )

        sesionFlow.value = Sesion(token = "token", expiraEn = null)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertEquals(0, estado.mascotas.size)
        assertEquals(error, estado.error)
    }

    private fun crearMascota(): Mascota {
        val ahora = OffsetDateTime.parse("2024-01-01T12:00:00Z")
        val rol = Rol(
            nombre = "TUTOR",
            creadoEn = ahora,
            modificadoEn = ahora,
        )
        val usuario = Usuario(
            email = "usuario@clinipets.cl",
            roles = setOf(rol),
            creadoEn = ahora,
            modificadoEn = ahora,
            id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            nombre = "Juan Perez",
        )
        return Mascota(
            nombre = "Bobby",
            especie = Mascota.Especie.PERRO,
            creadoEn = ahora,
            modificadoEn = ahora,
            id = UUID.fromString("11111111-2222-3333-4444-555555555555"),
            raza = "Labrador",
        )
    }

    private class FakeMascotasRepositorio(
        private var resultado: Resultado<List<Mascota>>,
    ) : MascotasRepositorio {
        override suspend fun obtenerMisMascotas(): Resultado<List<Mascota>> = resultado

        override suspend fun obtenerMascota(id: UUID): Resultado<Mascota> =
            throw UnsupportedOperationException("No se usa en esta prueba")

        override suspend fun crearMascota(datos: CrearMascota): Resultado<Mascota> =
            throw UnsupportedOperationException("No se usa en esta prueba")

        override suspend fun actualizarMascota(
            id: UUID,
            datos: ActualizarMascota,
        ): Resultado<Mascota> = throw UnsupportedOperationException("No se usa en esta prueba")

        override suspend fun eliminarMascota(id: UUID): Resultado<Unit> =
            throw UnsupportedOperationException("No se usa en esta prueba")
    }

    private class FakeAuthRepositorio(
        private val sesionFlow: MutableStateFlow<Sesion?>,
    ) : AuthRepositorio {
        override val sesion = sesionFlow

        override suspend fun iniciarSesionConGoogle(idToken: String): Resultado<cl.clinipets.openapi.models.LoginResponse> {
            throw UnsupportedOperationException("No se usa en esta prueba")
        }

        override suspend fun obtenerPerfil(): Resultado<cl.clinipets.openapi.models.MeResponse> {
            throw UnsupportedOperationException("No se usa en esta prueba")
        }

        override suspend fun cerrarSesion(): Resultado<Unit> {
            throw UnsupportedOperationException("No se usa en esta prueba")
        }
    }
}
