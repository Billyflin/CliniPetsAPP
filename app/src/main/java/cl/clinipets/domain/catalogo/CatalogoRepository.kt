package cl.clinipets.domain.catalogo

import cl.clinipets.data.dto.OfertaDto
import cl.clinipets.data.dto.ProcedimientoDto
import cl.clinipets.data.dto.UpsertOferta
import cl.clinipets.data.dto.UpsertProcedimiento

interface CatalogoRepository {
    suspend fun ofertas(
        vetId: String? = null,
        procedimientoId: String? = null,
        activo: Boolean? = true
    ): List<OfertaDto>

    // Ofertas CRUD
    suspend fun crearOferta(body: UpsertOferta): OfertaDto
    suspend fun editarOferta(ofertaId: String, body: UpsertOferta): OfertaDto
    suspend fun borrarOferta(ofertaId: String)

    // Procedimientos CRUD
    suspend fun procedimientos(q: String? = null, categoria: String? = null): List<ProcedimientoDto>
    suspend fun crearProcedimiento(body: UpsertProcedimiento): ProcedimientoDto
    suspend fun editarProcedimiento(id: String, body: UpsertProcedimiento): ProcedimientoDto
    suspend fun borrarProcedimiento(id: String)
}
