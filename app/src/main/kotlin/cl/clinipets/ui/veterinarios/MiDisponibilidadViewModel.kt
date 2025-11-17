package cl.clinipets.ui.veterinarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.HorariosControllerApi
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
import java.time.LocalTime
import javax.inject.Inject

data class MiDisponibilidadUiState(
    val cargando: Boolean = false,
    val userMessage: String? = null,
    val horarios: List<HorarioAtencion> = emptyList(),
    val excepciones: List<ExcepcionHorario> = emptyList(),
    val disponibilidad: List<Intervalo> = emptyList()
)

@HiltViewModel
class MiDisponibilidadViewModel @Inject constructor(
    private val api: HorariosControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MiDisponibilidadUiState())
    val uiState: StateFlow<MiDisponibilidadUiState> = _uiState.asStateFlow()

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun cargarTodo(fecha: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                // Horarios (vet)
                val resHorarios = api.listarHorariosVet()
                val horarios = if (resHorarios.isSuccessful) {
                    resHorarios.body().orEmpty()
                } else {
                    _uiState.update {
                        it.copy(userMessage = "Error al cargar horarios: ${resHorarios.code()}")
                    }
                    emptyList()
                }

                // Excepciones (vet)
                val resExcepciones = api.listarExcepcionesVet()
                val excepciones = if (resExcepciones.isSuccessful) {
                    resExcepciones.body().orEmpty()
                } else {
                    _uiState.update {
                        it.copy(userMessage = "Error al cargar excepciones: ${resExcepciones.code()}")
                    }
                    emptyList()
                }

                // Disponibilidad del día (opcional) usando tipo VETERINARIO
                val disponibilidad = if (fecha != null) {
                    val resDisp = api.disponibilidad(HorariosControllerApi.TipoDisponibilidad.VETERINARIO, fecha)
                    if (resDisp.isSuccessful) {
                        resDisp.body().orEmpty()
                    } else {
                        _uiState.update {
                            it.copy(userMessage = "Error al cargar disponibilidad: ${resDisp.code()}")
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
                _uiState.update { it.copy(userMessage = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /** Crear un nuevo horario (vet) y refrescar la lista. */
    fun crearHorario(horario: HorarioAtencion, fechaParaDisponibilidad: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                val res = api.crearHorarioVet(horario)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(userMessage = "Error al crear horario: ${res.code()}")
                    }
                } else {
                    // recargar todo para reflejar cambios
                    cargarTodo(fechaParaDisponibilidad)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(userMessage = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /** Eliminar horario (vet) y refrescar. */
    fun eliminarHorario(horarioId: java.util.UUID, fechaParaDisponibilidad: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                val res = api.eliminarHorarioVet(horarioId)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(userMessage = "Error al eliminar horario: ${res.code()}")
                    }
                } else {
                    cargarTodo(fechaParaDisponibilidad)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(userMessage = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /** Crear excepción (vet) y refrescar. */
    fun crearExcepcion(excepcion: ExcepcionHorario, fechaParaDisponibilidad: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                val res = api.crearExcepcionVet(excepcion)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(userMessage = "Error al crear excepción: ${res.code()}")
                    }
                } else {
                    cargarTodo(fechaParaDisponibilidad)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(userMessage = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /** Eliminar excepción (vet) y refrescar. */
    fun eliminarExcepcion(excepcionId: java.util.UUID, fechaParaDisponibilidad: LocalDate? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                val res = api.eliminarExcepcionVet(excepcionId)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(userMessage = "Error al eliminar excepción: ${res.code()}")
                    }
                } else {
                    cargarTodo(fechaParaDisponibilidad)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(userMessage = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    /** Crear intervalo semanal validando solapamientos antes de llamar a la API */
    fun crearIntervalo(
        dia: HorarioAtencion.DiaSemana,
        inicio: LocalTime,
        fin: LocalTime,
        fechaParaDisponibilidad: LocalDate? = null
    ) {
        if (fin <= inicio) {
            _uiState.update { it.copy(userMessage = "Hora fin debe ser posterior a inicio") }
            return
        }
        if (haySolape(dia, inicio, fin)) {
            _uiState.update { it.copy(userMessage = "Intervalo solapado con otro existente") }
            return
        }
        val nuevo = HorarioAtencion(
            id = null,
            tipoOwner = HorarioAtencion.TipoOwner.VETERINARIO,
            diaSemana = dia,
            horaInicio = inicio.toString(),
            horaFin = fin.toString()
        )
        crearHorario(nuevo, fechaParaDisponibilidad)
    }

    /** Eliminar intervalo semanal directo */
    fun eliminarIntervalo(horario: HorarioAtencion, fechaParaDisponibilidad: LocalDate? = null) {
        val id = horario.id ?: return
        eliminarHorario(id, fechaParaDisponibilidad)
    }

    /** Excepción de cierre total de un día específico */
    fun crearExcepcionCierre(fecha: LocalDate, motivo: String = "Cerrado") {
        val nueva = ExcepcionHorario(
            id = null,
            tipoOwner = ExcepcionHorario.TipoOwner.VETERINARIO,
            fecha = fecha,
            cerrado = true,
            horaInicio = null,
            horaFin = null,
            motivo = motivo
        )
        crearExcepcion(nueva, fecha)
    }

    /** Excepción para rango horario dentro de un día */
    fun crearExcepcionRango(
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        motivo: String = "Excepción"
    ) {
        if (fin <= inicio) {
            _uiState.update { it.copy(userMessage = "Fin debe ser posterior a inicio") }
            return
        }
        val nueva = ExcepcionHorario(
            id = null,
            tipoOwner = ExcepcionHorario.TipoOwner.VETERINARIO,
            fecha = fecha,
            cerrado = false,
            horaInicio = inicio.toString(),
            horaFin = fin.toString(),
            motivo = motivo
        )
        crearExcepcion(nueva, fecha)
    }

    /** Agrega un intervalo a toda la semana, omitiendo días con solape. */
    fun crearIntervaloSemanal(inicio: LocalTime, fin: LocalTime) {
        viewModelScope.launch {
            if (fin <= inicio) {
                _uiState.update { it.copy(userMessage = "Hora fin debe ser posterior a inicio") }
                return@launch
            }
            _uiState.update { it.copy(cargando = true) }
            val dias = HorarioAtencion.DiaSemana.entries
            val agregados = mutableListOf<String>()
            val omitidos = mutableListOf<String>()
            try {
                for (dia in dias) {
                    if (haySolape(dia, inicio, fin)) {
                        omitidos += nombreCortoDia(dia)
                    } else {
                        val ok = crearHorarioEn(dia, inicio, fin)
                        if (ok) agregados += nombreCortoDia(dia) else omitidos += nombreCortoDia(dia)
                    }
                }
                val msg = buildString {
                    if (agregados.isNotEmpty()) append("Agregado en: ${agregados.joinToString(", ")}. ")
                    if (omitidos.isNotEmpty()) append("Omitido por solape/errores: ${omitidos.joinToString(", ")}")
                    if (isEmpty()) append("No se pudo agregar en ningún día")
                }
                _uiState.update { it.copy(userMessage = msg) }
                cargarTodo(null)
            } catch (t: Throwable) {
                _uiState.update { it.copy(userMessage = t.message ?: "Error desconocido") }
            } finally {
                _uiState.update { it.copy(cargando = false) }
            }
        }
    }

    // Plantillas rápidas
    fun plantillaMananaSemana() = crearIntervaloSemanal(LocalTime.of(9, 0), LocalTime.of(13, 0))
    fun plantillaTardeSemana() = crearIntervaloSemanal(LocalTime.of(15, 0), LocalTime.of(19, 0))
    fun plantillaMananaDia(dia: HorarioAtencion.DiaSemana) = crearIntervalo(dia, LocalTime.of(9, 0), LocalTime.of(13, 0), null)
    fun plantillaTardeDia(dia: HorarioAtencion.DiaSemana) = crearIntervalo(dia, LocalTime.of(15, 0), LocalTime.of(19, 0), null)

    // --- Funciones Helper ---

    private fun nombreCortoDia(dia: HorarioAtencion.DiaSemana): String = when (dia) {
        HorarioAtencion.DiaSemana.MONDAY -> "Lun"
        HorarioAtencion.DiaSemana.TUESDAY -> "Mar"
        HorarioAtencion.DiaSemana.WEDNESDAY -> "Mié"
        HorarioAtencion.DiaSemana.THURSDAY -> "Jue"
        HorarioAtencion.DiaSemana.FRIDAY -> "Vie"
        HorarioAtencion.DiaSemana.SATURDAY -> "Sáb"
        HorarioAtencion.DiaSemana.SUNDAY -> "Dom"
    }

    /** Verifica solapamiento para un nuevo intervalo */
    private fun haySolape(dia: HorarioAtencion.DiaSemana, inicio: LocalTime, fin: LocalTime): Boolean {
        val existentes = _uiState.value.horarios.filter { it.diaSemana == dia }
        return existentes.any { h ->
            val hi = LocalTime.parse(h.horaInicio)
            val hf = LocalTime.parse(h.horaFin)
            inicio < hf && fin > hi
        }
    }

    /** Verifica solapamiento al actualizar, excluyendo el ID del ítem que se está editando */
    private fun haySolapeExcluyendo(
        dia: HorarioAtencion.DiaSemana,
        inicio: LocalTime,
        fin: LocalTime,
        excluirId: java.util.UUID
    ): Boolean {
        val existentes = _uiState.value.horarios.filter { it.diaSemana == dia && it.id != excluirId }
        return existentes.any { h ->
            val hi = LocalTime.parse(h.horaInicio)
            val hf = LocalTime.parse(h.horaFin)
            inicio < hf && fin > hi
        }
    }

    /** Helper de bajo nivel para crear un horario (usado por crearIntervaloSemanal) */
    private suspend fun crearHorarioEn(dia: HorarioAtencion.DiaSemana, inicio: LocalTime, fin: LocalTime): Boolean {
        val nuevo = HorarioAtencion(
            id = null,
            tipoOwner = HorarioAtencion.TipoOwner.VETERINARIO,
            diaSemana = dia,
            horaInicio = inicio.toString(),
            horaFin = fin.toString()
        )
        val res = api.crearHorarioVet(nuevo)
        return res.isSuccessful
    }

    // --- LÓGICA DE ACTUALIZACIÓN (Borrar y Crear) ---

    /**
     * "Actualiza" un horario haciendo un Borrar-y-Crear.
     */
    fun actualizarHorario(
        horarioId: java.util.UUID,
        dia: HorarioAtencion.DiaSemana,
        inicio: LocalTime,
        fin: LocalTime
    ) {
        if (fin <= inicio) {
            _uiState.update { it.copy(userMessage = "Hora fin debe ser posterior a inicio") }
            return
        }
        // Validar solapamiento, pero excluyendo el horario que estamos editando
        if (haySolapeExcluyendo(dia, inicio, fin, horarioId)) {
            _uiState.update { it.copy(userMessage = "Intervalo solapado con otro existente") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                // 1. Borrar el antiguo
                val resDelete = api.eliminarHorarioVet(horarioId)
                if (!resDelete.isSuccessful) {
                    _uiState.update { it.copy(userMessage = "Error al borrar el horario original: ${resDelete.code()}", cargando = false) }
                    return@launch
                }

                // 2. Crear el nuevo (reusando el helper)
                val okCreate = crearHorarioEn(dia, inicio, fin)

                if (!okCreate) {
                    // Si falla la creación, el horario original ya fue borrado.
                    _uiState.update { it.copy(userMessage = "Error al crear el nuevo horario. El original fue borrado.") }
                }

                // Recargar siempre al final para reflejar el estado real de la BD
                cargarTodo(null)

            } catch (t: Throwable) {
                _uiState.update { it.copy(userMessage = t.message ?: "Error desconocido", cargando = false) }
            }
        }
    }

    /**
     * "Actualiza" una excepción haciendo un Borrar-y-Crear.
     */
    fun actualizarExcepcion(
        excepcionId: java.util.UUID,
        fecha: LocalDate,
        inicio: LocalTime?, // Nullable si es 'cerrado'
        fin: LocalTime?,    // Nullable si es 'cerrado'
        cerrado: Boolean,
        motivo: String
    ) {
        if (!cerrado && (inicio == null || fin == null || fin <= inicio)) {
            _uiState.update { it.copy(userMessage = "Rango horario inválido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            try {
                // 1. Borrar la antigua
                val resDelete = api.eliminarExcepcionVet(excepcionId)
                if (!resDelete.isSuccessful) {
                    _uiState.update { it.copy(userMessage = "Error al borrar la excepción original: ${resDelete.code()}", cargando = false) }
                    return@launch
                }

                // 2. Crear la nueva
                val nuevaExcepcion = ExcepcionHorario(
                    id = null, // Nuevo ID será asignado por la BD
                    tipoOwner = ExcepcionHorario.TipoOwner.VETERINARIO,
                    fecha = fecha,
                    cerrado = cerrado,
                    horaInicio = if (cerrado) null else inicio.toString(),
                    horaFin = if (cerrado) null else fin.toString(),
                    motivo = motivo
                )

                val resCreate = api.crearExcepcionVet(nuevaExcepcion)

                if (!resCreate.isSuccessful) {
                    _uiState.update { it.copy(userMessage = "Error al crear la nueva excepción. La original fue borrada.") }
                }

                // Recargar siempre
                cargarTodo(fecha)

            } catch (t: Throwable) {
                _uiState.update { it.copy(userMessage = t.message ?: "Error desconocido", cargando = false) }
            }
        }
    }
}