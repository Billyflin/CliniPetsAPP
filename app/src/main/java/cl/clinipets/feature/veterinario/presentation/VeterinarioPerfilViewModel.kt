package cl.clinipets.feature.veterinario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.Resultado
import cl.clinipets.feature.veterinario.domain.ObtenerPerfilVeterinarioUseCase
import cl.clinipets.openapi.models.VeterinarioPerfil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VeterinarioPerfilUiState(
    val cargando: Boolean = true,
    val perfil: VeterinarioPerfil? = null,
    val error: Resultado.Error? = null,
    val sinPerfil: Boolean = false,
)

@HiltViewModel
class VeterinarioPerfilViewModel @Inject constructor(
    private val obtenerPerfil: ObtenerPerfilVeterinarioUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(VeterinarioPerfilUiState())
    val estado: StateFlow<VeterinarioPerfilUiState> = _estado.asStateFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, sinPerfil = false) }
            when (val resultado = obtenerPerfil()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        perfil = resultado.dato,
                        error = null,
                        sinPerfil = false,
                    )
                }

                is Resultado.Error -> _estado.update {
                    if (resultado.codigoHttp == 404) {
                        it.copy(
                            cargando = false,
                            perfil = null,
                            error = null,
                            sinPerfil = true,
                        )
                    } else {
                        it.copy(
                            cargando = false,
                            error = resultado,
                            sinPerfil = false,
                        )
                    }
                }
            }
        }
    }
}
