package cl.clinipets.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import cl.clinipets.BuildConfig
import cl.clinipets.ui.auth.LoginViewModel
import cl.clinipets.ui.auth.requestGoogleIdToken
import cl.clinipets.ui.navigation.AppNavGraph
import kotlinx.coroutines.launch

@Composable
fun ClinipetsApp(vm: LoginViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val state by vm.ui.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current
    @Suppress("UNUSED_VALUE")
    var requestingGoogle by remember { mutableStateOf(false) }
    val busy = requestingGoogle || state.isAuthenticating

    Surface(Modifier.fillMaxSize()) {
        if (state.isCheckingSession) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            AppNavGraph(
                navController = navController,
                uiState = state,
                busy = busy,
                onLoginClick = {
                    if (!busy) {
                        vm.clearError()
                        scope.launch {
                            try {
                                requestingGoogle = true
                                val token = requestGoogleIdToken(
                                    context = context,
                                    serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
                                )
                                if (!token.isNullOrBlank()) {
                                    vm.loginWithGoogleIdToken(token)
                                } else {
                                    vm.setError("No se obtuvieron credenciales")
                                }
                            } finally {
                                requestingGoogle = false
                            }
                        }
                    }
                },
                onLogout = { vm.logout() },
                onRefreshProfile = { vm.refreshProfile() }
            )
        }
    }
}
