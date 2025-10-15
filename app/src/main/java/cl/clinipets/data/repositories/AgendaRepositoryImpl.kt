package cl.clinipets.data.repositories

import cl.clinipets.data.api.AgendaApi
import cl.clinipets.data.dto.CrearReserva
import cl.clinipets.data.dto.ReservaDto
import cl.clinipets.domain.agenda.AgendaRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AgendaRepositoryImpl(private val api: AgendaApi) : AgendaRepository {
    override suspend fun slots(vetId: String, fromIso: String, toIso: String, ofertaId: String?): String {
        val list = api.slots(vetId, fromIso, toIso, ofertaId)
        val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
        return json.encodeToString(list)
    }

    override suspend fun crearReserva(body: CrearReserva): ReservaDto =
        api.crearReserva(body)

    override suspend fun reservasMias(): List<ReservaDto> = api.reservasMias()

    override suspend fun detalle(reservaId: String): ReservaDto = api.detalle(reservaId)

    override suspend fun aceptar(reservaId: String): ReservaDto = api.aceptar(reservaId)

    override suspend fun rechazar(reservaId: String): ReservaDto = api.rechazar(reservaId)

    override suspend fun cancelar(reservaId: String): ReservaDto = api.cancelar(reservaId)

    override suspend fun completar(reservaId: String): ReservaDto = api.completar(reservaId)
}
