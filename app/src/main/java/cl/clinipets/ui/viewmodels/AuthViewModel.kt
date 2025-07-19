// ui/viewmodels/AuthViewModel.kt
package cl.clinipets.ui.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class AuthViewModel @Inject constructor() : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        checkAuthStatus()
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
                    createOrUpdateUserInFirestore(user)
                }

                checkAuthStatus()
                _authState.value =
                    _authState.value.copy(isLoading = false, isLoginSuccessful = true)
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
                    createOrUpdateUserInFirestore(user)
                }

                checkAuthStatus()
                _authState.value =
                    _authState.value.copy(isLoading = false, isLoginSuccessful = true)
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
            _authState.value =
                _authState.value.copy(error = "El número debe incluir código de país (ej: +56)")
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
            _authState.value = _authState.value.copy(error = "Error: No hay verificación pendiente")
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
                    createOrUpdateUserInFirestore(user)
                }

                checkAuthStatus()
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

    private suspend fun createOrUpdateUserInFirestore(user: FirebaseUser) {
        try {
            val userDoc = firestore.collection("users").document(user.uid).get().await()

            if (!userDoc.exists()) {
                val userData = hashMapOf(
                    "name" to (user.displayName ?: "Usuario"),
                    "email" to user.email,
                    "phone" to user.phoneNumber,
                    "photoUrl" to user.photoUrl?.toString(),
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(user.uid).set(userData).await()
            }
        } catch (e: Exception) {
            // Log error but don't fail the sign in
        }
    }

    fun signOut() {
        auth.signOut()
        checkAuthStatus()
        _authState.value = _authState.value.copy(phoneVerificationStep = PhoneVerificationStep.NONE)
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
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