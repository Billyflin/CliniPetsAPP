package cl.clinipets.clinica

import android.content.Context
import cl.clinipets.network.NetworkModule
import cl.clinipets.network.SugerenciasDiagnosticoRequest
import cl.clinipets.network.SugerenciasTratamientoRequest

class ClinicaRepository(context: Context) {
    private val api = NetworkModule.provideApiService(context)

    suspend fun sugerenciasDiagnostico(sintomas: List<String>) = runCatching {
        api.sugerenciasDiagnostico(SugerenciasDiagnosticoRequest(sintomas))
    }

    suspend fun sugerenciasTratamiento(diagnostico: String) = runCatching {
        api.sugerenciasTratamiento(SugerenciasTratamientoRequest(diagnostico))
    }
}

