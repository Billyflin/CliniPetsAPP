package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.clinipets.feature.auth.presentation.AuthGate
import cl.clinipets.feature.mascotas.presentation.MisMascotasRoute
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
        AuthGate { _, onCerrarSesion ->
            MisMascotasRoute(onCerrarSesion = onCerrarSesion)
        }
    }
}
