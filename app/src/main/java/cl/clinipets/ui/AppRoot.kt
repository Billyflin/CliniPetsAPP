package cl.clinipets.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cl.clinipets.domain.agenda.AgendaRepository
import cl.clinipets.domain.auth.AuthRepository
import cl.clinipets.domain.catalogo.CatalogoRepository
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.mascotas.MascotasRepository
import kotlinx.coroutines.launch

@Suppress("FunctionName")
@Composable
fun AppRoot(
    authRepository: AuthRepository,
    discoveryRepository: DiscoveryRepository,
    webClientId: String,
    unauthorizedSignal: Long = 0L,
    mascotasRepository: MascotasRepository,
    agendaRepository: AgendaRepository,
    catalogoRepository: CatalogoRepository,
) {
    val snackbar = remember { SnackbarHostState() }
    val loggedIn = remember { mutableStateOf(authRepository.isLoggedIn()) }
    val scope = rememberCoroutineScope()

    // Auto-login: consulta /me al iniciar si hay JWT
    LaunchedEffect(loggedIn.value) {
        if (loggedIn.value) {
            runCatching { authRepository.me() }
                .onFailure { /* si es 401, interceptor limpiará token y notificará vía unauthorizedSignal */ }
        }
    }

    // Manejo de 401 global
    LaunchedEffect(unauthorizedSignal) {
        if (unauthorizedSignal > 0) {
            loggedIn.value = false
            snackbar.showSnackbar("Sesión expirada. Inicia sesión nuevamente.")
        }
    }

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
                cl.clinipets.ui.HomeScreen(
                    discoveryRepository = discoveryRepository,
                    mascotasRepository = mascotasRepository,
                    agendaRepository = agendaRepository,
                    catalogoRepository = catalogoRepository,
                    onLogout = {
                        // fuerza logout inmediato (token ya fue borrado por interceptor si aplica)
                        loggedIn.value = false
                    }
                )
            }
        }
    }
}
