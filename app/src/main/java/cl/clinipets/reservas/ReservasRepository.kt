package cl.clinipets.reservas

import android.content.Context
import cl.clinipets.network.CambioEstadoReservaRequest
import cl.clinipets.network.CrearReservaRequest
import cl.clinipets.network.NetworkModule
import cl.clinipets.network.Reserva
import cl.clinipets.network.Bloque

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

    suspend fun disponibilidad(vetId: String, fecha: String): Result<List<Bloque>> = try {
        Result.success(api.disponibilidadVeterinario(vetId, fecha))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cambiarEstado(reservaId: String, nuevoEstado: String, motivo: String? = null): Result<Unit> = try {
        api.cambiarEstadoReserva(reservaId, CambioEstadoReservaRequest(nuevoEstado, motivo))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
