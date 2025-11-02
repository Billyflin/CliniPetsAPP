package cl.clinipets.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    displayName: String?,
    roles: List<String>,
    onNavigateToMascotas: () -> Unit,
    onLogout: () -> Unit
) {
    val normalizedRoles = roles.ifEmpty { listOf("SIN_ROL") }
    val actions = buildList {
        if ("CLIENTE" in roles) {
            add(
                HomeAction(
                    title = "Mis mascotas",
                    description = "Revisa y administra tus mascotas registradas.",
                    onClick = onNavigateToMascotas
                )
            )
        }
        if ("VETERINARIO" in roles) {
            add(
                HomeAction(
                    title = "Panel veterinario",
                    description = "Gestiona tu agenda y atenciones (próximamente)."
                )
            )
        }
        if (isEmpty()) {
            add(
                HomeAction(
                    title = "Rol pendiente",
                    description = "Aún no tienes un rol asignado. Contáctanos para activar tu cuenta."
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName?.let { "Hola, $it" } ?: "Hola",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Bienvenido a CliniPets",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Roles activos: ${normalizedRoles.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(actions) { action ->
                    HomeActionCard(action)
                }
            }
        }
    }
}

private data class HomeAction(
    val title: String,
    val description: String,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun HomeActionCard(action: HomeAction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodyMedium
            )
            action.onClick?.let { handler ->
                Button(onClick = handler) {
                    Text("Abrir")
                }
            }
        }
    }
}
