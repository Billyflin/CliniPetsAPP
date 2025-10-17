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

    private val _profile = MutableStateFlow<cl.clinipets.network.UserProfile?>(null)
    val profile: StateFlow<cl.clinipets.network.UserProfile?> = _profile

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = repo.loginWithGoogle(idToken)
            if (res.isSuccess) {
                // fetch profile
                val p = repo.fetchProfile()
                if (p.isSuccess) {
                    _profile.value = p.getOrNull()
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
                _profile.value = p.getOrNull()
            } else {
                _error.value = p.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        repo.signOut()
        _profile.value = null
    }
}
