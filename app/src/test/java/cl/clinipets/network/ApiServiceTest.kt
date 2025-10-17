package cl.clinipets.network

import kotlinx.coroutines.runBlocking
import kotlin.test.assertNotNull
import org.junit.Test

class ApiServiceTest {
    @Test
    fun `api service instances are created`() = runBlocking {
        // TokenProvider.token stays null for now; we're only verifying instantiation
        assertNotNull(ApiService.authApi)
        assertNotNull(ApiService.mascotasApi)
        assertNotNull(ApiService.descubrimientoApi)
        assertNotNull(ApiService.reservasApi)
        assertNotNull(ApiService.veterinariosApi)
        assertNotNull(ApiService.disponibilidadApi)
        assertNotNull(ApiService.juntasApi)
        assertNotNull(ApiService.catalogoApi)
        assertNotNull(ApiService.inventarioApi)
    }
}
