package cl.clinipets.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(
    estado: AuthUiState,
    onLaunchGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Inicia sesión con tu cuenta de Google para continuar.")
        estado.error?.let {
            Text(
                text = it.mensaje ?: "No pudimos iniciar sesión.",
            )
        }
        Button(
            onClick = {
                onLaunchGoogleSignIn()
            },
            enabled = !estado.cargando,
        ) {
            Text(text = "Continuar con Google")
        }
        if (estado.cargando) {
            CircularProgressIndicator()
        }
    }
}
