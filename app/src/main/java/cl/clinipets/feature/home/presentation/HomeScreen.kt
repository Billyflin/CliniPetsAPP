package cl.clinipets.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.clinipets.feature.auth.presentation.AuthUiState

enum class HomeAction {
    MIS_MASCOTAS,
    DESCUBRIR,
    PERFIL,
    VETERINARIO,
}

@Composable
fun HomeRoute(
    estadoAuth: AuthUiState,
    onAction: (HomeAction) -> Unit,
    onRefreshProfile: () -> Unit,
) {
    HomeScreen(
        estadoAuth = estadoAuth,
        onNavigate = onAction,
        onRefreshProfile = onRefreshProfile,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    estadoAuth: AuthUiState,
    onNavigate: (HomeAction) -> Unit,
    onRefreshProfile: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "CliniPets") },
                actions = {
                    IconButton(
                        onClick = onRefreshProfile,
                        enabled = !estadoAuth.cargando,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar perfil",
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val perfil = estadoAuth.perfil
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "¡Hola ${perfil?.email ?: ""}!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    perfil?.nombre?.takeIf { it.isNotBlank() }?.let { nombre ->
                        Text(text = nombre, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (perfil?.roles != null && perfil.roles.isNotEmpty()) {
                        Text(
                            text = "Roles: ${perfil.roles.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeButton(
                    text = "Mis Mascotas",
                    onClick = { onNavigate(HomeAction.MIS_MASCOTAS) },
                )
                HomeButton(
                    text = "Descubrir servicios",
                    onClick = { onNavigate(HomeAction.DESCUBRIR) },
                )
                HomeButton(
                    text = "Mi perfil",
                    onClick = { onNavigate(HomeAction.PERFIL) },
                )
                if (perfil?.roles?.contains("VETERINARIO") == true) {
                    HomeButton(
                        text = "Panel veterinario",
                        onClick = { onNavigate(HomeAction.VETERINARIO) },
                    )
                }
            }
            estadoAuth.error?.let { error ->
                Text(
                    text = error.mensaje ?: "No pudimos actualizar tus datos.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HomeButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        Text(text = text)
    }
}
