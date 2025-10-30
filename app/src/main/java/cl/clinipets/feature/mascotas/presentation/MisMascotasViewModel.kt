package cl.clinipets.feature.mascotas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.Resultado
import cl.clinipets.feature.auth.domain.ObservarSesionUseCase
import cl.clinipets.feature.mascotas.domain.ObtenerMisMascotasUseCase
import cl.clinipets.openapi.models.Mascota
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MisMascotasUiState(
    val cargando: Boolean = false,
    val mascotas: List<Mascota> = emptyList(),
    val error: Resultado.Error? = null,
    val requiereSesion: Boolean = true,
)

@HiltViewModel
class MisMascotasViewModel @Inject constructor(
    private val obtenerMisMascotas: ObtenerMisMascotasUseCase,
    private val observarSesion: ObservarSesionUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(MisMascotasUiState())
    val estado: StateFlow<MisMascotasUiState> = _estado.asStateFlow()

    init {
        observarSesionActiva()
    }

    fun recargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = obtenerMisMascotas()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(cargando = false, mascotas = resultado.dato)
                }
                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado)
                }
            }
        }
    }

    private fun observarSesionActiva() {
        viewModelScope.launch {
            observarSesion().collectLatest { sesion ->
                if (sesion == null) {
                    _estado.update {
                        it.copy(
                            requiereSesion = true,
                            cargando = false,
                            mascotas = emptyList(),
                            error = null,
                        )
                    }
                } else {
                    val requiereSesionPrevio = _estado.value.requiereSesion
                    _estado.update { it.copy(requiereSesion = false, error = null) }
                    if (requiereSesionPrevio) {
                        recargar()
                    }
                }
            }
        }
    }
}
