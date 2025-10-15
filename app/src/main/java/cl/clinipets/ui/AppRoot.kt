package cl.clinipets.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cl.clinipets.domain.auth.AuthRepository
import cl.clinipets.domain.discovery.DiscoveryRepository
import kotlinx.coroutines.launch

@Suppress("FunctionName")
@Composable
fun AppRoot(
    authRepository: AuthRepository,
    discoveryRepository: DiscoveryRepository,
    webClientId: String
) {
    val snackbar = remember { SnackbarHostState() }
    val loggedIn = remember { mutableStateOf(authRepository.isLoggedIn()) }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbar) }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (!loggedIn.value) {
                LoginScreen(
                    webClientId = webClientId,
                    onLogin = { idToken ->
                        scope.launch {
                            try {
                                authRepository.loginWithGoogle(idToken)
                                loggedIn.value = true
                                snackbar.showSnackbar("Login exitoso")
                            } catch (e: Exception) {
                                snackbar.showSnackbar("Error de login: ${e.message}")
                            }
                        }
                    },
                    onError = { msg ->
                        scope.launch { snackbar.showSnackbar(msg) }
                    }
                )
            } else {
                DiscoveryScreen(discoveryRepository)
            }
        }
    }
}

