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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AutenticacinApi,
    private val session: SessionManager
) : ViewModel() {

    data class UiState(val loading: Boolean = false, val error: String? = null, val ok: Boolean = false)
    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    fun loginWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _ui.value = UiState(loading = true)
            try {
                val r = authApi.authLoginGoogle(GoogleLoginRequest(idToken))
                if (r.isSuccessful) {
                    val token = r.body()?.token.orEmpty()
                    if (token.isNotBlank()) {
                        session.setAndPersist(token)
                        _ui.value = UiState(ok = true)
                    } else _ui.value = UiState(error = "Token vacío")
                } else _ui.value = UiState(error = "HTTP ${r.code()}: ${r.errorBody()?.string()}")
            } catch (e: Exception) {
                _ui.value = UiState(error = e.message)
            }
        }
    }
}
