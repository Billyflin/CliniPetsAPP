// ui/screens/LoginScreen.kt
package cl.clinipets.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val authState by viewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()

    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    LaunchedEffect(authState.isLoginSuccessful) {
        if (authState.isLoginSuccessful) onLoginSuccess()
    }
    Column {


        // Google Sign In
        val credentialManager = remember { CredentialManager.create(context) }

        fun signInWithGoogle() {
            scope.launch {
                try {
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

                    val credential = result.credential
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                } catch (e: GetCredentialException) {
                    // Handle error
                }
            }
        }

        // Facebook Sign In
        val callbackManager = remember { CallbackManager.Factory.create() }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            callbackManager.onActivityResult(result.resultCode, result.resultCode, result.data)
        }

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

        // UI
        Text("Clinipets")

        when (authState.phoneVerificationStep) {
            PhoneVerificationStep.NONE -> {
                // Botones de login social
                Button(onClick = ::signInWithGoogle) { Text("Continuar con Google") }
                Button(onClick = ::signInWithFacebook) { Text("Continuar con Facebook") }

                // Login con teléfono
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Teléfono (ej: +56912345678)") }
                )
                Button(onClick = { viewModel.sendPhoneVerification(phoneNumber, activity) }) {
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
                Button(onClick = { viewModel.verifyPhoneCode(verificationCode) }) {
                    Text("Verificar")
                }
            }
        }

        authState.error?.let { Text(it) }
    }
}