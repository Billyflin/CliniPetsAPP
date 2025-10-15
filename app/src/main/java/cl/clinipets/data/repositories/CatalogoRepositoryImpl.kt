package cl.clinipets.data.repositories

import cl.clinipets.data.api.CatalogoApi
import cl.clinipets.data.dto.OfertaDto
import cl.clinipets.data.dto.ProcedimientoDto
import cl.clinipets.data.dto.UpsertOferta
import cl.clinipets.data.dto.UpsertProcedimiento
import cl.clinipets.domain.catalogo.CatalogoRepository

class CatalogoRepositoryImpl(private val api: CatalogoApi) : CatalogoRepository {
    override suspend fun ofertas(
        vetId: String?,
        procedimientoId: String?,
        activo: Boolean?
    ): List<OfertaDto> =
        api.ofertas(vetId, procedimientoId, activo)

    override suspend fun crearOferta(body: UpsertOferta): OfertaDto = api.crearOferta(body)
    override suspend fun editarOferta(ofertaId: String, body: UpsertOferta): OfertaDto =
        api.editarOferta(ofertaId, body)

    override suspend fun borrarOferta(ofertaId: String) {
        api.borrarOferta(ofertaId)
    }

    override suspend fun procedimientos(q: String?, categoria: String?): List<ProcedimientoDto> =
        api.procedimientos(q, categoria)

    override suspend fun crearProcedimiento(body: UpsertProcedimiento): ProcedimientoDto =
        api.crearProcedimiento(body)

    override suspend fun editarProcedimiento(
        id: String,
        body: UpsertProcedimiento
    ): ProcedimientoDto =
        api.editarProcedimiento(id, body)

    override suspend fun borrarProcedimiento(id: String) {
        api.borrarProcedimiento(id)
    }
}
