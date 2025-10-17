package cl.clinipets.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.clinipets.auth.AuthViewModel

@Composable
fun ProfileScreen(viewModel: AuthViewModel, onBeVet: () -> Unit, onVetProfile: () -> Unit = {}) {
    val profileState = viewModel.profile.collectAsState()
    val me = profileState.value

    LaunchedEffect(me) {
        if (me == null) {
            viewModel.fetchProfile()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (me != null) {
            // Placeholder avatar box
            Box(modifier = Modifier.size(96.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                Text(text = (me.email?.firstOrNull() ?: '?').toString(), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(me.email ?: "-", style = MaterialTheme.typography.titleMedium)
            Text("ID: ${me.id ?: "-"}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(12.dp))
            val roles = me.roles ?: emptyList()
            Text("Roles: ${roles.joinToString(", ")}")

            Spacer(modifier = Modifier.height(12.dp))
            if (!roles.contains("VETERINARIO")) {
                Button(onClick = { onBeVet() }) { Text("Ser veterinario") }
            } else {
                Button(onClick = { onVetProfile() }) { Text("Mi Perfil Vet") }
            }
        } else {
            Text("Cargando perfil...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
