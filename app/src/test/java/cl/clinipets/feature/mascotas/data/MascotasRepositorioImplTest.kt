package cl.clinipets.feature.mascotas.data

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.infrastructure.ApiClient
import cl.clinipets.openapi.infrastructure.Serializer
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Rol
import cl.clinipets.openapi.models.Usuario
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID

class MascotasRepositorioImplTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: MascotasApi
    private lateinit var dataSource: MascotasRemotoDataSource
    private lateinit var repositorio: MascotasRepositorioImpl

    @Before
    fun setUp() {
        servidor = MockWebServer().apply { start() }
        val baseUrl = servidor.url("/").toString()
        api = ApiClient(baseUrl = baseUrl).createService(MascotasApi::class.java)
        dataSource = MascotasRemotoDataSource(api)
        repositorio = MascotasRepositorioImpl(dataSource)
    }

    @After
    fun tearDown() {
        servidor.shutdown()
    }

    @Test
    fun `cuando el backend responde 200 devuelve Resultado_Exito con mascotas`() = runTest {
        val mascota = crearMascota()
        val cuerpo = Serializer.gson.toJson(listOf(mascota))
        servidor.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(cuerpo)
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.obtenerMisMascotas()

        assertTrue(resultado is Resultado.Exito)
        val dato = (resultado as Resultado.Exito).dato
        assertEquals(1, dato.size)
        assertEquals(mascota.nombre, dato.first().nombre)
    }

    @Test
    fun `cuando el backend responde error 404 devuelve Resultado_Error de cliente`() = runTest {
        servidor.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"mensaje":"No hay mascotas"}""")
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.obtenerMisMascotas()

        assertTrue(resultado is Resultado.Error)
        val error = resultado as Resultado.Error
        assertEquals(Resultado.Tipo.CLIENTE, error.tipo)
        assertEquals(404, error.codigoHttp)
    }

    @Test
    fun `cuando hay un error de red devuelve Resultado_Error de red`() = runTest {
        servidor.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val resultado = repositorio.obtenerMisMascotas()

        assertTrue(resultado is Resultado.Error)
        val error = resultado as Resultado.Error
        assertEquals(Resultado.Tipo.RED, error.tipo)
    }

    @Test
    fun `obtener detalle de mascota devuelve exito`() = runTest {
        val mascota = crearMascota()
        val cuerpo = Serializer.gson.toJson(mascota)
        servidor.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(cuerpo)
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.obtenerMascota(mascota.id!!)

        assertTrue(resultado is Resultado.Exito)
        val dato = (resultado as Resultado.Exito).dato
        assertEquals(mascota.id, dato.id)
    }

    @Test
    fun `crear mascota devuelve exito`() = runTest {
        val mascota = crearMascota()
        servidor.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(Serializer.gson.toJson(mascota))
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.crearMascota(
            CrearMascota(
                nombre = mascota.nombre,
                especie = CrearMascota.Especie.valueOf(mascota.especie.name),
                raza = mascota.raza,
            ),
        )

        assertTrue(resultado is Resultado.Exito)
        val dato = (resultado as Resultado.Exito).dato
        assertEquals(mascota.nombre, dato.nombre)
    }

    @Test
    fun `eliminar mascota devuelve exito`() = runTest {
        servidor.enqueue(
            MockResponse()
                .setResponseCode(204),
        )

        val resultado = repositorio.eliminarMascota(UUID.randomUUID())

        assertTrue(resultado is Resultado.Exito)
    }

    private fun crearMascota(): Mascota {
        val ahora = OffsetDateTime.parse("2024-01-01T12:00:00Z")
        val rol = Rol(
            nombre = "TUTOR",
            creadoEn = ahora,
            modificadoEn = ahora,
            id = 1L,
        )
        val tutor = Usuario(
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
            sexo = "MACHO",
        )
    }
}
