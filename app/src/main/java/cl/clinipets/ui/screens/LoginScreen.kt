package cl.clinipets.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import cl.clinipets.BuildConfig
import cl.clinipets.auth.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CustomCredential
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        val message = error
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    val cm = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    fun startSignIn() {
        scope.launch {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CliniPets",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Inicia sesión para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                FilledTonalButton(
                    onClick = { startSignIn() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                        Text("Conectando...")
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continuar con Google")
                    }
                }

                // Espacio para crecimiento futuro (card de privacidad, etc.)
            }
        }
    }
}
