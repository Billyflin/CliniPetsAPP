// ui/screens/LoginScreen.kt
package cl.clinipets.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.R
import cl.clinipets.ui.viewmodels.AuthViewModel
import cl.clinipets.ui.viewmodels.PhoneVerificationStep
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val authState by viewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()

    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    // Facebook callback manager
    val callbackManager = remember { CallbackManager.Factory.create() }

    LaunchedEffect(authState.isLoginSuccessful) {
        if (authState.isLoginSuccessful) {
            viewModel.resetLoginState()
            onLoginSuccess()
        }
    }

    // Google Sign In
    fun signInWithGoogle() {
        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                when (val credential = result.credential) {
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                            viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                        }
                    }
                }
            } catch (e: GetCredentialException) {
                // Handle error - puedes mostrar un mensaje al usuario
                viewModel.setError("Error al iniciar sesión con Google: ${e.message}")
            }
        }
    }

    // Facebook Sign In
    fun signInWithFacebook() {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    viewModel.signInWithFacebook(result.accessToken.token)
                }

                override fun onCancel() {
                    viewModel.setError("Inicio de sesión con Facebook cancelado")
                }

                override fun onError(error: FacebookException) {
                    viewModel.setError("Error al iniciar sesión con Facebook: ${error.message}")
                }
            }
        )

        LoginManager.getInstance().logInWithReadPermissions(
            activity,
            listOf("email", "public_profile")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo o título
        Text(
            text = "Clinipets",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tu clínica veterinaria de confianza",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (authState.isLoading) {
            CircularProgressIndicator()
        } else {
            when (authState.phoneVerificationStep) {
                PhoneVerificationStep.NONE -> {
                    // Botones de inicio de sesión social
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Google Sign In
                        OutlinedButton(
                            onClick = ::signInWithGoogle,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !authState.isLoading
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Aquí podrías agregar el ícono de Google
                                Text("Continuar con Google")
                            }
                        }

                        // Facebook Sign In
                        Button(
                            onClick = ::signInWithFacebook,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !authState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1877F2) // Color de Facebook
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Facebook,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Continuar con Facebook")
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )

                        // Phone Sign In
                        Text(
                            text = "O ingresa con tu teléfono",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Teléfono (ej: +56912345678)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.sendPhoneVerification(phoneNumber, activity) },
                            enabled = phoneNumber.isNotBlank() && !authState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enviar código")
                        }
                    }
                }

                PhoneVerificationStep.CODE_SENT -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Ingresa el código enviado a",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it },
                            label = { Text("Código de 6 dígitos") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = { viewModel.verifyPhoneCode(verificationCode) },
                            enabled = verificationCode.length == 6 && !authState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verificar")
                        }

                        TextButton(
                            onClick = {
                                viewModel.clearError()
                                phoneNumber = ""
                                verificationCode = ""
                                viewModel.resetPhoneVerification()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cambiar número")
                        }
                    }
                }
            }
        }

        // Error message
        authState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}