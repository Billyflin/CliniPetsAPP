package cl.clinipets.reservas

import android.content.Context
import cl.clinipets.network.CrearReservaRequest
import cl.clinipets.network.NetworkModule
import cl.clinipets.network.Reserva

class ReservasRepository(context: Context) {
    private val api = NetworkModule.provideApiService(context)

    suspend fun crearReserva(
        mascotaId: String,
        veterinarioId: String,
        procedimientoSku: String,
        inicio: String,
        modo: String,
        direccionAtencion: String?,
        notas: String?
    ): Result<Reserva> = try {
        val req = CrearReservaRequest(mascotaId, veterinarioId, procedimientoSku, inicio, modo, direccionAtencion, notas)
        Result.success(api.crearReserva(req))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun misReservas(): Result<List<Reserva>> = try {
        Result.success(api.misReservas())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

