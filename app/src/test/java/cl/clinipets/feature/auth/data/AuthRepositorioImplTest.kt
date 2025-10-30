package cl.clinipets.feature.auth.data

import cl.clinipets.core.Resultado
import cl.clinipets.feature.auth.data.SesionLocalDataSource
import cl.clinipets.feature.auth.domain.Sesion
import cl.clinipets.openapi.apis.AutenticacinApi
import cl.clinipets.openapi.infrastructure.ApiClient
import cl.clinipets.openapi.infrastructure.Serializer
import cl.clinipets.openapi.models.LoginResponse
import cl.clinipets.openapi.models.MeResponse
import cl.clinipets.openapi.models.UsuarioInfo
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositorioImplTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: AutenticacinApi
    private lateinit var sesionLocal: FakeSesionLocalDataSource
    private lateinit var repositorio: AuthRepositorioImpl

    @Before
    fun setup() {
        servidor = MockWebServer().apply { start() }
        val baseUrl = servidor.url("/").toString()
        api = ApiClient(baseUrl = baseUrl).createService(AutenticacinApi::class.java)
        sesionLocal = FakeSesionLocalDataSource()
        repositorio = AuthRepositorioImpl(api, sesionLocal)
    }

    @After
    fun tearDown() {
        servidor.shutdown()
    }

    @Test
    fun `login exitoso guarda sesion y retorna resultado exito`() = runTest {
        val respuesta = crearLoginResponse()
        servidor.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Serializer.gson.toJson(respuesta))
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.iniciarSesionConGoogle("token-google")

        assertTrue(resultado is Resultado.Exito)
        val sesionGuardada = sesionLocal.sesion.first()
        assertEquals(respuesta.token, sesionGuardada?.token)
    }

    @Test
    fun `obtener perfil devuelve exito y no altera sesion`() = runTest {
        val me = MeResponse(
            autenticado = true,
            usuario = crearLoginResponse().usuario,
        )
        servidor.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Serializer.gson.toJson(me))
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.obtenerPerfil()

        assertTrue(resultado is Resultado.Exito)
        assertNull(sesionLocal.sesion.first())
    }

    @Test
    fun `cerrar sesion limpia token local`() = runTest {
        sesionLocal.guardarSesion(crearLoginResponse())
        servidor.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.cerrarSesion()

        assertTrue(resultado is Resultado.Exito)
        assertNull(sesionLocal.sesion.first())
    }

    private fun crearLoginResponse(): LoginResponse {
        val ahora = OffsetDateTime.parse("2024-01-01T10:00:00Z")
        val usuario = UsuarioInfo(
            id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffffffff"),
            email = "usuario@clinipets.cl",
            roles = listOf("TUTOR"),
            nombre = "Usuario de Prueba",
        )
        return LoginResponse(
            token = "token-jwt",
            expiresAt = ahora.plusHours(1),
            usuario = usuario,
            refreshExpiresAt = ahora.plusDays(7),
        )
    }

    private class FakeSesionLocalDataSource : SesionLocalDataSource {
        private val estado = MutableStateFlow<Sesion?>(null)

        override val sesion: Flow<Sesion?> = estado

        override suspend fun guardarSesion(login: LoginResponse) {
            estado.value = Sesion(token = login.token, expiraEn = login.expiresAt)
        }

        override suspend fun limpiarSesion() {
            estado.value = null
        }
    }
}
