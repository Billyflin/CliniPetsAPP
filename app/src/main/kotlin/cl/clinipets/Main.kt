package cl.clinipets
import cl.clinipets.openapi.apis.DescubrimientoApi
import cl.clinipets.openapi.infrastructure.ApiClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val baseUrl = "https://clinipets.cl"      // ajusta si corresponde
    val token   ="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5MmM5MDI0NC04ODc5LTQzNjctOWNhMS1kOGYxYmRjNDk4MGUiLCJpc3MiOiJjbGluaXBldHMtYmFja2VuZCIsImlhdCI6MTc2MjAzODgwNSwiZXhwIjoxNzYyMDc0ODA1LCJlbWFpbCI6ImJpbGx5bWFydGluZXpjQGdtYWlsLmNvbSIsInJvbGVzIjpbIlZFVEVSSU5BUklPIiwiQ0xJRU5URSJdLCJub21icmUiOiJCaWxseWZsaW4iLCJmb3RvVXJsIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jTEFZZGdjRUlYYmlCSnJlSFhhWkFvVmFpRFlpVjZvQk9KSjBrYjh1OUx0MDN5ajdDWHA9czk2LWMifQ.lolH_vKGiponUINbs6LwF9670zZlRdWlRNypfsjAsF0" // o null/"" si no usas bearer
    val client = if (!token.isNullOrBlank())
        ApiClient(baseUrl = baseUrl, authName = "bearerAuth", bearerToken = token)
    else
        ApiClient(baseUrl = baseUrl)

    val api = client.createService(DescubrimientoApi::class.java)

    val resp = api.descubrimientoBuscarOfertas(limit = 5, offset = 0)
    println("HTTP ${resp.code()}")
    println(resp.body()?.joinToString("\n") { it.toString() } ?: resp.errorBody()?.string())
}
