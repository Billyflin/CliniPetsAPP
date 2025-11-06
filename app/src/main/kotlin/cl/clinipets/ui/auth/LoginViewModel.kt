package cl.clinipets.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.session.SessionManager
import cl.clinipets.openapi.apis.AutenticacinApi
import cl.clinipets.openapi.models.GoogleLoginRequest
import cl.clinipets.openapi.models.MeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AutenticacinApi,
    private val session: SessionManager
) : ViewModel() {

    data class UiState(
        val isCheckingSession: Boolean = true,
        val isAuthenticating: Boolean = false,
        val me: MeResponse? = null,
        val roles: List<String> = emptyList(),
        val displayName: String? = null,
        val error: String? = null,
        val ok: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private var hasRequestedProfile = false

    init {
        viewModelScope.launch {
            session.sessionFlow.collect { snapshot ->
                val hasToken = !snapshot.token.isNullOrBlank()
                if (!hasToken) {
                    hasRequestedProfile = false
                }

                _ui.update { current ->
                    current.copy(
                        isCheckingSession = false,
                        ok = hasToken && (current.me?.authenticated != false),
                        roles = snapshot.roles,
                        displayName = snapshot.displayName,
                        error = if (hasToken) current.error else null
                    )
                }

                if (hasToken && !hasRequestedProfile) {
                    hasRequestedProfile = true
                    fetchProfile()
                }
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isAuthenticating = true, error = null) }
            try {
                val r = authApi.authLoginGoogle(GoogleLoginRequest(idToken))
                if (r.isSuccessful) {
                    val token = r.body()?.token.orEmpty()
                    if (token.isNotBlank()) {
                        hasRequestedProfile = false
                        session.setAndPersist(token)
                    } else {
                        _ui.update { it.copy(error = "Token vacío") }
                    }
                } else {
                    _ui.update {
                        it.copy(
                            error = "HTTP ${r.code()}: ${r.errorBody()?.string()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Error inesperado") }
            } finally {
                _ui.update { it.copy(isAuthenticating = false) }
            }
        }
    }

    fun fetchProfile() {
        hasRequestedProfile = true
        viewModelScope.launch {
            val result = runCatching { authApi.authMe() }
            result.onSuccess { response ->
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        session.persistProfile(body)
                        _ui.update {
                            it.copy(
                                ok = body.authenticated,
                                me = body,
                                roles = body.roles.orEmpty(),
                                displayName = body.nombre,
                                error = null
                            )
                        }
                    } else {
                        _ui.update { it.copy(ok = false, error = "Perfil vacío") }
                    }
                } else {
                    _ui.update {
                        it.copy(
                            ok = false,
                            error = "HTTP ${response.code()}: ${response.errorBody()?.string()}"
                        )
                    }
                }
            }.onFailure { throwable ->
                _ui.update { it.copy(ok = false, error = throwable.message ?: "Error inesperado") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            session.clear()
            hasRequestedProfile = false
            _ui.update { UiState(isCheckingSession = false) }
        }
    }

    fun refreshProfile() {
        hasRequestedProfile = false
        fetchProfile()
    }
}
