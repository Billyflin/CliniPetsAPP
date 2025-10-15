package cl.clinipets.domain.agenda

import cl.clinipets.data.dto.CrearReserva
import cl.clinipets.data.dto.ReservaDto

interface AgendaRepository {
    suspend fun slots(vetId: String, fromIso: String, toIso: String, ofertaId: String? = null): String
    suspend fun crearReserva(body: CrearReserva): ReservaDto
    suspend fun reservasMias(): List<ReservaDto>
    suspend fun detalle(reservaId: String): ReservaDto
    suspend fun aceptar(reservaId: String): ReservaDto
    suspend fun rechazar(reservaId: String): ReservaDto
    suspend fun cancelar(reservaId: String): ReservaDto
    suspend fun completar(reservaId: String): ReservaDto
}
