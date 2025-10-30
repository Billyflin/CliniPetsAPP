package cl.clinipets.feature.auth.presentation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthGate(
    viewModel: AuthViewModel = hiltViewModel(),
    contenidoProtegido: @Composable (AuthUiState, () -> Unit) -> Unit,
) {
    val estado by viewModel.estado.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            viewModel.procesarResultadoGoogle(resultado.data)
        } else {
            viewModel.cancelarGoogleSignIn()
        }
    }

    if (estado.sesion == null) {
        AuthScreen(
            estado = estado,
            onLaunchGoogleSignIn = {
                launcher.launch(viewModel.obtenerIntentGoogle())
            },
        )
    } else {
        contenidoProtegido(estado, viewModel::cerrarSesion)
    }
}
