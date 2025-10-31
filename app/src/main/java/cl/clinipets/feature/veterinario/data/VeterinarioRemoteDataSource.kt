package cl.clinipets.feature.veterinario.data

import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.ActualizarPerfilRequest
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VeterinarioRemoteDataSource @Inject constructor(
    private val api: VeterinariosApi,
) {
    suspend fun registrar(request: RegistrarVeterinarioRequest) = api.registrar(request)
    suspend fun obtenerMiPerfil() = api.miPerfil()
    suspend fun actualizarPerfil(request: ActualizarPerfilRequest) = api.actualizarMiPerfil(request)
}
