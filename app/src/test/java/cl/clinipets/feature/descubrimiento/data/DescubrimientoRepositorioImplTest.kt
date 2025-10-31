package cl.clinipets.feature.descubrimiento.data

import cl.clinipets.core.Resultado
import cl.clinipets.feature.descubrimiento.domain.FiltrosVeterinarios
import cl.clinipets.openapi.apis.DescubrimientoApi
import cl.clinipets.openapi.infrastructure.ApiClient
import cl.clinipets.openapi.infrastructure.Serializer
import cl.clinipets.openapi.models.VetItem
import java.util.UUID
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DescubrimientoRepositorioImplTest {

    private lateinit var server: MockWebServer
    private lateinit var api: DescubrimientoApi
    private lateinit var repositorio: DescubrimientoRepositorioImpl

    @Before
    fun setup() {
        server = MockWebServer().apply { start() }
        api = ApiClient(baseUrl = server.url("/").toString()).createService(DescubrimientoApi::class.java)
        repositorio = DescubrimientoRepositorioImpl(DescubrimientoDataSource(api))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `buscar veterinarios retorna exito cuando el backend responde 200`() = runTest {
        val vet = VetItem(
            id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffffffff"),
            nombre = "Clínica Central",
            estado = "VERIFICADO",
            modosAtencion = setOf(VetItem.ModosAtencion.CLINICA),
            latitud = -33.45,
            longitud = -70.66,
            distanciaKm = 1.5,
        )
        val body = Serializer.gson.toJson(listOf(vet))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.buscarVeterinarios(
            FiltrosVeterinarios(especie = "PERRO"),
        )

        assertTrue(resultado is Resultado.Exito)
        val data = (resultado as Resultado.Exito).dato
        assertEquals(1, data.size)
        assertEquals("Clínica Central", data.first().nombre)
    }

    @Test
    fun `buscar veterinarios retorna error cuando backend responde 500`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"mensaje":"Error interno"}""")
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.buscarVeterinarios(
            FiltrosVeterinarios(),
        )

        assertTrue(resultado is Resultado.Error)
        val error = resultado as Resultado.Error
        assertEquals(Resultado.Tipo.SERVIDOR, error.tipo)
        assertEquals(500, error.codigoHttp)
    }
}
