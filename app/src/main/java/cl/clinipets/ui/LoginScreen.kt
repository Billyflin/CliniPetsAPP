package cl.clinipets.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import cl.clinipets.ui.auth.GoogleAuthClient
import cl.clinipets.ui.auth.GoogleSignInResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "ClinipetsLogin"
private const val TAIL_LEN = 8

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("FunctionNaming")
@Composable
fun LoginScreen(
    webClientId: String,
    onLogin: (idToken: String) -> Unit,
    onError: (message: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val trimmedClientId = webClientId.trim()
    if (trimmedClientId.isBlank()) {
        val msg = "webClientId vacío. Define string google_web_client_id en res/values/auth.xml"
        Log.e(TAG, msg)
        onError(msg)
    } else {
        Log.d(
            TAG,
            "Configurado webClientId: ***${trimmedClientId.takeLast(TAIL_LEN)} (long=${trimmedClientId.length})"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Clinipets")
        Button(onClick = {
            scope.launch(Dispatchers.Main) {
                val state = lifecycleOwner.lifecycle.currentState
                if (!state.isAtLeast(Lifecycle.State.RESUMED)) {
                    onError("La actividad no está en primer plano. Intenta nuevamente.")
                    return@launch
                }
                val activity = context.findActivity()
                if (activity == null) {
                    onError("No se encontró Activity para iniciar el flujo de Google.")
                    return@launch
                }

                Log.d(TAG, "Iniciando sign-in con GoogleAuthClient…")
                when (val result = GoogleAuthClient().signIn(activity, trimmedClientId)) {
                    is GoogleSignInResult.Success -> onLogin(result.idToken)
                    is GoogleSignInResult.Cancelled -> onError("Inicio de sesión cancelado.")
                    is GoogleSignInResult.NoCredentials -> onError(
                        result.message ?: "No hay cuentas elegibles en el dispositivo."
                    )
                    is GoogleSignInResult.ConfigurationError -> onError(result.message)
                    is GoogleSignInResult.Unexpected -> onError(result.message)
                }
            }
        }) { Text("Continuar con Google") }
    }
}

