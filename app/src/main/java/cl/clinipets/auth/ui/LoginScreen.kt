package cl.clinipets.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.clinipets.R
import cl.clinipets.auth.presentation.AuthUiState
import cl.clinipets.auth.presentation.LoginViewModel

@Composable
fun LoginScreen(vm: LoginViewModel) {
    val ctx = LocalContext.current
    val uiState by vm.uiState.collectAsState() // Loading / LoggedOut / LoggedIn
    val snackbar = remember { SnackbarHostState() }

    // Mostrar errores como snackbars
    LaunchedEffect(Unit) {
        vm.errors.collect { msg ->
            snackbar.showSnackbar(msg)
        }
    }

    val loading = uiState == AuthUiState.Loading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pads ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(pads),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))

                Image(
                    painter = painterResource(id = R.drawable.logopastel),
                    contentDescription = "Logo Clinipets",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bienvenido",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Inicia sesión para continuar",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    )

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = { vm.signIn(ctx) },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(50)),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (loading) "Entrando..." else "Entrar con Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}
