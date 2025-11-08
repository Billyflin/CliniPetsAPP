package cl.clinipets.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccessTime
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.R
import cl.clinipets.core.di.ApiModule.resolveBaseUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    displayName: String?,
    roles: List<String>,
    onNavigateToMascotas: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMiCatalogo: () -> Unit,
    onNavigateToMiDisponibilidad: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState()
    val isVeterinarian = roles.contains("VETERINARIO")
    val baseUrl = resolveBaseUrl()

    LaunchedEffect(Unit) {
        vm.cargarCitas()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Perfil",
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { HomeHeader(displayName = displayName, baseUrl = baseUrl) }
            item { Spacer(Modifier.height(12.dp)) }
            item { QuickActions(isVeterinarian = isVeterinarian, onNavigateToMascotas, onNavigateToAgenda, onNavigateToMiCatalogo, onNavigateToMiDisponibilidad) }
            item { Spacer(Modifier.height(24.dp)) }
            item { SectionTitle(title = "Próximas citas") }
            item {
                // Veterinario también ve sus citas (rol aditivo)
                ReservationsSection(
                    state = uiState,
                    onRetry = { vm.reintentar() },
                    onViewAll = onNavigateToAgenda
                )
            }
            if (isVeterinarian) {
                item { Spacer(Modifier.height(28.dp)) }
                item { SectionTitle(title = "Herramientas profesionales") }
                item { VetToolsCompact(onNavigateToMiCatalogo, onNavigateToMiDisponibilidad, onNavigateToAgenda) }
            }
        }
    }
}

@Composable
private fun HomeHeader(displayName: String?, baseUrl: String) {
    val name = displayName?.takeIf { it.isNotBlank() } ?: "Bienvenido"
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${"Hola"} $name 👋",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Gestiona tu mundo veterinario y tus mascotas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                AssistiveInfo(baseUrl = baseUrl)
            }
            Image(
                painter = painterResource(id = R.drawable.logopastel),
                contentDescription = "Logo",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }
    }
}

@Composable
private fun AssistiveInfo(baseUrl: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "Backend: $baseUrl",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

// Acción rápida: contrato simple para chips/botones
private data class ActionItem(
    val icon: ImageVector,
    val title: String,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun QuickActions(
    isVeterinarian: Boolean,
    onMascotas: () -> Unit,
    onAgenda: () -> Unit,
    onCatalogo: () -> Unit,
    onDisponibilidad: () -> Unit
) {
    val scroll = rememberScrollState()
    val common = listOf(
        ActionItem(Icons.Default.Pets, "Mascotas", MaterialTheme.colorScheme.primaryContainer, onMascotas),
        ActionItem(Icons.AutoMirrored.Filled.Assignment, "Agenda", MaterialTheme.colorScheme.tertiaryContainer, onAgenda)
    )
    val vetExtra = if (isVeterinarian) listOf(
        ActionItem(Icons.Default.Store, "Catálogo", MaterialTheme.colorScheme.secondaryContainer, onCatalogo),
        ActionItem(Icons.Default.AccessTime, "Disponibilidad", MaterialTheme.colorScheme.surfaceVariant, onDisponibilidad)
    ) else emptyList()
    val items = common + vetExtra

    Row(
        modifier = Modifier
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (action in items) {
            ActionChip(action)
        }
    }
}

@Composable
private fun ActionChip(item: ActionItem) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = item.color.copy(alpha = 0.35f),
        modifier = Modifier
            .widthIn(min = 120.dp)
            .shadow(1.dp, RoundedCornerShape(20.dp))
            .clickable { item.onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column { Text(item.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// Herramientas para veterinario (grid compacto reutilizando chips)
@Composable
private fun VetToolsCompact(
    onNavigateToMiCatalogo: () -> Unit,
    onNavigateToMiDisponibilidad: () -> Unit,
    onNavigateToAgenda: () -> Unit
) {
    val actions = listOf(
        ActionItem(Icons.Default.Store, "Mi catálogo", MaterialTheme.colorScheme.secondaryContainer, onNavigateToMiCatalogo),
        ActionItem(Icons.Default.AccessTime, "Disponibilidad", MaterialTheme.colorScheme.surfaceVariant, onNavigateToMiDisponibilidad),
        ActionItem(Icons.AutoMirrored.Filled.Assignment, "Agenda", MaterialTheme.colorScheme.tertiaryContainer, onNavigateToAgenda)
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (item in rowItems) {
                    Box(modifier = Modifier.weight(1f)) { ActionChip(item) }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// --- MEJORA: Sección de Citas (ahora sí se usa) ---
@Composable
private fun ReservationsSection(
    state: HomeViewModel.UiState,
    onRetry: () -> Unit,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                state.loading -> LoadingAppointments()
                state.error != null -> ErrorAppointments(error = state.error, onRetry = onRetry)
                state.citas.isEmpty() -> EmptyAppointments(onViewAll)
                else -> AppointmentList(citas = state.citas, onViewAll = onViewAll)
            }
        }
    }
}

@Composable private fun LoadingAppointments() { Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp) } }
@Composable private fun EmptyAppointments(onViewAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("No tienes citas programadas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onViewAll, shape = RoundedCornerShape(12.dp)) { Text("Ver Agenda") }
    }
}

@Composable
private fun ErrorAppointments(error: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        FilledTonalButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) { Text("Reintentar") }
    }
}

@Composable
private fun AppointmentList(citas: List<String>, onViewAll: () -> Unit) { // Adaptado a UiState existente
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        citas.take(5).forEach { texto -> AppointmentItem(texto) }
        if (citas.size > 5) {
            FilledTonalButton(onClick = onViewAll, shape = RoundedCornerShape(12.dp)) { Text("Ver todas en Agenda") }
        }
    }
}

@Composable
private fun AppointmentItem(texto: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(texto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Detalle de la cita", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
