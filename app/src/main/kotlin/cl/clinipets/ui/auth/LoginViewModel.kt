package cl.clinipets.ui.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.session.SessionManager
import cl.clinipets.openapi.apis.AuthControllerApi
import cl.clinipets.openapi.apis.DeviceTokenControllerApi
import cl.clinipets.openapi.models.DeviceTokenRequest
import cl.clinipets.openapi.models.ProfileResponse
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthControllerApi,
    private val session: SessionManager,
    private val deviceTokenApi: DeviceTokenControllerApi
) : ViewModel() {

    data class UiState(
        val isCheckingSession: Boolean = true,
        val isAuthenticating: Boolean = false,
        val me: ProfileResponse? = null,
        val error: String? = null,
        val ok: Boolean = false,
        val needsPhoneVerification: Boolean = false,
        val otpSent: Boolean = false,
        val phoneNumber: String = "",
        val verificationId: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        viewModelScope.launch {
            // Check if user is already signed in with Firebase
            val currentUser = auth.currentUser
            if (currentUser != null) {
                fetchProfile()
            } else {
                _ui.update { it.copy(isCheckingSession = false) }
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String, phone: String? = null) {
        viewModelScope.launch {
            Log.d("ClinipetsAuth", ">>> INICIO: loginWithGoogleIdToken")
            _ui.update { it.copy(isAuthenticating = true, error = null) }
            try {
                // 1. Sign in to Firebase with Google Credential
                Log.d("ClinipetsAuth", "Paso 1: Iniciando sesión en Firebase con Google IdToken...")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = suspendCancellableCoroutine { cont ->
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener { 
                            Log.d("ClinipetsAuth", "Firebase: signInWithCredential ÉXITO para UID: ${it.user?.uid}")
                            cont.resume(it) 
                        }
                        .addOnFailureListener { 
                            Log.e("ClinipetsAuth", "Firebase: signInWithCredential FALLÓ", it)
                            cont.resumeWith(Result.failure(it)) 
                        }
                }

                if (authResult.user != null) {
                    Log.d("ClinipetsAuth", "Paso 2: Firebase OK. Llamando a fetchProfile para sincronizar con Backend...")
                    fetchProfile()
                } else {
                    Log.e("ClinipetsAuth", "Firebase devolvió un usuario nulo tras el login")
                    throw Exception("El usuario de Firebase es nulo")
                }
            } catch (e: Exception) {
                Log.e("ClinipetsAuth", "Error durante el flujo de login Google -> Firebase", e)
                auth.signOut()
                _ui.update {
                    it.copy(
                        isAuthenticating = false,
                        error = e.message ?: "Error durante el inicio de sesión",
                    )
                }
            }
        }
    }

    fun startPhoneLogin(activity: Activity, phone: String) {
        val cleanPhone = phone.replace(" ", "").trim()
        _ui.update { it.copy(isAuthenticating = true, error = null, phoneNumber = cleanPhone) }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval or instant verification
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _ui.update { 
                    it.copy(isAuthenticating = false, error = e.message ?: "Error en verificación de teléfono") 
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _ui.update {
                    it.copy(
                        isAuthenticating = false,
                        otpSent = true,
                        verificationId = verificationId
                    )
                }
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(cleanPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneCode(code: String) {
        val verificationId = _ui.value.verificationId
        if (verificationId == null) {
            _ui.update { it.copy(error = "Error: ID de verificación perdido. Intenta nuevamente.") }
            return
        }
        
        _ui.update { it.copy(isAuthenticating = true, error = null) }
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                fetchProfile()
            }
            .addOnFailureListener { e ->
                _ui.update { 
                    it.copy(isAuthenticating = false, error = e.message ?: "Código inválido o error de autenticación") 
                }
            }
    }

    fun resetLoginState() {
        _ui.update { it.copy(otpSent = false, phoneNumber = "", error = null, isAuthenticating = false, verificationId = null) }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            Log.d("ClinipetsAuth", ">>> INICIO: fetchProfile")
            _ui.update { it.copy(isCheckingSession = true) }
            try {
                Log.d("ClinipetsAuth", "Llamando a authApi.firebaseAuth()...")
                val response = authApi.firebaseAuth()
                Log.d("ClinipetsAuth", "Backend: Respuesta recibida. Código: ${response.code()}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("ClinipetsAuth", "Backend: ÉXITO. Usuario: ${body.email}, Rol: ${body.role}")
                        _ui.update {
                            it.copy(
                                ok = true,
                                me = body,
                                isCheckingSession = false,
                                error = null,
                                needsPhoneVerification = !body.phoneVerified
                            )
                        }
                        sendFcmTokenSafe()
                    } else {
                        Log.e("ClinipetsAuth", "Backend: Error - El cuerpo de la respuesta es nulo")
                        handleSessionError("Sesión inválida (body nulo)")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ClinipetsAuth", "Backend: Error HTTP ${response.code()}. Detalle: $errorBody")
                    handleSessionError("Error de sesión: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ClinipetsAuth", "Backend: Excepción de conexión", e)
                handleSessionError("Error de conexión: ${e.message}")
            }
        }
    }

    private suspend fun handleSessionError(message: String) {
        auth.signOut()
        session.clear()
        _ui.update {
            it.copy(
                ok = false,
                me = null,
                isCheckingSession = false,
                error = null,
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            session.clear()
            _ui.update { UiState(isCheckingSession = false) }
        }
    }

    fun refreshProfile() {
        fetchProfile()
    }

    fun setError(message: String?) {
        _ui.update { it.copy(error = message) }
    }

    fun clearError() = setError(null)

    private fun sendFcmTokenSafe() {
        viewModelScope.launch {
            try {
                val token = suspendCancellableCoroutine<String?> { cont ->
                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }

                if (token.isNullOrBlank()) {
                    Log.w("ClinipetsFCM", "Token FCM vacío o nulo; se omite envío")
                    return@launch
                }

                runCatching { deviceTokenApi.saveDeviceToken(DeviceTokenRequest(token)) }
                    .onSuccess { response ->
                        if (response.isSuccessful) {
                            Log.d("ClinipetsFCM", "Token FCM enviado correctamente")
                        } else {
                            Log.w(
                                "ClinipetsFCM",
                                "Fallo API al enviar token FCM: ${response.code()} ${response.message()}",
                            )
                        }
                    }.onFailure { error ->
                        Log.e("ClinipetsFCM", "Excepción enviando token FCM al backend", error)
                    }
            } catch (e: Exception) {
                Log.e("ClinipetsFCM", "Excepción al obtener token FCM", e)
            }
        }
    }
}
