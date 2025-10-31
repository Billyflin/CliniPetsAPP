package cl.clinipets.feature.perfil.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.clinipets.feature.auth.presentation.AuthUiState

@Composable
fun PerfilRoute(
    estado: AuthUiState,
    onBack: () -> Unit,
    onCerrarSesion: () -> Unit,
    onIrOnboardingVet: () -> Unit,
) {
    PerfilScreen(
        estado = estado,
        onBack = onBack,
        onCerrarSesion = onCerrarSesion,
        onIrOnboardingVet = onIrOnboardingVet,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerfilScreen(
    estado: AuthUiState,
    onBack: () -> Unit,
    onCerrarSesion: () -> Unit,
    onIrOnboardingVet: () -> Unit,
) {
    val perfil = estado.perfil
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Mi perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = perfil?.email ?: "Correo no disponible")
            perfil?.nombre?.takeIf { it.isNotBlank() }?.let {
                Text(text = it)
            }
            if (perfil?.roles != null) {
                Text(text = "Roles: ${perfil.roles.joinToString()}")
                if (!perfil.roles.contains("VETERINARIO")) {
                    Button(
                        onClick = onIrOnboardingVet,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !estado.cargando,
                    ) {
                        Text(text = "Quiero ser veterinario")
                    }
                }
            }

            Button(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Cerrar sesión")
            }
        }
    }
}
