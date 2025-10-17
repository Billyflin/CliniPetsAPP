package cl.clinipets.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _profile = MutableStateFlow<cl.clinipets.network.MeResponse?>(null)
    val profile: StateFlow<cl.clinipets.network.MeResponse?> = _profile

    private var bootstrapped = false

    fun bootstrap() {
        if (bootstrapped) return
        bootstrapped = true
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = repo.bootstrapSession()
            if (res.isSuccess) {
                val me = res.getOrNull()
                _profile.value = if (me?.authenticated == true) me else null
            } else {
                _profile.value = null
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = repo.loginWithGoogle(idToken)
            if (res.isSuccess) {
                val p = repo.fetchProfile()
                if (p.isSuccess) {
                    val me = p.getOrNull()
                    _profile.value = if (me?.authenticated == true) me else null
                }
                onSuccess()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage ?: "Login failed"
            }
            _isLoading.value = false
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val p = repo.fetchProfile()
            if (p.isSuccess) {
                val me = p.getOrNull()
                _profile.value = if (me?.authenticated == true) me else null
            } else {
                _error.value = p.exceptionOrNull()?.localizedMessage
                _profile.value = null
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try { repo.logout() } finally { _profile.value = null }
        }
    }
}
