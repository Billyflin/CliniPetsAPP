package cl.clinipets.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DefaultApi
import cl.clinipets.openapi.models.GoogleLoginRequest
import cl.clinipets.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val defaultApi: DefaultApi,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Result<Unit>>(Result.Success(Unit))
    val loginState: StateFlow<Result<Unit>> = _loginState

    fun setLoginError(exception: Throwable, message: String? = null) {
        _loginState.value = Result.Error(exception, message)
    }

    fun loginWithGoogle(idToken: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _loginState.value = Result.Loading
            try {
                val response = defaultApi.apiAuthGooglePost(GoogleLoginRequest(idToken = idToken))
                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    tokenResponse?.let { tokens ->
                        tokenRepository.setTokens(tokens.token, null)
                        _loginState.value = Result.Success(Unit)
                        onLoginSuccess()
                    } ?: run { _loginState.value = Result.Error(Exception("Token response is null")) }
                } else {
                    _loginState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _loginState.value = Result.Error(e)
            }
        }
    }

    fun checkLoginStatus(onLoggedIn: () -> Unit, onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            val accessToken = tokenRepository.access.value
            if (accessToken != null) {
                onLoggedIn()
            } else {
                onLoggedOut()
            }
        }
    }
}
