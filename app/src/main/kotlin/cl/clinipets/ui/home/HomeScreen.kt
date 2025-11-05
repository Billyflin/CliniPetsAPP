package cl.clinipets.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.R

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    displayName: String?,
    roles: List<String>,
    onNavigateToMascotas: () -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToMiCatalogo: () -> Unit,
    onNavigateToMiDisponibilidad: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState()

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
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Filled.Person, contentDescription = "Perfil")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    WelcomeSection(displayName = displayName)
                }
                item {
                    QuickActionsSection(
                        onNavigateToMascotas = onNavigateToMascotas,
                        onNavigateToDiscover = onNavigateToDiscover,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
                if (roles.contains("VETERINARIO")) {
                    item {
                        VeterinarianToolsSection(
                            onNavigateToMiCatalogo = onNavigateToMiCatalogo,
                            onNavigateToMiDisponibilidad = onNavigateToMiDisponibilidad
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun WelcomeSection(displayName: String?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logopastel),
                contentDescription = "CliniPets"
            )
            Text(
                text = displayName?.let { "Hola, $it" } ?: "Hola",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Cuida a tus mascotas con nosotros 🐾",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToMascotas: () -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Accesos rápidos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            QuickActionButton(
                icon = Icons.Filled.Pets,
                label = "Mis Mascotas",
                onClick = onNavigateToMascotas
            )
            QuickActionButton(
                icon = Icons.Filled.MedicalServices,
                label = "Descubrir Veterinarios",
                onClick = onNavigateToDiscover
            )
            QuickActionButton(
                icon = Icons.Filled.AccountCircle,
                label = "Mi Perfil",
                onClick = onNavigateToProfile
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null)
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun VeterinarianToolsSection(
    onNavigateToMiCatalogo: () -> Unit,
    onNavigateToMiDisponibilidad: () -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Herramientas de veterinario",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Button(onClick = onNavigateToMiCatalogo, modifier = Modifier.fillMaxWidth()) {
                Text("Mi catálogo")
            }
            Button(onClick = onNavigateToMiDisponibilidad, modifier = Modifier.fillMaxWidth()) {
                Text("Mi disponibilidad")
            }
        }
    }
}

@Composable
private fun ReservationsSection(
    isClient: Boolean,
    state: HomeViewModel.UiState,
    onRetry: () -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Mis próximas reservas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            when {
                !isClient -> {
                    Text(
                        text = "Esta sección está disponible para clientes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                state.loading -> {
                    CircularProgressIndicator()
                }

                state.error != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRetry) {
                            Text("Reintentar")
                        }
                    }
                }


            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RolesSection(roles: List<String>) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Roles activos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            roles.forEach { role ->
                AssistChip(
                    onClick = {},
                    label = { Text(role) },
                    colors = AssistChipDefaults.assistChipColors()
                )
            }
        }
    }
}
