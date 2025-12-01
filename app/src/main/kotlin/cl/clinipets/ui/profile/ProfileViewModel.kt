package cl.clinipets.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.session.SessionManager
import cl.clinipets.openapi.apis.AuthControllerApi
import cl.clinipets.openapi.models.ProfileResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: ProfileResponse) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authApi: AuthControllerApi,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val response = authApi.getProfile()
                if (response.isSuccessful) {
                    response.body()?.let {
                        _uiState.value = ProfileUiState.Success(it)
                    } ?: run {
                        _uiState.value = ProfileUiState.Error("Perfil vacío")
                    }
                } else {
                    _uiState.value = ProfileUiState.Error("Error al cargar perfil: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clear()
            // Navigation logic should observe session state or be handled by UI callback
        }
    }
}
