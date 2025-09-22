package cl.clinipets.auth.presentation

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object LoggedOut : AuthUiState
    data object LoggedIn : AuthUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val credManager: CredentialManager,
    private val request: GetCredentialRequest
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState

    // eventos de error consumibles (snackbar/toast)
    private val _errors = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errors = _errors.asSharedFlow()

    init {
        // reflejar sesión actual y cambios
        _uiState.value =
            if (auth.currentUser == null) AuthUiState.LoggedOut else AuthUiState.LoggedIn
        auth.addAuthStateListener { fa ->
            _uiState.value =
                if (fa.currentUser == null) AuthUiState.LoggedOut else AuthUiState.LoggedIn
        }
    }

    fun signIn(context: Context) {
        // Nota: pasar el context de la pantalla está OK para CredentialManager
        if (_uiState.value == AuthUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = credManager.getCredential(context, request)
                val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
                val token = googleCred.idToken
                val credential = GoogleAuthProvider.getCredential(token, null)
                auth.signInWithCredential(credential).await()
                // authStateListener actualizará a LoggedIn
            } catch (e: GetCredentialCancellationException) {
                _uiState.value =
                    if (auth.currentUser == null) AuthUiState.LoggedOut else AuthUiState.LoggedIn
                // usuario canceló: no mostramos error
            } catch (e: NoCredentialException) {
                _uiState.value = AuthUiState.LoggedOut
                _errors.tryEmit("No hay credenciales disponibles en el dispositivo")
            } catch (e: GetCredentialProviderConfigurationException) {
                _uiState.value = AuthUiState.LoggedOut
                _errors.tryEmit("Proveedor no configurado (actualiza Google Play Services)")
            } catch (e: GetCredentialException) {
                _uiState.value = AuthUiState.LoggedOut
                _errors.tryEmit("Error obteniendo credencial: ${e.errorMessage ?: e.message}")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.LoggedOut
                _errors.tryEmit(e.message ?: "Error de autenticación")
            }
        }
    }

    fun logout() {
        auth.signOut()
        // authStateListener moverá a LoggedOut
    }
}
