package cl.clinipets.data.repositories

import cl.clinipets.data.api.VeterinariosApi
import cl.clinipets.data.dto.UpsertPerfil
import cl.clinipets.domain.veterinarios.VeterinariosRepository

class VeterinariosRepositoryImpl(private val api: VeterinariosApi) : VeterinariosRepository {
    override suspend fun upsertMiPerfil(body: UpsertPerfil): String = api.upsertMiPerfil(body)
    override suspend fun publicos(): String = api.publicos()
    override suspend fun verificar(vetId: String): String = api.verificar(vetId)
}

