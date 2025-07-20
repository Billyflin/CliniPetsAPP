// ui/viewmodels/AuthViewModel.kt
package cl.clinipets.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.preferences.UserPreferences
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        checkAuthStatus()
        observeAuthChanges()
    }

    private fun observeAuthChanges() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.value = _authState.value.copy(
                isAuthenticated = user != null,
                userId = user?.uid,
                userEmail = user?.email,
                userPhone = user?.phoneNumber
            )

            // Actualizar preferencias cuando cambia el estado de auth
            viewModelScope.launch {
                if (user != null) {
                    userPreferences.updateUserData(
                        userId = user.uid,
                        email = user.email,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl?.toString()
                    )
                } else {
                    userPreferences.clearUserData()
                }
            }
        }
    }

    private fun checkAuthStatus() {
        val currentUser = auth.currentUser
        _authState.value = _authState.value.copy(
            isAuthenticated = currentUser != null,
            userId = currentUser?.uid,
            userEmail = currentUser?.email,
            userPhone = currentUser?.phoneNumber
        )
    }

    // Google Sign In
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = _authState.value.copy(isLoading = true, error = null)

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()

                result.user?.let { user ->
                    createOrUpdateUserInFirestore(user, "google")
                }

                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isLoginSuccessful = true
                )
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Error con Google: ${e.message}"
                )
            }
        }
    }

    // Facebook Sign In
    fun signInWithFacebook(token: String) {
        viewModelScope.launch {
            try {
                _authState.value = _authState.value.copy(isLoading = true, error = null)

                val credential = FacebookAuthProvider.getCredential(token)
                val result = auth.signInWithCredential(credential).await()

                result.user?.let { user ->
                    createOrUpdateUserInFirestore(user, "facebook")
                }

                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isLoginSuccessful = true
                )
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Error con Facebook: ${e.message}"
                )
            }
        }
    }

    // Phone Authentication - Step 1: Send code
    fun sendPhoneVerification(phoneNumber: String, activity: Activity) {
        if (!phoneNumber.startsWith("+")) {
            _authState.value = _authState.value.copy(
                error = "El número debe incluir código de país (ej: +56912345678)"
            )
            return
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Error al verificar: ${e.message}"
                    )
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@AuthViewModel.verificationId = verificationId
                    this@AuthViewModel.resendToken = token
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        phoneVerificationStep = PhoneVerificationStep.CODE_SENT
                    )
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
    }

    // Phone Authentication - Step 2: Verify code
    fun verifyPhoneCode(code: String) {
        val verId = verificationId
        if (verId == null) {
            _authState.value = _authState.value.copy(
                error = "Error: No hay verificación pendiente"
            )
            return
        }

        viewModelScope.launch {
            val credential = PhoneAuthProvider.getCredential(verId, code)
            signInWithPhoneCredential(credential)
        }
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                _authState.value = _authState.value.copy(isLoading = true, error = null)

                val result = auth.signInWithCredential(credential).await()

                result.user?.let { user ->
                    createOrUpdateUserInFirestore(user, "phone")
                }

                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isLoginSuccessful = true,
                    phoneVerificationStep = PhoneVerificationStep.NONE
                )
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Código inválido: ${e.message}"
                )
            }
        }
    }

    private suspend fun createOrUpdateUserInFirestore(user: FirebaseUser, provider: String) {
        try {
            val userDoc = firestore.collection("users").document(user.uid).get().await()

            if (!userDoc.exists()) {
                val userData = hashMapOf(
                    "name" to (user.displayName ?: "Usuario"),
                    "email" to user.email,
                    "phone" to user.phoneNumber,
                    "photoUrl" to user.photoUrl?.toString(),
                    "provider" to provider,
                    "isVet" to false,
                    "createdAt" to System.currentTimeMillis(),
                    "lastLogin" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid).set(userData).await()
            } else {
                // Actualizar último login
                firestore.collection("users").document(user.uid)
                    .update(
                        mapOf(
                            "lastLogin" to System.currentTimeMillis(),
                            "lastProvider" to provider
                        )
                    ).await()
            }
        } catch (e: Exception) {
            // Log error pero no fallar el inicio de sesión
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            // También cerrar sesión de Facebook si es necesario
            com.facebook.login.LoginManager.getInstance().logOut()
            _authState.value = AuthState() // Reset state
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun setError(message: String) {
        _authState.value = _authState.value.copy(error = message)
    }

    fun resetLoginState() {
        _authState.value = _authState.value.copy(isLoginSuccessful = false)
    }

    fun resetPhoneVerification() {
        _authState.value = _authState.value.copy(
            phoneVerificationStep = PhoneVerificationStep.NONE,
            error = null
        )
        verificationId = null
        resendToken = null
    }
}

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val userId: String? = null,
    val userEmail: String? = null,
    val userPhone: String? = null,
    val phoneVerificationStep: PhoneVerificationStep = PhoneVerificationStep.NONE,
    val error: String? = null
)

enum class PhoneVerificationStep {
    NONE,
    CODE_SENT
}