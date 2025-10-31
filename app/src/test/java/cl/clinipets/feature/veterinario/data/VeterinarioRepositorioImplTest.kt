package cl.clinipets.feature.veterinario.data

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.apis.DisponibilidadApi
import cl.clinipets.openapi.infrastructure.ApiClient
import cl.clinipets.openapi.infrastructure.Serializer
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import cl.clinipets.openapi.models.VeterinarioPerfil
import cl.clinipets.feature.veterinario.data.DisponibilidadRemoteDataSource
import java.util.UUID
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VeterinarioRepositorioImplTest {

    private lateinit var server: MockWebServer
    private lateinit var api: VeterinariosApi
    private lateinit var repositorio: VeterinarioRepositorioImpl

    @Before
    fun setup() {
        server = MockWebServer().apply { start() }
        api = ApiClient(baseUrl = server.url("/").toString()).createService(VeterinariosApi::class.java)
        val disponibilidadApi = ApiClient(baseUrl = server.url("/").toString()).createService(cl.clinipets.openapi.apis.DisponibilidadApi::class.java)
        repositorio = VeterinarioRepositorioImpl(
            remoto = VeterinarioRemoteDataSource(api),
            disponibilidadRemote = DisponibilidadRemoteDataSource(disponibilidadApi),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `registrar veterinario devuelve exito`() = runTest {
        val perfil = crearPerfil()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Serializer.gson.toJson(perfil))
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.registrar(
            RegistrarVeterinarioRequest(nombreCompleto = "Dr Vet"),
        )

        assertTrue(resultado is Resultado.Exito)
        val dato = (resultado as Resultado.Exito).dato
        assertEquals(perfil.id, dato.id)
    }

    @Test
    fun `crear regla semanal devuelve exito`() = runTest {
        val regla = Serializer.gson.toJson(crearRegla())
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(regla)
                .addHeader("Content-Type", "application/json"),
        )

        val resultado = repositorio.crearReglaSemanal(
            UUID.randomUUID(),
            cl.clinipets.openapi.models.CrearReglaSemanal(
                diaSemana = cl.clinipets.openapi.models.CrearReglaSemanal.DiaSemana.MONDAY,
                horaInicio = "09:00",
                horaFin = "10:00",
            ),
        )

        assertTrue(resultado is Resultado.Exito)
    }

    private fun crearPerfil(): VeterinarioPerfil = VeterinarioPerfil(
        id = UUID.randomUUID(),
        nombreCompleto = "Dr Vet",
        modosAtencion = setOf(VeterinarioPerfil.ModosAtencion.CLINICA),
        verificado = false,
    )

    private fun crearRegla(): cl.clinipets.openapi.models.ReglaSemanal {
        val ahora = java.time.OffsetDateTime.parse("2024-01-01T10:00:00Z")
        val vet = cl.clinipets.openapi.models.Veterinario(
            id = UUID.randomUUID(),
            nombre = "Dr Vet",
            email = "vet@clinipets.cl",
            roles = emptySet(),
            creadoEn = ahora,
            modificadoEn = ahora,
        )
        return cl.clinipets.openapi.models.ReglaSemanal(
            veterinario = vet,
            diaSemana = cl.clinipets.openapi.models.ReglaSemanal.DiaSemana.MONDAY,
            horaInicio = "09:00",
            horaFin = "10:00",
            activa = true,
            creadoEn = ahora,
            modificadoEn = ahora,
            id = UUID.randomUUID(),
        )
    }
}
