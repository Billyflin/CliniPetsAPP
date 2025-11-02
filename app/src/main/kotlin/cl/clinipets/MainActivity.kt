// MainActivity.kt (uso mínimo del botón de login)
package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.auth.ui.LoginViewModel
import cl.clinipets.auth.ui.requestGoogleIdToken
import cl.clinipets.ui.theme.ClinipetsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClinipetsTheme { ClinipetsApp() }
        }
    }
}

@Composable
private fun ClinipetsApp(vm: LoginViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val state by vm.ui.collectAsState()
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!state.ok)
            Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        val token = requestGoogleIdToken(
                            context = context,
                            serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
                        )
                        if (!token.isNullOrBlank()) vm.loginWithGoogleIdToken(token)
                        busy = false
                    }
                }
            ) {
                if (busy || state.loading) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("Iniciar sesión con Google")
            }
            else
                Text("¡Sesión iniciada correctamente!")
        }
    }
}
