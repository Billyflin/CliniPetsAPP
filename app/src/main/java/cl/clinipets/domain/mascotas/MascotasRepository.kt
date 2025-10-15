package cl.clinipets.domain.mascotas

import cl.clinipets.data.dto.ActualizarMascota
import cl.clinipets.data.dto.CrearMascota
import cl.clinipets.data.dto.Mascota

interface MascotasRepository {
    suspend fun listarMias(): List<Mascota>
    suspend fun crear(body: CrearMascota): Mascota
    suspend fun actualizar(id: String, body: ActualizarMascota): Mascota
    suspend fun eliminar(id: String)
}

