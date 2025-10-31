package cl.clinipets.feature.veterinario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.Resultado
import cl.clinipets.feature.veterinario.domain.ActualizarPerfilVeterinarioUseCase
import cl.clinipets.feature.veterinario.domain.ObtenerPerfilVeterinarioUseCase
import cl.clinipets.feature.veterinario.domain.RegistrarVeterinarioUseCase
import cl.clinipets.openapi.models.ActualizarPerfilRequest
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VeterinarioOnboardingUiState(
    val nombreCompleto: String = "",
    val numeroLicencia: String = "",
    val modosAtencion: Set<RegistrarVeterinarioRequest.ModosAtencion> = emptySet(),
    val latitud: Double? = null,
    val longitud: Double? = null,
    val radioCoberturaKm: Double = 5.0,
    val cargando: Boolean = false,
    val cargandoInicial: Boolean = true,
    val esEdicion: Boolean = false,
    val error: Resultado.Error? = null,
    val nombreError: String? = null,
)

sealed class VeterinarioOnboardingEvento {
    data class RegistroCompleto(val perfilId: java.util.UUID) : VeterinarioOnboardingEvento()
}

@HiltViewModel
class VeterinarioOnboardingViewModel @Inject constructor(
    private val registrarVeterinario: RegistrarVeterinarioUseCase,
    private val obtenerPerfilVeterinario: ObtenerPerfilVeterinarioUseCase,
    private val actualizarPerfilVeterinario: ActualizarPerfilVeterinarioUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(VeterinarioOnboardingUiState())
    val estado: StateFlow<VeterinarioOnboardingUiState> = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<VeterinarioOnboardingEvento>()
    val eventos: SharedFlow<VeterinarioOnboardingEvento> = _eventos.asSharedFlow()

    init {
        cargarPerfilExistente()
    }

    fun onNombreChange(valor: String) {
        _estado.update { it.copy(nombreCompleto = valor, nombreError = null, error = null) }
    }

    fun onNumeroLicenciaChange(valor: String) {
        _estado.update { it.copy(numeroLicencia = valor, error = null) }
    }

    fun onUbicacionSeleccionada(latitud: Double, longitud: Double) {
        _estado.update { it.copy(latitud = latitud, longitud = longitud, error = null) }
    }

    fun limpiarUbicacion() {
        _estado.update { it.copy(latitud = null, longitud = null, error = null) }
    }

    fun onRadioChange(valorKm: Double) {
        _estado.update { it.copy(radioCoberturaKm = valorKm, error = null) }
    }

    fun toggleModo(modo: RegistrarVeterinarioRequest.ModosAtencion) {
        _estado.update { estadoActual ->
            val nuevoSet = estadoActual.modosAtencion.toMutableSet().apply {
                if (contains(modo)) remove(modo) else add(modo)
            }
            estadoActual.copy(modosAtencion = nuevoSet, error = null)
        }
    }

    fun registrar() {
        val estadoActual = estado.value
        if (estadoActual.nombreCompleto.isBlank()) {
            _estado.update { it.copy(nombreError = "Ingresa tu nombre completo") }
            return
        }
        if (estadoActual.cargando) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val nombreNormalizado = estadoActual.nombreCompleto.trim()
            val radio = if (estadoActual.latitud != null && estadoActual.longitud != null) {
                estadoActual.radioCoberturaKm
            } else {
                null
            }

            val resultado = if (estadoActual.esEdicion) {
                val request = ActualizarPerfilRequest(
                    nombreCompleto = nombreNormalizado,
                    numeroLicencia = estadoActual.numeroLicencia.takeIf { it.isNotBlank() },
                    modosAtencion = estadoActual.modosAtencion
                        .takeIf { it.isNotEmpty() }
                        ?.map { ActualizarPerfilRequest.ModosAtencion.valueOf(it.name) }
                        ?.toSet(),
                    latitud = estadoActual.latitud,
                    longitud = estadoActual.longitud,
                    radioCobertura = radio,
                )
                actualizarPerfilVeterinario(request)
            } else {
                val request = RegistrarVeterinarioRequest(
                    nombreCompleto = nombreNormalizado,
                    numeroLicencia = estadoActual.numeroLicencia.takeIf { it.isNotBlank() },
                    modosAtencion = estadoActual.modosAtencion.ifEmpty { null },
                    latitud = estadoActual.latitud,
                    longitud = estadoActual.longitud,
                    radioCobertura = radio,
                )
                registrarVeterinario(request)
            }

            when (resultado) {
                is Resultado.Exito -> {
                    _estado.update {
                        it.copy(
                            cargando = false,
                            cargandoInicial = false,
                            esEdicion = true,
                        )
                    }
                    _eventos.emit(VeterinarioOnboardingEvento.RegistroCompleto(resultado.dato.id))
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado)
                }
            }
        }
    }

    private fun cargarPerfilExistente() {
        viewModelScope.launch {
            when (val resultado = obtenerPerfilVeterinario()) {
                is Resultado.Exito -> {
                    val perfil = resultado.dato
                    _estado.update {
                        it.copy(
                            nombreCompleto = perfil.nombreCompleto,
                            numeroLicencia = perfil.numeroLicencia.orEmpty(),
                            modosAtencion = perfil.modosAtencion.map { modo ->
                                RegistrarVeterinarioRequest.ModosAtencion.valueOf(modo.name)
                            }.toSet(),
                            latitud = perfil.latitud,
                            longitud = perfil.longitud,
                            radioCoberturaKm = perfil.radioCobertura ?: RADIO_DEFECTO_KM,
                            cargandoInicial = false,
                            esEdicion = true,
                            error = null,
                        )
                    }
                }

                is Resultado.Error -> {
                    if (resultado.codigoHttp == 404) {
                        _estado.update {
                            it.copy(
                                cargandoInicial = false,
                                esEdicion = false,
                                error = null,
                            )
                        }
                    } else {
                        _estado.update {
                            it.copy(
                                cargandoInicial = false,
                                error = resultado,
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val RADIO_DEFECTO_KM = 5.0
    }
}
