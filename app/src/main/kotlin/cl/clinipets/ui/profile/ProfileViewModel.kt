package cl.clinipets.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.session.SessionManager
import cl.clinipets.openapi.apis.AuthControllerApi
import cl.clinipets.openapi.models.ProfileResponse
import cl.clinipets.openapi.models.UserUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: ProfileResponse,
        val isUpdating: Boolean = false,
        val updateError: String? = null
    ) : ProfileUiState()
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

    fun updateProfile(name: String, phone: String, address: String) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isUpdating = true, updateError = null)
            try {
                val request = UserUpdateRequest(
                    name = name,
                    phone = phone,
                    address = address
                )
                val response = authApi.updateProfile(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _uiState.value = ProfileUiState.Success(
                            profile = body,
                            isUpdating = false,
                            updateError = null
                        )
                    } else {
                        _uiState.value = currentState.copy(
                            isUpdating = false,
                            updateError = "Respuesta vacía al actualizar perfil"
                        )
                    }
                } else {
                    val errorMessage = runCatching { response.errorBody()?.string() }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: "No pudimos actualizar tus datos: ${response.code()}"
                    _uiState.value = currentState.copy(
                        isUpdating = false,
                        updateError = errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isUpdating = false,
                    updateError = e.message ?: "Error desconocido al actualizar perfil"
                )
            }
        }
    }
}
