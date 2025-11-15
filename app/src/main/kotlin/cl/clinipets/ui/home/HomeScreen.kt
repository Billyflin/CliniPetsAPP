package cl.clinipets.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote // NUEVO: Import para el ícono
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.R

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
    onNavigateToAgendaGestion: () -> Unit, // NUEVO: Parámetro de V2
    vm: HomeViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState() // Esto ya no se usa, pero se mantiene por si vm es necesario
    val isVeterinarian = roles.contains("VETERINARIO")

    // Esto ya no es necesario si quitamos las citas
    // LaunchedEffect(Unit) {
    //     vm.cargarCitas()
    // }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding), contentPadding = PaddingValues(
                top = 24.dp, bottom = 32.dp
            )
        ) {
            item {
                HomeHeader(
                    displayName = displayName, onNavigateToProfile = onNavigateToProfile
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                QuickActions(
                    isVeterinarian = isVeterinarian,
                    onMascotas = onNavigateToMascotas,
                    onAgenda = onNavigateToAgenda,
                    onCatalogo = onNavigateToMiCatalogo,
                    onDisponibilidad = onNavigateToMiDisponibilidad,
                    onGestionAgendas = onNavigateToAgendaGestion // NUEVO: Pasando el parámetro
                )
            }

            // === SECCIÓN DE CITAS ELIMINADA ===

            if (isVeterinarian) {
                item { Spacer(Modifier.height(28.dp)) }
                item { SectionTitle(title = "Herramientas profesionales") }
                item {
                    VetToolsCompact(
                        onNavigateToMiCatalogo = onNavigateToMiCatalogo,
                        onNavigateToMiDisponibilidad = onNavigateToMiDisponibilidad,
                        onNavigateToAgenda = onNavigateToAgenda
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    displayName: String?, onNavigateToProfile: () -> Unit
) {
    val name = displayName?.takeIf { it.isNotBlank() } ?: "Bienvenido"

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hola $name 👋",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = onNavigateToProfile, modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            modifier = Modifier.padding(6.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gestiona tu mundo veterinario y tus mascotas.",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Image(
                    painter = painterResource(id = R.drawable.logopastel),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
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

private data class ActionItem(
    val icon: ImageVector, val title: String, val color: Color, val onClick: () -> Unit
)

@Composable
private fun QuickActions(
    isVeterinarian: Boolean,
    onMascotas: () -> Unit,
    onAgenda: () -> Unit,
    onCatalogo: () -> Unit,
    onDisponibilidad: () -> Unit,
    onGestionAgendas: () -> Unit // NUEVO: Parámetro de V2
) {
    val items = buildList {
        add(
            ActionItem(
                Icons.Default.Pets,
                "Mascotas",
                MaterialTheme.colorScheme.primaryContainer,
                onMascotas
            )
        )
        if (isVeterinarian) {
            add(
                ActionItem(
                    Icons.Default.Store,
                    "Catálogo",
                    MaterialTheme.colorScheme.secondaryContainer,
                    onCatalogo
                )
            )
        }
        add(
            ActionItem(
                Icons.AutoMirrored.Filled.Assignment,
                "Agenda",
                MaterialTheme.colorScheme.tertiaryContainer,
                onAgenda
            )
        )
        // NUEVO: Botón "Gestionar agendas" añadido a la lista
        add(
            ActionItem(
                Icons.AutoMirrored.Filled.EventNote,
                "Gestionar agendas",
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), // Color similar a V2
                onGestionAgendas
            )
        )
        if (isVeterinarian) {
            add(
                ActionItem(
                    Icons.Default.AccessTime,
                    "Disponibilidad",
                    MaterialTheme.colorScheme.surfaceVariant,
                    onDisponibilidad
                )
            )
        }
    }

    ActionGrid(items)
}

@Composable
private fun ActionGrid(items: List<ActionItem>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        ActionChip(item)
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActionChip(item: ActionItem) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = item.color.copy(alpha = 0.35f),
        modifier = Modifier
            .shadow(1.dp, RoundedCornerShape(20.dp))
            .clickable { item.onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape, color = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VetToolsCompact(
    onNavigateToMiCatalogo: () -> Unit,
    onNavigateToMiDisponibilidad: () -> Unit,
    onNavigateToAgenda: () -> Unit
) {
    val actions = listOf(
        ActionItem(
            Icons.Default.Store,
            "Mi catálogo",
            MaterialTheme.colorScheme.secondaryContainer,
            onNavigateToMiCatalogo
        ), ActionItem(
            Icons.Default.AccessTime,
            "Disponibilidad",
            MaterialTheme.colorScheme.surfaceVariant,
            onNavigateToMiDisponibilidad
        ), ActionItem(
            Icons.AutoMirrored.Filled.Assignment,
            "Agenda",
            MaterialTheme.colorScheme.tertiaryContainer,
            onNavigateToAgenda
        )
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (item in rowItems) {
                    Box(modifier = Modifier.weight(1f)) {
                        ActionChip(item)
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}