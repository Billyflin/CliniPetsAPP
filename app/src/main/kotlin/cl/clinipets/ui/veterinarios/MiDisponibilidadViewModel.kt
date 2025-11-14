package cl.clinipets.ui.veterinarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.HorariosVeterinarioControllerApi
import cl.clinipets.openapi.models.ExcepcionHorario
import cl.clinipets.openapi.models.HorarioAtencion
import cl.clinipets.openapi.models.Intervalo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MiDisponibilidadUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val horarios: List<HorarioAtencion> = emptyList(),
    val excepciones: List<ExcepcionHorario> = emptyList(),
    val disponibilidad: List<Intervalo> = emptyList()
)

@HiltViewModel
class MiDisponibilidadViewModel @Inject constructor(
    private val api: HorariosVeterinarioControllerApi
) : ViewModel() {

    companion object {
        private const val TAG = "MiDisponibilidadVM"
    }

    private val _uiState = MutableStateFlow(MiDisponibilidadUiState())
    val uiState: StateFlow<MiDisponibilidadUiState> = _uiState.asStateFlow()

    /**
     * Carga horarios, excepciones y opcionalmente disponibilidad para una fecha.
     */
    fun cargarTodo(fecha: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                // Horarios
                val resHorarios = api.listarHorarios()
                val horarios = if (resHorarios.isSuccessful) {
                    resHorarios.body().orEmpty()
                } else {
                    _uiState.update {
                        it.copy(error = "Error al cargar horarios: ${resHorarios.code()}")
                    }
                    emptyList()
                }

                // Excepciones
                val resExcepciones = api.listarExcepciones()
                val excepciones = if (resExcepciones.isSuccessful) {
                    resExcepciones.body().orEmpty()
                } else {
                    _uiState.update {
                        it.copy(error = "Error al cargar excepciones: ${resExcepciones.code()}")
                    }
                    emptyList()
                }

                // Disponibilidad del día (opcional)
                val disponibilidad = if (fecha != null) {
                    val resDisp = api.disponibilidadEnFecha(fecha)
                    if (resDisp.isSuccessful) {
                        resDisp.body().orEmpty()
                    } else {
                        _uiState.update {
                            it.copy(error = "Error al cargar disponibilidad: ${resDisp.code()}")
                        }
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        horarios = horarios,
                        excepciones = excepciones,
                        disponibilidad = disponibilidad
                    )
                }

            } catch (t: Throwable) {
                _uiState.update { it.copy(error = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /**
     * Crear un nuevo horario y refrescar la lista.
     */
    fun crearHorario(horario: HorarioAtencion, fechaParaDisponibilidad: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val res = api.crearHorario(horario)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(error = "Error al crear horario: ${res.code()}")
                    }
                } else {
                    // recargar todo para reflejar cambios
                    cargarTodo(fechaParaDisponibilidad)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /**
     * Eliminar horario y refrescar.
     */
    fun eliminarHorario(horarioId: java.util.UUID, fechaParaDisponibilidad: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val res = api.eliminarHorario(horarioId)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(error = "Error al eliminar horario: ${res.code()}")
                    }
                } else {
                    cargarTodo(fechaParaDisponibilidad)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /**
     * Crear excepción y refrescar.
     */
    fun crearExcepcion(excepcion: ExcepcionHorario, fechaParaDisponibilidad: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val res = api.crearExcepcion(excepcion)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(error = "Error al crear excepción: ${res.code()}")
                    }
                } else {
                    cargarTodo(fechaParaDisponibilidad)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /**
     * Eliminar excepción y refrescar.
     */
    fun eliminarExcepcion(excepcionId: java.util.UUID, fechaParaDisponibilidad: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val res = api.eliminarExcepcion(excepcionId)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(error = "Error al eliminar excepción: ${res.code()}")
                    }
                } else {
                    cargarTodo(fechaParaDisponibilidad)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /**
     * Solo recargar disponibilidad para otra fecha, sin tocar horarios/excepciones.
     */
    fun cargarDisponibilidadParaFecha(fecha: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val res = api.disponibilidadEnFecha(fecha)
                if (res.isSuccessful) {
                    val disp = res.body().orEmpty()
                    _uiState.update { it.copy(disponibilidad = disp) }
                } else {
                    _uiState.update {
                        it.copy(error = "Error al cargar disponibilidad: ${res.code()}")
                    }
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }
}
