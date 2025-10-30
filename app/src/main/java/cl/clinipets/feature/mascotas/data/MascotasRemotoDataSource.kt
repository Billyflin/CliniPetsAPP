package cl.clinipets.feature.mascotas.data

import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.models.Mascota
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class MascotasRemotoDataSource @Inject constructor(
    private val api: MascotasApi,
) {
    suspend fun obtenerMisMascotas(): Response<List<Mascota>> = api.listarMisMascotas()
}
