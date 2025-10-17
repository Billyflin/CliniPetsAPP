package cl.clinipets.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import cl.clinipets.BuildConfig
import cl.clinipets.auth.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CustomCredential
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val cm = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    fun startSignIn() {
        scope.launch {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false) // muestra selector
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            try {
                val result = cm.getCredential(context, request)
                val cred = result.credential
                when (cred) {
                    is CustomCredential -> {
                        if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                            val idToken = googleCred.idToken
                            if (!idToken.isNullOrEmpty()) {
                                viewModel.loginWithGoogle(idToken) { onLoginSuccess() }
                            } else {
                                Log.w("LoginCM", "GoogleIdTokenCredential sin token")
                            }
                        } else {
                            Log.w("LoginCM", "CustomCredential tipo no soportado: ${cred.type}")
                        }
                    }
                    else -> {
                        Log.w("LoginCM", "Credential no soportada: ${cred::class.java.name}")
                    }
                }
            } catch (e: GetCredentialException) {
                Log.w("LoginCM", "getCredential falló: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("LoginCM", "Error general en sign-in", e)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { startSignIn() }) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Continuar con Google")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (!error.isNullOrEmpty()) {
            Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}
