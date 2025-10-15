package cl.clinipets.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.clinipets.domain.auth.AuthRepository

@Composable
fun ProfileScreen(onLogout: () -> Unit, authRepository: AuthRepository) {
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    val authenticated = remember { mutableStateOf<Boolean?>(null) }
    val userId = remember { mutableStateOf<String?>(null) }
    val email = remember { mutableStateOf<String?>(null) }
    val roles = remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading.value = true
        error.value = null
        runCatching { authRepository.me() }
            .onSuccess { me ->
                authenticated.value = me.authenticated
                userId.value = me.id
                email.value = me.email
                roles.value = me.roles
            }
            .onFailure { e -> error.value = e.message }
        loading.value = false
    }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        when {
            loading.value -> CircularProgressIndicator()
            error.value != null -> Text(
                "Error: ${error.value}",
                color = MaterialTheme.colorScheme.error
            )

            else -> {
                Text("Autenticado: ${authenticated.value}")
                Text("ID: ${userId.value}")
                Text("Email: ${email.value}")
                Text("Roles: ${roles.value.joinToString(", ")}")
            }
        }
        Divider(Modifier.padding(vertical = 12.dp))
        Button(onClick = onLogout) { Text("Cerrar sesión") }
    }
}
