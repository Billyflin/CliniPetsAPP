package cl.clinipets.auth.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val credManager: CredentialManager,
    private val request: GetCredentialRequest
) : ViewModel() {

    var state by mutableStateOf(LoginUiState())
        private set

    fun signIn(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            runCatching {
                val result = credManager.getCredential(
                    context = context,
                    request = request
                )
                val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
                val token = googleCred.idToken
                val credential = GoogleAuthProvider.getCredential(token, null)
                auth.signInWithCredential(credential).await()
            }.onSuccess {
                state = state.copy(loading = false)
                onSuccess()
            }.onFailure { e ->
                state = state.copy(loading = false, error = e.message)
            }
        }
    }
}
