package cl.clinipets.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.clinipets.auth.AuthViewModel

@Composable
fun ProfileScreen(viewModel: AuthViewModel, onBeVet: () -> Unit) {
    val profileState = viewModel.profile.collectAsState()
    val profile = profileState.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (profile != null) {
            // Placeholder avatar: show initials or a colored box
            Box(modifier = Modifier.size(96.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                Text(text = profile.name?.take(1) ?: "?", textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(profile.name ?: "-", style = MaterialTheme.typography.titleMedium)
            Text(profile.email, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(12.dp))
            // Roles chips simple text
            Text("Roles: ${profile.roles.joinToString(", ")}")

            Spacer(modifier = Modifier.height(12.dp))
            if (!profile.roles.contains("VETERINARIO")) {
                Button(onClick = { onBeVet() }) {
                    Text("Ser veterinario")
                }
            } else {
                Text("Eres veterinario", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text("Cargando perfil...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
