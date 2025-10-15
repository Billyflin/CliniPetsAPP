package cl.clinipets.domain.veterinarios

import cl.clinipets.data.dto.UpsertPerfil

interface VeterinariosRepository {
    suspend fun upsertMiPerfil(body: UpsertPerfil): String
    suspend fun publicos(): String
    suspend fun verificar(vetId: String): String
}

