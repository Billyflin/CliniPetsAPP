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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.R
import cl.clinipets.openapi.models.Reserva
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    displayName: String?,
    roles: List<String>,
    onNavigateToMascotas: () -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState()
    val isClient = roles.any { it.equals("CLIENTE", ignoreCase = true) }

    LaunchedEffect(isClient) {
        if (isClient) vm.refresh() else vm.clear()
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
                item {
                    ReservationsSection(
                        isClient = isClient,
                        state = uiState,
                        onRetry = vm::refresh
                    )
                }
                if (!isClient && roles.isNotEmpty()) {
                    item {
                        RolesSection(roles = roles)
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

                state.reservas.isEmpty() -> {
                    Text(
                        text = "Aún no tienes reservas, ¡empieza en Descubrir!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    state.reservas.take(3).forEach { reserva ->
                        ReservationRow(reserva = reserva)
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

@Composable
private fun ReservationRow(reserva: Reserva) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = reserva.mascota.nombre,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = formatReservaFecha(reserva),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Estado: ${reserva.estado.value}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatReservaFecha(reserva: Reserva): String {
    val formatter = DateTimeFormatter.ofPattern("EEE d MMM • HH:mm", Locale.getDefault())
    val zoned = reserva.inicio.atZoneSameInstant(ZoneId.systemDefault())
    return formatter.format(zoned)
}
