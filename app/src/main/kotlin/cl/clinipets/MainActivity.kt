package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import cl.clinipets.feature.auth.presentation.AuthGate
import cl.clinipets.navigation.AppNavHost
import cl.clinipets.ui.theme.ClinipetsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClinipetsTheme {
                ClinipetsApp()
            }
        }
    }
}

@Composable
private fun ClinipetsApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        AuthGate { estado, onCerrarSesion, onRefreshPerfil ->
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                authState = estado,
                onLogout = onCerrarSesion,
                onRefreshProfile = onRefreshPerfil,
            )
        }
    }
}
