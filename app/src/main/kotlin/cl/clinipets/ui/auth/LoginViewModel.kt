package cl.clinipets.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.session.SessionManager
import cl.clinipets.openapi.apis.AuthControllerApi
import cl.clinipets.openapi.apis.DeviceTokenControllerApi
import cl.clinipets.openapi.models.DeviceTokenRequest
import cl.clinipets.openapi.models.GoogleLoginRequest
import cl.clinipets.openapi.models.OtpRequest
import cl.clinipets.openapi.models.OtpVerifyRequest
import cl.clinipets.openapi.models.ProfileResponse
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

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
        val phoneNumber: String = ""
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            // Restore session from DataStore on app launch
            val snapshot = session.sessionFlow.first()
            val token = snapshot.token

            if (!token.isNullOrBlank()) {
                // Token exists, restore it in API client
                session.restoreIfAny()
                // Fetch profile to validate session and get user info
                fetchProfile()
            } else {
                // No token, we are ready for login
                _ui.update { it.copy(isCheckingSession = false) }
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String, phone: String? = null) {
        viewModelScope.launch {
            _ui.update { it.copy(isAuthenticating = true, error = null) }
            try {
                // 1. Exchange Google ID Token for App Token
                val loginResponse = authApi.loginGoogle(GoogleLoginRequest(idToken, phone))

                if (loginResponse.isSuccessful) {
                    val token = loginResponse.body()?.accessToken
                    if (!token.isNullOrBlank()) {
                        // 2. Set token in SessionManager (updates API client & DataStore)
                        session.setAndPersist(token)

                        // 3. Fetch Profile immediately to complete login
                        val profileResponse = authApi.getProfile()

                        if (profileResponse.isSuccessful) {
                            val me = profileResponse.body()
                            if (me != null) {
                                _ui.update {
                                    it.copy(
                                        ok = true,
                                        me = me,
                                        isAuthenticating = false,
                                        isCheckingSession = false,
                                        error = null,
                                        needsPhoneVerification = !me.phoneVerified
                                    )
                                }
                                sendFcmTokenSafe()
                            } else {
                                throw Exception("Perfil de usuario vacío")
                            }
                        } else {
                            throw Exception("Error al obtener perfil: ${profileResponse.code()}")
                        }
                    } else {
                        throw Exception("Token de sesión inválido")
                    }
                } else {
                    val errorBody = loginResponse.errorBody()?.string()
                    throw Exception("Falló autenticación: ${loginResponse.code()} $errorBody")
                }
            } catch (e: Exception) {
                session.clear() // Clean up if anything failed
                _ui.update {
                    it.copy(
                        isAuthenticating = false,
                        error = e.message ?: "Error durante el inicio de sesión",
                    )
                }
            }
        }
    }

    fun requestOtp(phone: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isAuthenticating = true, error = null) }
            val cleanPhone = phone.replace(" ", "").trim()
            try {
                val response = authApi.requestOtp(OtpRequest(cleanPhone))
                if (response.isSuccessful) {
                    _ui.update { it.copy(isAuthenticating = false, otpSent = true, phoneNumber = cleanPhone) }
                } else {
                     _ui.update { it.copy(isAuthenticating = false, error = "Error al solicitar código: ${response.code()}") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isAuthenticating = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun loginWithOtp(code: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isAuthenticating = true, error = null) }
            try {
                val currentPhone = _ui.value.phoneNumber
                val response = authApi.verifyOtp(OtpVerifyRequest(phone = currentPhone, code = code))
                
                if (response.isSuccessful) {
                    val token = response.body()?.accessToken
                    if (!token.isNullOrBlank()) {
                         session.setAndPersist(token)
                         fetchProfile() // This will update state to ok=true if successful
                    } else {
                        _ui.update { it.copy(isAuthenticating = false, error = "Token inválido") }
                    }
                } else {
                    _ui.update { it.copy(isAuthenticating = false, error = "Código incorrecto o expirado") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isAuthenticating = false, error = e.message ?: "Error al verificar código") }
            }
        }
    }

    fun resetLoginState() {
        _ui.update { it.copy(otpSent = false, phoneNumber = "", error = null, isAuthenticating = false) }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _ui.update { it.copy(isCheckingSession = true) }
            try {
                val response = authApi.getProfile()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
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
                        // Token might be stale or invalid if profile is null
                        handleSessionError("Sesión inválida")
                    }
                } else {
                    handleSessionError("Error de sesión: ${response.code()}")
                }
            } catch (e: Exception) {
                handleSessionError("Error de conexión")
            }
        }
    }

    private suspend fun handleSessionError(message: String) {
        session.clear()
        _ui.update {
            it.copy(
                ok = false,
                me = null,
                isCheckingSession = false,
                // Only show error if we were explicitly authenticating, otherwise just logout silently
                error = null,
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
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
