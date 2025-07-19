// ui/screens/auth/LoginViewModel.kt
package cl.clinipets.ui.screens.auth

import android.app.Activity
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.preferences.UserPreferences
import cl.clinipets.domain.auth.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Phone verification
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Validación de email
    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Validación de contraseña
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun signInWithEmail(email: String, password: String) {
        // Validaciones previas
        when {
            !isValidEmail(email) -> {
                _uiState.update { it.copy(error = "Por favor ingresa un email válido") }
                return
            }

            !isValidPassword(password) -> {
                _uiState.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.signInWithEmail(email, password)
                .onSuccess { authResult ->
                    updateUserPreferences(authResult.user)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignInSuccessful = true
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

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.signInWithGoogle(idToken)
                .onSuccess { authResult ->
                    updateUserPreferences(authResult.user)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignInSuccessful = true
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

    // Nueva función para restablecer contraseña
    fun sendPasswordResetEmail(email: String) {
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(error = "Por favor ingresa un email válido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordResetSent = true,
                            passwordResetMessage = "Se ha enviado un email para restablecer tu contraseña a $email"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = when (exception) {
                                is FirebaseAuthInvalidUserException -> "No existe una cuenta con este email"
                                else -> "Error al enviar el email: ${exception.message}"
                            }
                        )
                    }
                }
        }
    }

    fun sendPhoneVerification(phoneNumber: String, activity: Activity) {
        // Validación básica del número de teléfono
        if (phoneNumber.length < 9) {
            _uiState.update { it.copy(error = "Número de teléfono inválido") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = getErrorMessage(e)
                    )
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                this@LoginViewModel.verificationId = verificationId
                this@LoginViewModel.resendToken = token
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phoneVerificationStep = PhoneVerificationStep.CODE_SENT
                    )
                }
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneCode(code: String) {
        viewModelScope.launch {
            val verId = verificationId
            if (verId != null) {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val credential = PhoneAuthProvider.getCredential(verId, code)
                signInWithPhoneCredential(credential)
            } else {
                _uiState.update {
                    it.copy(error = "ID de verificación no encontrado. Por favor, intenta de nuevo.")
                }
            }
        }
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            authRepository.signInWithPhoneCredential(credential)
                .onSuccess { authResult ->
                    updateUserPreferences(authResult.user)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignInSuccessful = true,
                            phoneVerificationStep = PhoneVerificationStep.NONE
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

    private suspend fun updateUserPreferences(user: FirebaseUser?) {
        user?.let {
            userPreferences.updateUserData(
                userId = it.uid,
                email = it.email,
                displayName = it.displayName,
                photoUrl = it.photoUrl?.toString()
            )
        }
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Credenciales inválidas"
            is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil"
            is FirebaseAuthInvalidUserException -> "Usuario no encontrado"
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este email"
            else -> exception.message ?: "Error desconocido"
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearPasswordResetMessage() {
        _uiState.update { it.copy(passwordResetSent = false, passwordResetMessage = null) }
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSignInSuccessful: Boolean = false,
    val error: String? = null,
    val phoneVerificationStep: PhoneVerificationStep = PhoneVerificationStep.NONE,
    val passwordResetSent: Boolean = false,
    val passwordResetMessage: String? = null
)

enum class PhoneVerificationStep {
    NONE,
    CODE_SENT,
    VERIFIED
}