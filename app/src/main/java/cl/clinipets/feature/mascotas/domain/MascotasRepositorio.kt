package cl.clinipets.feature.mascotas.domain

import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.Mascota

interface MascotasRepositorio {
    suspend fun obtenerMisMascotas(): Resultado<List<Mascota>>
}
