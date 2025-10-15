package cl.clinipets.data.repositories

import cl.clinipets.data.api.AgendaApi
import cl.clinipets.data.dto.CrearReserva
import cl.clinipets.domain.agenda.AgendaRepository

class AgendaRepositoryImpl(private val api: AgendaApi) : AgendaRepository {
    override suspend fun slots(vetId: String, fromIso: String, toIso: String, ofertaId: String?): String =
        api.slots(vetId, fromIso, toIso, ofertaId)

    override suspend fun crearReserva(body: CrearReserva) {
        api.crearReserva(body)
    }

    override suspend fun reservasMias(): String = api.reservasMias()
}

