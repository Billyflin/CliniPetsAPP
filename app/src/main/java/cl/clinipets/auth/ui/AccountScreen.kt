package cl.clinipets.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val user = auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cuenta") })
        }
    ) { pads ->
        Column(
            modifier = modifier
                .padding(pads)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Usuario: ${user?.displayName ?: "Invitado"}")
            Text("Email: ${user?.email ?: "-"}")

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        auth.signOut()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }

            OutlinedButton(
                onClick = { /* TODO: Navegar a pantalla de edición perfil */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Editar perfil")
            }
        }
    }
}


@Preview(name = "AccountScreen")
@Composable
private fun PreviewAccountScreen() {
    AccountScreen(
        onLogout = {}
    )
}