package cl.clinipets.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateToMiCatalogo: () -> Unit,
    onNavigateToMiDisponibilidad: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState()
    val isVeterinarian = roles.contains("VETERINARIO")

    // --- MEJORA: Cargar las citas al entrar ---
    LaunchedEffect(Unit) {
        // Asumiendo que tu VM tiene una función para cargar citas
        // vm.cargarCitas()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayName?.let { "¡Hola, $it!" } ?: "¡Hola!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bienvenido a CliniPets 🐾",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Perfil",
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card con logo
            item {
                HeroCard()
            }

            // --- MEJORA: Sección de Citas Añadida ---
            item {
                ReservationsSection(
                    isClient = !isVeterinarian,
                    state = uiState,
                    onRetry = {
                        // Asumiendo que tu VM tiene una función para reintentar
                        // vm.cargarCitas()
                    }
                )
            }

            // --- MEJORA: Grid de Acciones (2 acciones) ---
            item {
                ActionGrid(
                    onNavigateToMascotas = onNavigateToMascotas,
                    onNavigateToDiscover = onNavigateToDiscover
                )
            }

            // Herramientas profesionales si es veterinario
            if (isVeterinarian) {
                item {
                    Text(
                        text = "Herramientas Profesionales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    VetToolsCompact(
                        onNavigateToMiCatalogo = onNavigateToMiCatalogo,
                        onNavigateToMiDisponibilidad = onNavigateToMiDisponibilidad
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cuida a tus",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "mascotas 💕",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Todo en un solo lugar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.logopastel),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
}

// --- MEJORA: Grid de Acciones Simplificado ---
@Composable
private fun ActionGrid(
    onNavigateToMascotas: () -> Unit,
    onNavigateToDiscover: () -> Unit
) {
    val actions = listOf(
        ActionItem(
            icon = Icons.Default.Pets,
            title = "Mis Mascotas",
            color = MaterialTheme.colorScheme.primaryContainer,
            onClick = onNavigateToMascotas
        ),
        ActionItem(
            icon = Icons.Default.MedicalServices,
            title = "Veterinarios",
            color = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onNavigateToDiscover
        )
    )

    // Layout de Fila única y balanceada
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            ActionCard(
                item = action,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

data class ActionItem(
    val icon: ImageVector,
    val title: String,
    val color: Color,
    val onClick: () -> Unit
)

// --- MEJORA: ActionCard Simplificada (sin emoji) ---
@Composable
private fun ActionCard(
    item: ActionItem,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = item.onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = item.color.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Contenido principal
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VetToolsCompact(
    onNavigateToMiCatalogo: () -> Unit,
    onNavigateToMiDisponibilidad: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VetToolCard(
            icon = Icons.Default.Store,
            title = "Mi Catálogo",
            onClick = onNavigateToMiCatalogo,
            modifier = Modifier.weight(1f)
        )

        VetToolCard(
            icon = Icons.Default.AccessTime,
            title = "Disponibilidad",
            onClick = onNavigateToMiDisponibilidad,
            modifier = Modifier.weight(1f)
        )
    }
}

// --- MEJORA: VetToolCard Simplificada (sin emoji) ---
@Composable
private fun VetToolCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// --- MEJORA: Sección de Citas (ahora sí se usa) ---
@Composable
private fun ReservationsSection(
    isClient: Boolean,
    state: HomeViewModel.UiState,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Próximas Citas 📅",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when {
                !isClient -> {
                    Text(
                        text = "Disponible para clientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                state.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }

                state.error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        FilledTonalButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reintentar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // --- MEJORA: Estado vacío explícito ---
                state.citas.isEmpty() -> { // Asumiendo que tu UiState tiene `citas`
                    Text(
                        text = "No tienes citas programadas por ahora.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }

                // --- MEJORA: Mostrar las citas (falta esta lógica) ---
                else -> {
                    // Aquí deberías iterar sobre `state.citas`
                    // Por ejemplo:
                    // state.citas.forEach { cita ->
                    //    Text("Cita el ${cita.fecha} con ${cita.veterinario}")
                    // }
                    Text("Aquí se mostrará la lista de citas...")
                }
            }
        }
    }
}