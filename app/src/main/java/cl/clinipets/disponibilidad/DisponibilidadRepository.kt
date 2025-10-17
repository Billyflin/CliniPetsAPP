package cl.clinipets.disponibilidad

import android.content.Context
import cl.clinipets.network.ApiService
import cl.clinipets.network.ExcepcionDisponibilidadRequest
import cl.clinipets.network.NetworkModule
import cl.clinipets.network.ReglaDisponibilidadRequest

class DisponibilidadRepository(context: Context) {
    private val api: ApiService = NetworkModule.provideApiService(context)

    suspend fun disponibilidad(vetId: String, fecha: String) = runCatching {
        api.disponibilidadVeterinario(vetId, fecha)
    }

    suspend fun crearRegla(vetId: String, diaSemana: Int, horaInicio: String, horaFin: String) = runCatching {
        api.crearReglaDisponibilidad(vetId, ReglaDisponibilidadRequest(diaSemana, horaInicio, horaFin))
    }

    suspend fun eliminarRegla(reglaId: String) = runCatching {
        api.eliminarReglaDisponibilidad(reglaId)
    }

    suspend fun crearExcepcion(vetId: String, fecha: String, tipo: String, horaInicio: String?, horaFin: String?, motivo: String?) = runCatching {
        api.crearExcepcionDisponibilidad(vetId, ExcepcionDisponibilidadRequest(fecha, tipo, horaInicio, horaFin, motivo))
    }
}

