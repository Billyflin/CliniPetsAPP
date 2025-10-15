package cl.clinipets.domain.catalogo

interface CatalogoRepository {
    suspend fun ofertas(vetId: String? = null, procedimientoId: String? = null, activo: Boolean? = true): String
}

