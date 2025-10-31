package cl.clinipets.feature.mascotas.data

import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.Mascota
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class MascotasRemotoDataSource @Inject constructor(
    private val api: MascotasApi,
) {
    suspend fun obtenerMisMascotas(): Response<List<Mascota>> = api.listarMisMascotas()
    suspend fun obtenerMascota(id: UUID): Response<Mascota> = api.detalleMascota(id)
    suspend fun crearMascota(datos: CrearMascota): Response<Mascota> = api.crearMascota(datos)
    suspend fun actualizarMascota(id: UUID, datos: ActualizarMascota): Response<Mascota> =
        api.actualizarMascota(id, datos)

    suspend fun eliminarMascota(id: UUID): Response<Unit> = api.eliminarMascota(id)
}
