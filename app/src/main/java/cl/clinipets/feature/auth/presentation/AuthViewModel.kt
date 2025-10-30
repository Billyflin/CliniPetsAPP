package cl.clinipets.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.Resultado
import cl.clinipets.feature.auth.data.GoogleAuthProvider
import cl.clinipets.feature.auth.domain.CerrarSesionUseCase
import cl.clinipets.feature.auth.domain.IniciarSesionConGoogleUseCase
import cl.clinipets.feature.auth.domain.ObtenerPerfilUseCase
import cl.clinipets.feature.auth.domain.ObservarSesionUseCase
import cl.clinipets.feature.auth.domain.Sesion
import cl.clinipets.openapi.models.MeResponse
import android.content.Intent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val cargando: Boolean = false,
    val sesion: Sesion? = null,
    val perfil: MeResponse? = null,
    val error: Resultado.Error? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val observarSesion: ObservarSesionUseCase,
    private val iniciarSesionConGoogle: IniciarSesionConGoogleUseCase,
    private val obtenerPerfil: ObtenerPerfilUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val googleAuthProvider: GoogleAuthProvider,
) : ViewModel() {

    private val _estado = MutableStateFlow(AuthUiState())
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            observarSesion().collect { sesion ->
                _estado.update {
                    it.copy(sesion = sesion, error = null, cargando = false).let { nuevo ->
                        if (sesion == null) {
                            nuevo.copy(perfil = null)
                        } else {
                            nuevo
                        }
                    }
                }
                if (sesion != null) {
                    cargarPerfil()
                }
            }
        }
    }

    fun obtenerIntentGoogle(): Intent = googleAuthProvider.getSignInIntent()

    fun procesarResultadoGoogle(data: Intent?) {
        _estado.update { it.copy(error = null) }
        when (val resultado = googleAuthProvider.extractIdTokenFromIntent(data)) {
            is Resultado.Exito -> iniciarSesion(resultado.dato)
            is Resultado.Error -> _estado.update {
                it.copy(
                    cargando = false,
                    error = resultado,
                )
            }
        }
    }

    fun cancelarGoogleSignIn() {
        _estado.update { it.copy(cargando = false, error = null) }
    }

    fun iniciarSesion(idToken: String) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = iniciarSesionConGoogle(idToken)) {
                is Resultado.Exito -> {
                    // La sesión se actualizará vía DataStore collector.
                    _estado.update { it.copy(cargando = false, error = null) }
                }

                is Resultado.Error -> {
                    _estado.update {
                        it.copy(cargando = false, error = resultado)
                    }
                }
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            googleAuthProvider.signOut()
            when (val resultado = cerrarSesion.invoke()) {
                is Resultado.Exito -> {
                    _estado.update { it.copy(cargando = false, error = null) }
                }

                is Resultado.Error -> {
                    _estado.update { it.copy(cargando = false, error = resultado) }
                }
            }
        }
    }

    private fun cargarPerfil() {
        viewModelScope.launch {
            when (val resultado = obtenerPerfil()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(perfil = resultado.dato, error = null)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(error = resultado)
                }
            }
        }
    }
}
