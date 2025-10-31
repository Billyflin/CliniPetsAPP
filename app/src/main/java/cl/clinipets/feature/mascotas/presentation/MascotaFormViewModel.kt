package cl.clinipets.feature.mascotas.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.Resultado
import cl.clinipets.feature.mascotas.domain.ActualizarMascotaUseCase
import cl.clinipets.feature.mascotas.domain.CrearMascotaUseCase
import cl.clinipets.feature.mascotas.domain.ObtenerMascotaUseCase
import cl.clinipets.navigation.AppDestination
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.Mascota
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MascotaFormularioModo { CREAR, EDITAR }

data class MascotaFormUiState(
    val modo: MascotaFormularioModo,
    val cargando: Boolean = modo == MascotaFormularioModo.EDITAR,
    val guardando: Boolean = false,
    val nombre: String = "",
    val especie: CrearMascota.Especie? = null,
    val raza: String = "",
    val sexo: String = "",
    val fechaNacimiento: String = "",
    val pesoKg: String = "",
    val error: Resultado.Error? = null,
    val nombreError: String? = null,
    val especieError: String? = null,
)

sealed class MascotaFormEvento {
    data class MascotaGuardada(val id: UUID, val fueEdicion: Boolean) : MascotaFormEvento()
}

@HiltViewModel
class MascotaFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val crearMascota: CrearMascotaUseCase,
    private val actualizarMascota: ActualizarMascotaUseCase,
    private val obtenerMascota: ObtenerMascotaUseCase,
) : ViewModel() {

    private val mascotaId: UUID? = savedStateHandle.get<String>(AppDestination.MascotaEditar.ARG_ID)
        ?.let(UUID::fromString)

    private val modo = if (mascotaId == null) MascotaFormularioModo.CREAR else MascotaFormularioModo.EDITAR

    private val _estado = MutableStateFlow(MascotaFormUiState(modo = modo))
    val estado: StateFlow<MascotaFormUiState> = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<MascotaFormEvento>()
    val eventos: SharedFlow<MascotaFormEvento> = _eventos.asSharedFlow()

    init {
        if (modo == MascotaFormularioModo.EDITAR && mascotaId != null) {
            cargarMascota(mascotaId)
        }
    }

    fun onNombreChange(value: String) {
        _estado.update { it.copy(nombre = value, nombreError = null) }
    }

    fun onEspecieChange(value: CrearMascota.Especie) {
        _estado.update { it.copy(especie = value, especieError = null) }
    }

    fun onRazaChange(value: String) {
        _estado.update { it.copy(raza = value) }
    }

    fun onSexoChange(value: String) {
        _estado.update { it.copy(sexo = value) }
    }

    fun onFechaNacimientoChange(value: String) {
        _estado.update { it.copy(fechaNacimiento = value) }
    }

    fun onPesoChange(value: String) {
        _estado.update { it.copy(pesoKg = value) }
    }

    fun guardar() {
        val estadoActual = estado.value
        val errores = validar(estadoActual)
        if (errores != null) {
            _estado.update { it.copy(nombreError = errores.nombre, especieError = errores.especie) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null) }
            when (modo) {
                MascotaFormularioModo.CREAR -> guardarNuevaMascota()
                MascotaFormularioModo.EDITAR -> guardarMascotaExistente()
            }
        }
    }

    private suspend fun guardarNuevaMascota() {
        val estadoActual = estado.value
        val especie = estadoActual.especie ?: return
        val solicitud = CrearMascota(
            nombre = estadoActual.nombre.trim(),
            especie = especie,
            raza = estadoActual.raza.takeIf { it.isNotBlank() },
            sexo = estadoActual.sexo.takeIf { it.isNotBlank() },
            fechaNacimiento = estadoActual.fechaNacimiento.toLocalDateOrNull(),
            pesoKg = estadoActual.pesoKg.toDoubleOrNull(),
        )
        when (val resultado = crearMascota(solicitud)) {
            is Resultado.Exito -> {
                val id = resultado.dato.id
                if (id != null) {
                    _estado.update { it.copy(guardando = false) }
                    _eventos.emit(MascotaFormEvento.MascotaGuardada(id, fueEdicion = false))
                } else {
                    _estado.update {
                        it.copy(
                            guardando = false,
                            error = Resultado.Error(Resultado.Tipo.DESCONOCIDO, "El servidor no retornó ID de la mascota."),
                        )
                    }
                }
            }

            is Resultado.Error -> _estado.update {
                it.copy(guardando = false, error = resultado)
            }
        }
    }

    private suspend fun guardarMascotaExistente() {
        val id = mascotaId ?: return
        val estadoActual = estado.value
        val solicitud = ActualizarMascota(
            nombre = estadoActual.nombre.trim().takeIf { it.isNotBlank() },
            raza = estadoActual.raza.takeIf { it.isNotBlank() },
            sexo = estadoActual.sexo.takeIf { it.isNotBlank() },
            fechaNacimiento = estadoActual.fechaNacimiento.toLocalDateOrNull(),
            pesoKg = estadoActual.pesoKg.toDoubleOrNull(),
        )
        when (val resultado = actualizarMascota(id, solicitud)) {
            is Resultado.Exito -> {
                val mascotaActualizada = resultado.dato
                val mascotaId = mascotaActualizada.id ?: id
                _estado.update { it.copy(guardando = false) }
                _eventos.emit(MascotaFormEvento.MascotaGuardada(mascotaId, fueEdicion = true))
            }

            is Resultado.Error -> _estado.update {
                it.copy(guardando = false, error = resultado)
            }
        }
    }

    private fun validar(estado: MascotaFormUiState): CamposError? {
        var nombreError: String? = null
        var especieError: String? = null

        if (estado.nombre.isBlank()) {
            nombreError = "Ingresa un nombre"
        }
        if (estado.modo == MascotaFormularioModo.CREAR && estado.especie == null) {
            especieError = "Selecciona una especie"
        }

        return if (nombreError != null || especieError != null) {
            CamposError(nombreError, especieError)
        } else {
            null
        }
    }

    private fun cargarMascota(id: UUID) {
        viewModelScope.launch {
            when (val resultado = obtenerMascota(id)) {
                is Resultado.Exito -> _estado.update { estadoActual ->
                    val mascota = resultado.dato
                    estadoActual.copy(
                        cargando = false,
                        nombre = mascota.nombre,
                        especie = try {
                            CrearMascota.Especie.valueOf(mascota.especie.name)
                        } catch (ex: IllegalArgumentException) {
                            null
                        },
                        raza = mascota.raza.orEmpty(),
                        sexo = mascota.sexo.orEmpty(),
                        fechaNacimiento = mascota.fechaNacimiento?.toString().orEmpty(),
                        pesoKg = mascota.pesoKg?.toString().orEmpty(),
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado)
                }
            }
        }
    }

    private fun String.toLocalDateOrNull(): LocalDate? =
        takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }

    private data class CamposError(
        val nombre: String?,
        val especie: String?,
    )
}
