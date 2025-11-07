package cl.clinipets.ui.veterinarios

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.ActualizarDisponibilidadRequest
import cl.clinipets.openapi.models.BloqueHorario
import cl.clinipets.openapi.models.BloqueHorarioDto
import cl.clinipets.openapi.models.DisponibilidadSemanal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val REGEX_HH_MM = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
private val REGEX_HH_MM_SS = Regex("^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$")

@HiltViewModel
class MiDisponibilidadViewModel @Inject constructor(
    private val api: VeterinariosApi
) : ViewModel() {

    companion object {
        private const val TAG = "MiDisponibilidadVM"
    }

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _miDisponibilidadState = MutableStateFlow(MiDisponibilidadState())
    val miDisponibilidadState: StateFlow<MiDisponibilidadState> =
        _miDisponibilidadState.asStateFlow()

    fun limpiarError() {
        _error.value = null
        _miDisponibilidadState.update { it.copy(errorMessage = null) }
    }

    fun cargarDisponibilidad() {
        viewModelScope.launch {
            _cargando.value = true
            _miDisponibilidadState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val resp = api.miDisponibilidad()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _miDisponibilidadState.update {
                        it.copy(
                            disponibilidad = body,
                            bloquesEditables = body.toEditableBloques(),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    setDisponibilidadError("Error obteniendo disponibilidad (${resp.code()})")
                }
            } catch (t: Throwable) {
                setDisponibilidadError(t.message ?: "Error desconocido")
            } finally {
                _cargando.value = false
                _miDisponibilidadState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetEdicionDisponibilidad() {
        _miDisponibilidadState.update { it.copy(bloquesEditables = it.disponibilidad.toEditableBloques()) }
    }

    fun addBloque(
        dia: BloqueHorarioDto.Dia,
        inicio: String = "09:00",
        fin: String = "18:00",
        habilitado: Boolean = true
    ) {
        modifyBloques {
            it += BloqueHorarioDto(
                dia = dia, inicio = inicio, fin = fin, habilitado = habilitado
            )
        }
    }

    fun addBloqueCustom(
        dia: BloqueHorarioDto.Dia, inicio: String, fin: String, habilitado: Boolean
    ) {
        modifyBloques {
            it += BloqueHorarioDto(
                dia = dia, inicio = inicio, fin = fin, habilitado = habilitado
            )
        }
    }

    fun removeBloque(indexGlobal: Int) {
        modifyBloques { list -> if (indexGlobal in list.indices) list.removeAt(indexGlobal) }
    }

    fun setInicio(indexGlobal: Int, value: String) {
        modifyBloques { list ->
            if (indexGlobal in list.indices) list[indexGlobal] =
                list[indexGlobal].copy(inicio = value)
        }
    }

    fun setFin(indexGlobal: Int, value: String) {
        modifyBloques { list ->
            if (indexGlobal in list.indices) list[indexGlobal] = list[indexGlobal].copy(fin = value)
        }
    }

    fun setHabilitado(indexGlobal: Int, habilitado: Boolean) {
        modifyBloques { list ->
            if (indexGlobal in list.indices) list[indexGlobal] =
                list[indexGlobal].copy(habilitado = habilitado)
        }
    }

    fun setDia(indexGlobal: Int, dia: BloqueHorarioDto.Dia) {
        modifyBloques { list ->
            if (indexGlobal in list.indices) list[indexGlobal] = list[indexGlobal].copy(dia = dia)
        }
    }

    fun removeBloquesDia(dia: BloqueHorarioDto.Dia) {
        _miDisponibilidadState.update { it.copy(bloquesEditables = it.bloquesEditables.filterNot { bloque -> bloque.dia == dia }) }
    }

    fun puedeAgregarBloque(dia: BloqueHorarioDto.Dia, inicio: String, fin: String): String? {
        val inicioNormalizado = normalizeHoraForValidation(inicio)
        if (inicioNormalizado == null) {
            Log.w(
                TAG,
                "Nuevo bloque inválido: inicio='$inicio' para $dia. " + "Formato esperado HH:mm. ¿Trae segundos u otro separador?"
            )
            return "Hora de inicio inválida"
        }
        val finNormalizado = normalizeHoraForValidation(fin)
        if (finNormalizado == null) {
            Log.w(
                TAG,
                "Nuevo bloque inválido: fin='$fin' para $dia. " + "Formato esperado HH:mm. ¿Trae segundos u otro separador?"
            )
            return "Hora de fin inválida"
        }
        if (toMin(inicioNormalizado) >= toMin(finNormalizado)) {
            Log.w(
                TAG,
                "Nuevo bloque inválido: inicio='${inicioNormalizado}' >= fin='${finNormalizado}' ($dia). " + "El sistema requiere inicio < fin."
            )
            return "El inicio debe ser menor que el fin"
        }
        return null
    }

    fun guardarDisponibilidad(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val bloques = _miDisponibilidadState.value.bloquesEditables
                validarBloques(bloques)?.let { msg ->
                    setDisponibilidadError(msg)
                    _cargando.value = false
                    return@launch
                }
                _miDisponibilidadState.update { it.copy(isSaving = true, errorMessage = null) }
                val payloadBloques = bloques.mapIndexed { idx, bloque ->
                    bloque.copy(
                        inicio = toPayloadHora(bloque.inicio)
                            ?: error("Hora inicio inválida tras validación en bloque #${idx + 1}: ${bloque.inicio}"),
                        fin = toPayloadHora(bloque.fin)
                            ?: error("Hora fin inválida tras validación en bloque #${idx + 1}: ${bloque.fin}")
                    )
                }
                val resp =
                    api.actualizarMiDisponibilidad(ActualizarDisponibilidadRequest(bloques = payloadBloques))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _miDisponibilidadState.update {
                        it.copy(
                            disponibilidad = body,
                            bloquesEditables = body.toEditableBloques(),
                            isSaving = false,
                            errorMessage = null
                        )
                    }
                    onSuccess?.invoke()
                } else {
                    setDisponibilidadError("Error guardando disponibilidad (${resp.code()})")
                }
            } catch (t: Throwable) {
                setDisponibilidadError(t.message ?: "Error desconocido")
            } finally {
                _cargando.value = false
                _miDisponibilidadState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun validarBloques(bloques: List<BloqueHorarioDto>): String? {
        bloques.forEachIndexed { idx, bloque ->
            val inicioNormalizado = normalizeHoraForValidation(bloque.inicio)
            if (inicioNormalizado == null) {
                Log.w(
                    TAG,
                    "Validación: bloque #${idx + 1} con inicio inválido='${bloque.inicio}'. " + "Formato esperado HH:mm (24h). Valor normalizado='${bloque.inicio.trim()}'"
                )
                return "Bloque #${idx + 1}: hora inicio inválida"
            }

            val finNormalizado = normalizeHoraForValidation(bloque.fin)
            if (finNormalizado == null) {
                Log.w(
                    TAG,
                    "Validación: bloque #${idx + 1} con fin inválido='${bloque.fin}'. " + "Formato esperado HH:mm (24h). Valor normalizado='${bloque.fin.trim()}'"
                )
                return "Bloque #${idx + 1}: hora fin inválida"
            }

            if (toMin(inicioNormalizado) >= toMin(finNormalizado)) {
                Log.w(
                    TAG,
                    "Validación: bloque #${idx + 1} inicio='${bloque.inicio}' >= fin='${bloque.fin}'. " + "Se requiere inicio estrictamente menor."
                )
                return "Bloque #${idx + 1}: inicio debe ser menor que fin"
            }
        }
        return null
    }

    private fun modifyBloques(block: (MutableList<BloqueHorarioDto>) -> Unit) {
        _miDisponibilidadState.update { state ->
            val mutable = state.bloquesEditables.toMutableList()
            block(mutable)
            state.copy(bloquesEditables = mutable)
        }
    }

    private fun setDisponibilidadError(message: String) {
        Log.e(TAG, "Error disponibilidad: $message")
        _error.value = message
        _miDisponibilidadState.update {
            it.copy(
                errorMessage = message, isLoading = false, isSaving = false
            )
        }
    }
}

data class MiDisponibilidadState(
    val disponibilidad: DisponibilidadSemanal? = null,
    val bloquesEditables: List<BloqueHorarioDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isDirty: Boolean
        get() = (disponibilidad?.bloques?.map {
            it.toDto().copy(
                inicio = normalizeHoraForDisplay(it.inicio), fin = normalizeHoraForDisplay(it.fin)
            )
        } ?: emptyList()) != bloquesEditables
}

private fun normalizeHoraForValidation(value: String): String? {
    val trimmed = value.trim()
    return when {
        REGEX_HH_MM.matches(trimmed) -> trimmed
        REGEX_HH_MM_SS.matches(trimmed) -> trimmed.take(5)
        else -> null
    }
}

private fun normalizeHoraForDisplay(value: String): String =
    normalizeHoraForValidation(value) ?: value.trim()

private fun toPayloadHora(value: String): String? {
    val trimmed = value.trim()
    return when {
        REGEX_HH_MM.matches(trimmed) -> "$trimmed:00"
        REGEX_HH_MM_SS.matches(trimmed) -> trimmed
        else -> null
    }
}

private fun toMin(hhmm: String): Int {
    val p = hhmm.split(":")
    return p[0].toInt() * 60 + p[1].toInt()
}

private fun DisponibilidadSemanal?.toEditableBloques(): List<BloqueHorarioDto> =
    this?.bloques?.map { bloque ->
        bloque.toDto().copy(
            inicio = normalizeHoraForDisplay(bloque.inicio),
            fin = normalizeHoraForDisplay(bloque.fin)
        )
    } ?: emptyList()

private fun BloqueHorario.toDto(): BloqueHorarioDto = BloqueHorarioDto(
    dia = BloqueHorarioDto.Dia.valueOf(this.dia.name),
    inicio = this.inicio,
    fin = this.fin,
    habilitado = this.habilitado
)
