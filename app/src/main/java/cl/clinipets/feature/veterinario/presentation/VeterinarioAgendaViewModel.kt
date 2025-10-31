package cl.clinipets.feature.veterinario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.Resultado
import cl.clinipets.feature.veterinario.domain.CrearExcepcionUseCase
import cl.clinipets.feature.veterinario.domain.CrearReglaSemanalUseCase
import cl.clinipets.feature.veterinario.domain.EliminarReglaSemanalUseCase
import cl.clinipets.feature.veterinario.domain.ObtenerDisponibilidadUseCase
import cl.clinipets.feature.veterinario.domain.ObtenerPerfilVeterinarioUseCase
import cl.clinipets.openapi.models.BloqueHorario
import cl.clinipets.openapi.models.CrearExcepcion
import cl.clinipets.openapi.models.CrearReglaSemanal
import cl.clinipets.openapi.models.ExcepcionDisponibilidad
import cl.clinipets.openapi.models.ReglaSemanal
import cl.clinipets.openapi.models.VeterinarioPerfil
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VeterinarioAgendaUiState(
    val cargandoPerfil: Boolean = true,
    val veterinarioId: UUID? = null,
    val perfil: VeterinarioPerfil? = null,
    val reglas: List<ReglaSemanal> = emptyList(),
    val excepciones: List<ExcepcionDisponibilidad> = emptyList(),
    val disponibilidad: List<BloqueHorario> = emptyList(),
    val fechaConsulta: String = "",
    val error: Resultado.Error? = null,
    val guardando: Boolean = false,
)

@HiltViewModel
class VeterinarioAgendaViewModel @Inject constructor(
    private val obtenerPerfil: ObtenerPerfilVeterinarioUseCase,
    private val crearReglaSemanal: CrearReglaSemanalUseCase,
    private val eliminarReglaSemanal: EliminarReglaSemanalUseCase,
    private val crearExcepcion: CrearExcepcionUseCase,
    private val obtenerDisponibilidad: ObtenerDisponibilidadUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(VeterinarioAgendaUiState())
    val estado: StateFlow<VeterinarioAgendaUiState> = _estado.asStateFlow()

    private val _reglaForm = MutableStateFlow(ReglaFormState())
    val reglaForm: StateFlow<ReglaFormState> = _reglaForm.asStateFlow()

    private val _excepcionForm = MutableStateFlow(ExcepcionFormState())
    val excepcionForm: StateFlow<ExcepcionFormState> = _excepcionForm.asStateFlow()

    init {
        cargarPerfil()
    }

    private fun cargarPerfil() {
        viewModelScope.launch {
            _estado.update { it.copy(cargandoPerfil = true, error = null) }
            when (val resultado = obtenerPerfil()) {
                is Resultado.Exito -> {
                    val perfil = resultado.dato
                    _estado.update {
                        it.copy(
                            cargandoPerfil = false,
                            veterinarioId = perfil.id,
                            perfil = perfil,
                            error = null,
                        )
                    }
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargandoPerfil = false, error = resultado)
                }
            }
        }
    }

    fun onReglaDiaChange(dia: CrearReglaSemanal.DiaSemana) {
        _reglaForm.update { it.copy(diaSemana = dia) }
    }

    fun onReglaHoraInicioChange(valor: String) {
        _reglaForm.update { it.copy(horaInicio = valor) }
    }

    fun onReglaHoraFinChange(valor: String) {
        _reglaForm.update { it.copy(horaFin = valor) }
    }

    fun crearRegla() {
        val estadoActual = estado.value
        val vetId = estadoActual.veterinarioId ?: return
        val form = reglaForm.value
        if (!form.esValido()) {
            _reglaForm.update { it.copy(error = "Completa día y horario") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null) }
            val request = CrearReglaSemanal(
                diaSemana = form.diaSemana!!,
                horaInicio = form.horaInicio,
                horaFin = form.horaFin,
            )
            when (val resultado = crearReglaSemanal(vetId, request)) {
                is Resultado.Exito -> {
                    _estado.update {
                        it.copy(
                            guardando = false,
                            reglas = it.reglas + resultado.dato,
                        )
                    }
                    _reglaForm.value = ReglaFormState()
                }

                is Resultado.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado)
                }
            }
        }
    }

    fun eliminarRegla(id: UUID) {
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null) }
            when (val resultado = eliminarReglaSemanal(id)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        guardando = false,
                        reglas = it.reglas.filterNot { regla -> regla.id == id },
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado)
                }
            }
        }
    }

    fun onExcepcionFechaChange(valor: String) {
        _excepcionForm.update { it.copy(fecha = valor) }
    }

    fun onExcepcionTipoChange(tipo: CrearExcepcion.Tipo) {
        _excepcionForm.update { it.copy(tipo = tipo) }
    }

    fun onExcepcionHoraInicioChange(valor: String) {
        _excepcionForm.update { it.copy(horaInicio = valor) }
    }

    fun onExcepcionHoraFinChange(valor: String) {
        _excepcionForm.update { it.copy(horaFin = valor) }
    }

    fun onExcepcionMotivoChange(valor: String) {
        _excepcionForm.update { it.copy(motivo = valor) }
    }

    fun crearExcepcion() {
        val vetId = estado.value.veterinarioId ?: return
        val form = excepcionForm.value
        if (!form.esValido()) {
            _excepcionForm.update { it.copy(error = "Selecciona fecha y tipo") }
            return
        }
        val fecha = runCatching { LocalDate.parse(form.fecha!!) }.getOrElse {
            _excepcionForm.update { it.copy(error = "Formato de fecha inválido") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null) }
            val request = CrearExcepcion(
                fecha = fecha,
                tipo = form.tipo!!,
                horaInicio = form.horaInicio.takeIf { it.isNotBlank() },
                horaFin = form.horaFin.takeIf { it.isNotBlank() },
                motivo = form.motivo.takeIf { it.isNotBlank() },
            )
            when (val resultado = crearExcepcion(vetId, request)) {
                is Resultado.Exito -> {
                    _estado.update {
                        it.copy(
                            guardando = false,
                            excepciones = it.excepciones + resultado.dato,
                        )
                    }
                    _excepcionForm.value = ExcepcionFormState()
                }

                is Resultado.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado)
                }
            }
        }
    }

    fun onFechaDisponibilidadChange(valor: String) {
        _estado.update { it.copy(fechaConsulta = valor) }
    }

    fun consultarDisponibilidad() {
        val vetId = estado.value.veterinarioId ?: return
        val fecha = estado.value.fechaConsulta.takeIf { it.isNotBlank() } ?: return
        val fechaParsed = runCatching { LocalDate.parse(fecha) }.getOrElse {
            _estado.update { it.copy(error = Resultado.Error(Resultado.Tipo.CLIENTE, "Formato de fecha inválido")) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null) }
            when (val resultado = obtenerDisponibilidad(vetId, fechaParsed)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(guardando = false, disponibilidad = resultado.dato)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado)
                }
            }
        }
    }
}

data class ReglaFormState(
    val diaSemana: CrearReglaSemanal.DiaSemana? = null,
    val horaInicio: String = "",
    val horaFin: String = "",
    val error: String? = null,
) {
    fun esValido(): Boolean = diaSemana != null && horaInicio.isNotBlank() && horaFin.isNotBlank()
}

data class ExcepcionFormState(
    val fecha: String? = null,
    val tipo: CrearExcepcion.Tipo? = null,
    val horaInicio: String = "",
    val horaFin: String = "",
    val motivo: String = "",
    val error: String? = null,
) {
    fun esValido(): Boolean = fecha != null && tipo != null
}
