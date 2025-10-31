package cl.clinipets.feature.descubrimiento.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.Resultado
import cl.clinipets.feature.descubrimiento.domain.FiltrosProcedimientos
import cl.clinipets.feature.descubrimiento.domain.FiltrosVeterinarios
import cl.clinipets.feature.descubrimiento.domain.ObtenerProcedimientosUseCase
import cl.clinipets.feature.descubrimiento.domain.ObtenerVeterinariosUseCase
import cl.clinipets.feature.mascotas.domain.ObtenerMisMascotasUseCase
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.ProcedimientoItem
import cl.clinipets.openapi.models.VetItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DescubrimientoUiState(
    val cargando: Boolean = false,
    val mascotas: List<MascotaResumen> = emptyList(),
    val mascotaSeleccionada: MascotaResumen? = null,
    val veterinarios: List<VetItem> = emptyList(),
    val procedimientos: List<ProcedimientoItem> = emptyList(),
    val error: Resultado.Error? = null,
)

data class MascotaResumen(
    val id: java.util.UUID,
    val nombre: String,
    val especie: String,
)

@HiltViewModel
class DescubrimientoViewModel @Inject constructor(
    private val obtenerMisMascotas: ObtenerMisMascotasUseCase,
    private val obtenerVeterinarios: ObtenerVeterinariosUseCase,
    private val obtenerProcedimientos: ObtenerProcedimientosUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(DescubrimientoUiState())
    val estado: StateFlow<DescubrimientoUiState> = _estado.asStateFlow()

    init {
        cargarMascotas()
    }

    fun refrescar() {
        cargarMascotas()
    }

    private fun cargarMascotas() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = obtenerMisMascotas()) {
                is Resultado.Exito -> {
                    val mascotas = resultado.dato.mapNotNull { mascota ->
                        mascota.toResumenOrNull()
                    }
                    _estado.update {
                        it.copy(
                            cargando = false,
                            mascotas = mascotas,
                            mascotaSeleccionada = mascotas.firstOrNull(),
                            error = null,
                        )
                    }
                    if (mascotas.isNotEmpty()) {
                        cargarProcedimientos()
                        cargarVeterinarios()
                    }
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado)
                }
            }
        }
    }

    fun seleccionarMascota(mascota: MascotaResumen) {
        _estado.update { it.copy(mascotaSeleccionada = mascota) }
        cargarProcedimientos()
        cargarVeterinarios()
    }

    fun cargarProcedimientos(query: String? = null) {
        val mascota = _estado.value.mascotaSeleccionada ?: return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val filtros = FiltrosProcedimientos(
                especie = mascota.especie,
                q = query,
            )
            when (val resultado = obtenerProcedimientos(filtros)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        procedimientos = resultado.dato,
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado)
                }
            }
        }
    }

    fun cargarVeterinarios() {
        val mascota = _estado.value.mascotaSeleccionada ?: return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val filtros = FiltrosVeterinarios(
                especie = mascota.especie,
            )
            when (val resultado = obtenerVeterinarios(filtros)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        veterinarios = resultado.dato,
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado)
                }
            }
        }
    }
}

private fun Mascota.toResumenOrNull(): MascotaResumen? =
    id?.let {
        MascotaResumen(
            id = it,
            nombre = nombre,
            especie = especie.value,
        )
    }
