package cl.clinipets.domain.agenda

import cl.clinipets.data.dto.CrearReserva

interface AgendaRepository {
    suspend fun slots(vetId: String, fromIso: String, toIso: String, ofertaId: String? = null): String
    suspend fun crearReserva(body: CrearReserva)
    suspend fun reservasMias(): String
}

