package cl.clinipets.data.repositories

import cl.clinipets.data.api.VeterinariosApi
import cl.clinipets.data.dto.UpsertPerfil
import cl.clinipets.domain.veterinarios.VeterinariosRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VeterinariosRepositoryImpl(private val api: VeterinariosApi) : VeterinariosRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    override suspend fun upsertMiPerfil(body: UpsertPerfil): String =
        json.encodeToString(api.upsertMiPerfil(body))

    override suspend fun publicos(): String =
        json.encodeToString(api.publicos())

    override suspend fun verificar(vetId: String): String =
        json.encodeToString(api.verificar(vetId))
}
