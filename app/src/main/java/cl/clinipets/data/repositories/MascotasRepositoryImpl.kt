package cl.clinipets.data.repositories

import cl.clinipets.data.api.MascotasApi
import cl.clinipets.data.dto.ActualizarMascota
import cl.clinipets.data.dto.CrearMascota
import cl.clinipets.data.dto.Mascota
import cl.clinipets.domain.mascotas.MascotasRepository

class MascotasRepositoryImpl(private val api: MascotasApi) : MascotasRepository {
    override suspend fun listarMias(): List<Mascota> = api.mias()
    override suspend fun crear(body: CrearMascota): Mascota = api.crear(body)
    override suspend fun actualizar(id: String, body: ActualizarMascota): Mascota = api.actualizar(id, body)
    override suspend fun eliminar(id: String) { api.eliminar(id) }
}

