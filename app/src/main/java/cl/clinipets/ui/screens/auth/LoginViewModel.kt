package cl.clinipets.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.preferences.UserPreferences
import cl.clinipets.domain.auth.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
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

    fun signInWithEmail(email: String, password: String) {
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

    fun sendPhoneVerification(phoneNumber: String, activity: Activity) {
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
                    it.copy(error = "Verification ID not found. Please retry.")
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
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSignInSuccessful: Boolean = false,
    val error: String? = null,
    val phoneVerificationStep: PhoneVerificationStep = PhoneVerificationStep.NONE
)

enum class PhoneVerificationStep {
    NONE,
    CODE_SENT,
    VERIFIED
}
