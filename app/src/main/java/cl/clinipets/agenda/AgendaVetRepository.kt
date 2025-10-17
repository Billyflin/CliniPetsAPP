package cl.clinipets.agenda

import android.content.Context
import cl.clinipets.network.NetworkModule

class AgendaVetRepository(context: Context) {
    private val api = NetworkModule.provideApiService(context)

    suspend fun reservasPorVet(vetId: String) = runCatching {
        api.reservasPorVeterinario(vetId)
    }

    suspend fun cambiarEstado(reservaId: String, nuevoEstado: String, motivo: String? = null) = runCatching {
        api.cambiarEstadoReserva(reservaId, cl.clinipets.network.CambioEstadoReservaRequest(nuevoEstado, motivo))
    }
}

