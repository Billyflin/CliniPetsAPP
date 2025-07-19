// ui/screens/LoginScreen.kt
package cl.clinipets.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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

    Column {

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
                    // Handle error
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

                    override fun onCancel() {}
                    override fun onError(error: FacebookException) {}
                }
            )

            LoginManager.getInstance().logInWithReadPermissions(
                activity,
                listOf("email", "public_profile")
            )
        }

        Column {
        Text("Clinipets")

            if (authState.isLoading) {
                CircularProgressIndicator()
            } else {
                when (authState.phoneVerificationStep) {
                    PhoneVerificationStep.NONE -> {
                        Button(onClick = ::signInWithGoogle) {
                            Text("Continuar con Google")
                        }
                        Button(onClick = ::signInWithFacebook) {
                            Text("Continuar con Facebook")
                        }



                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Teléfono (ej: +56912345678)") }
                        )
                        Button(
                            onClick = { viewModel.sendPhoneVerification(phoneNumber, activity) },
                            enabled = phoneNumber.isNotBlank()
                        ) {
                            Text("Enviar código")
                        }
                    }

                    PhoneVerificationStep.CODE_SENT -> {
                        Text("Ingresa el código enviado a $phoneNumber")
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it },
                            label = { Text("Código de 6 dígitos") }
                        )
                        Button(
                            onClick = { viewModel.verifyPhoneCode(verificationCode) },
                            enabled = verificationCode.length == 6
                        ) {
                            Text("Verificar")
                        }
                        TextButton(
                            onClick = {
                                viewModel.clearError()
                                phoneNumber = ""
                                verificationCode = ""
                                // Reset to phone input
                            }
                        ) {
                            Text("Cambiar número")
                        }
                }
            }
        }

            authState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}