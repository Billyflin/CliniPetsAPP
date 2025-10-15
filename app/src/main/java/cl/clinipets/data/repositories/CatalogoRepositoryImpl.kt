package cl.clinipets.data.repositories

import cl.clinipets.data.api.CatalogoApi
import cl.clinipets.domain.catalogo.CatalogoRepository

class CatalogoRepositoryImpl(private val api: CatalogoApi) : CatalogoRepository {
    override suspend fun ofertas(vetId: String?, procedimientoId: String?, activo: Boolean?): String =
        api.ofertas(vetId, procedimientoId, activo)
}

