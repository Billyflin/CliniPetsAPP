// auth/ui/LoginViewModel.kt
package cl.clinipets.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.session.SessionManager
import cl.clinipets.openapi.apis.AutenticacinApi
import cl.clinipets.openapi.models.GoogleLoginRequest
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
        val error: String? = null,
        val ok: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            session.tokenFlow.collect { token ->
                _ui.update { current ->
                    current.copy(
                        isCheckingSession = false,
                        ok = !token.isNullOrBlank(),
                        error = if (!token.isNullOrBlank()) null else current.error
                    )
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
                        session.setAndPersist(token)
                        _ui.update { it.copy(ok = true, error = null) }
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

    fun logout() {
        viewModelScope.launch {
            session.clear()
            _ui.update { it.copy(error = null) }
        }
    }
}
