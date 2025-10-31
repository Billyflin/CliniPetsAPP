package cl.clinipets.feature.perfil.presentation

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

data class PerfilVeterinarioUiState(
    val cargando: Boolean = true,
    val perfil: VeterinarioPerfil? = null,
    val error: Resultado.Error? = null,
    val sinPerfil: Boolean = false,
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val obtenerPerfilVeterinario: ObtenerPerfilVeterinarioUseCase,
) : ViewModel() {

    private val _estadoVeterinario = MutableStateFlow(PerfilVeterinarioUiState())
    val estadoVeterinario: StateFlow<PerfilVeterinarioUiState> = _estadoVeterinario.asStateFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            _estadoVeterinario.update { it.copy(cargando = true, error = null, sinPerfil = false) }
            when (val resultado = obtenerPerfilVeterinario()) {
                is Resultado.Exito -> _estadoVeterinario.update {
                    it.copy(
                        cargando = false,
                        perfil = resultado.dato,
                        error = null,
                        sinPerfil = false,
                    )
                }

                is Resultado.Error -> _estadoVeterinario.update { anterior ->
                    if (resultado.codigoHttp == 404) {
                        anterior.copy(
                            cargando = false,
                            perfil = null,
                            error = null,
                            sinPerfil = true,
                        )
                    } else {
                        anterior.copy(
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
