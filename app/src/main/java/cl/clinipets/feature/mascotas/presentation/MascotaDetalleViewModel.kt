package cl.clinipets.feature.mascotas.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.Resultado
import cl.clinipets.feature.mascotas.domain.EliminarMascotaUseCase
import cl.clinipets.feature.mascotas.domain.ObtenerMascotaUseCase
import cl.clinipets.navigation.AppDestination
import cl.clinipets.openapi.models.Mascota
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class MascotaDetalleUiState(
    val cargando: Boolean = true,
    val mascota: Mascota? = null,
    val error: Resultado.Error? = null,
    val mostrandoConfirmacion: Boolean = false,
)

sealed class MascotaDetalleEvento {
    data object MascotaEliminada : MascotaDetalleEvento()
}

@HiltViewModel
class MascotaDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerMascota: ObtenerMascotaUseCase,
    private val eliminarMascota: EliminarMascotaUseCase,
) : ViewModel() {

    private val mascotaId: UUID = savedStateHandle.get<String>(AppDestination.MascotaDetalle.ARG_ID)
        ?.let(UUID::fromString)
        ?: error("Mascota id requerido")

    private val _estado = MutableStateFlow(MascotaDetalleUiState())
    val estado: StateFlow<MascotaDetalleUiState> = _estado.asStateFlow()

    private val _eventos = MutableSharedFlow<MascotaDetalleEvento>()
    val eventos: SharedFlow<MascotaDetalleEvento> = _eventos.asSharedFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = obtenerMascota(mascotaId)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(cargando = false, mascota = resultado.dato, error = null)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado)
                }
            }
        }
    }

    fun mostrarConfirmacionEliminar(mostrar: Boolean) {
        _estado.update { it.copy(mostrandoConfirmacion = mostrar) }
    }

    fun eliminarMascota() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, mostrandoConfirmacion = false) }
            when (val resultado = eliminarMascota(mascotaId)) {
                is Resultado.Exito -> {
                    _estado.update { it.copy(cargando = false) }
                    _eventos.emit(MascotaDetalleEvento.MascotaEliminada)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado)
                }
            }
        }
    }
}
