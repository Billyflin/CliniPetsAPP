
// ui/screens/auth/RegisterViewModel.kt
package cl.clinipets.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.preferences.UserPreferences
import cl.clinipets.domain.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun createAccount(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.createUserWithEmail(email, password)
                .onSuccess { authResult ->
                    // Update display name
                    authResult.user?.let { user ->
                        val profileUpdates = userProfileChangeRequest {
                            displayName = name
                        }
                        user.updateProfile(profileUpdates).await()

                        // Update preferences
                        userPreferences.updateUserData(
                            userId = user.uid,
                            email = user.email,
                            displayName = name,
                            photoUrl = null
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegisterSuccessful = true
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = getErrorMessage(exception)
                        )
                    }
                }
        }
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when (exception.message) {
            "The email address is already in use by another account." -> "Este email ya está registrado"
            "The email address is badly formatted." -> "Email inválido"
            "The given password is invalid. [ Password should be at least 6 characters ]" -> "La contraseña debe tener al menos 6 caracteres"
            else -> exception.message ?: "Error al crear la cuenta"
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isRegisterSuccessful: Boolean = false,
    val error: String? = null
)