package cl.clinipets.feature.mascotas.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.ActualizarMascota
import java.util.UUID

interface MascotasRepositorio {
    suspend fun obtenerMisMascotas(): Resultado<List<Mascota>>
    suspend fun obtenerMascota(id: UUID): Resultado<Mascota>
    suspend fun crearMascota(datos: CrearMascota): Resultado<Mascota>
    suspend fun actualizarMascota(id: UUID, datos: ActualizarMascota): Resultado<Mascota>
    suspend fun eliminarMascota(id: UUID): Resultado<Unit>
}
