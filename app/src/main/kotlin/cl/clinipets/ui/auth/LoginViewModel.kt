package cl.clinipets.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.session.SessionManager
import cl.clinipets.openapi.apis.AuthControllerApi
import cl.clinipets.openapi.models.GoogleLoginRequest
import cl.clinipets.openapi.models.ProfileResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthControllerApi,
    private val session: SessionManager
) : ViewModel() {

    data class UiState(
        val isCheckingSession: Boolean = true,
        val isAuthenticating: Boolean = false,
        val me: ProfileResponse? = null,
        val error: String? = null,
        val ok: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            // Restore session from DataStore on app launch
            val snapshot = session.sessionFlow.first()
            val token = snapshot.token

            if (!token.isNullOrBlank()) {
                // Token exists, restore it in API client
                session.restoreIfAny()
                // Fetch profile to validate session and get user info
                fetchProfile()
            } else {
                // No token, we are ready for login
                _ui.update { it.copy(isCheckingSession = false) }
            }
        }
    }

        fun loginWithGoogleIdToken(idToken: String) {
            viewModelScope.launch {
                _ui.update { it.copy(isAuthenticating = true, error = null) }
                try {
                    // 1. Exchange Google ID Token for App Token
                    val loginResponse = authApi.loginGoogle(GoogleLoginRequest(idToken))
                    
                    if (loginResponse.isSuccessful) {
                        val token = loginResponse.body()?.accessToken
                        if (!token.isNullOrBlank()) {
                            // 2. Set token in SessionManager (updates API client & DataStore)
                            session.setAndPersist(token)
    
                            // 3. Fetch Profile immediately to complete login
                            // We call this directly instead of relying on 'fetchProfile' helper to keep flow atomic/clear
                            val profileResponse = authApi.getProfile()
                            
                            if (profileResponse.isSuccessful) {                                            val me = profileResponse.body()
                                            if (me != null) {
                                                _ui.update {
                                                    it.copy(
                                                        ok = true,                                        me = me,
                                        isAuthenticating = false,
                                        isCheckingSession = false,
                                        error = null
                                    )
                                }
                            } else {
                                throw Exception("Perfil de usuario vacío")
                            }
                        } else {
                            throw Exception("Error al obtener perfil: ${profileResponse.code()}")
                        }
                    } else {
                        throw Exception("Token de sesión inválido")
                    }
                } else {
                    val errorBody = loginResponse.errorBody()?.string()
                    throw Exception("Falló autenticación: ${loginResponse.code()} $errorBody")
                }
            } catch (e: Exception) {
                session.clear() // Clean up if anything failed
                _ui.update { 
                    it.copy(
                        isAuthenticating = false, 
                        error = e.message ?: "Error durante el inicio de sesión"
                    ) 
                }
            }
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _ui.update { it.copy(isCheckingSession = true) }
            try {
                val response = authApi.getProfile()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _ui.update {
                            it.copy(
                                ok = true,
                                me = body,
                                isCheckingSession = false,
                                error = null
                            )
                        }
                    } else {
                        // Token might be stale or invalid if profile is null
                        handleSessionError("Sesión inválida")
                    }
                } else {
                    handleSessionError("Error de sesión: ${response.code()}")
                }
            } catch (e: Exception) {
                handleSessionError("Error de conexión")
            }
        }
    }

    private suspend fun handleSessionError(message: String) {
        session.clear()
        _ui.update { 
            it.copy(
                ok = false, 
                me = null, 
                isCheckingSession = false, 
                // Only show error if we were explicitly authenticating, otherwise just logout silently
                error = null 
            ) 
        }
    }

    fun logout() {
        viewModelScope.launch {
            session.clear()
            _ui.update { UiState(isCheckingSession = false) }
        }
    }

    fun refreshProfile() {
        fetchProfile()
    }

    fun setError(message: String?) {
        _ui.update { it.copy(error = message) }
    }

    fun clearError() = setError(null)
}
