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
// import androidx.compose.foundation.shape.CutCornerShape // [CAMBIO] Ya no se usa
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
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
    onNavigateToAgendaCliente: () -> Unit,
    onNavigateToAgendaVeterinario: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState()
    val isVeterinarian = roles.contains("VETERINARIO")

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
                    onNavigateToAgendaCliente = onNavigateToAgendaCliente
                )
            }

            if (isVeterinarian) {
                item { Spacer(Modifier.height(28.dp)) }
                item { SectionTitle(title = "Herramientas profesionales") }
                item {
                    VetToolsCompact(
                        onNavigateToMiCatalogo = onNavigateToMiCatalogo,
                        onNavigateToMiDisponibilidad = onNavigateToMiDisponibilidad,
                        onNavigateToAgendaVeterinario = onNavigateToAgendaVeterinario
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
    val name = displayName?.takeIf { it.isNotBlank() }?.split(" ")?.firstOrNull() ?: "Bienvenido"

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        // [CAMBIO] Forma simétrica y muy redondeada
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            // Los colores "Container" ya son "pastel" por defecto en M3
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        shape = CircleShape, // Mantenemos el círculo
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            modifier = Modifier.padding(6.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
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
                    style = MaterialTheme.typography.bodyMedium
                )

                Image(
                    painter = painterResource(id = R.drawable.logopastel),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp)) // Redondeado
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
    onNavigateToAgendaCliente: () -> Unit
) {
    val tituloReservas = if (isVeterinarian) "Mis Reservas (Cliente)" else "Mis Reservas"

    val items = buildList {
        add(
            ActionItem(
                Icons.Default.Pets,
                "Mascotas",
                MaterialTheme.colorScheme.primaryContainer,
                onMascotas
            )
        )
        add(
            ActionItem(
                Icons.AutoMirrored.Filled.Assignment,
                "Buscar Servicio",
                MaterialTheme.colorScheme.tertiaryContainer,
                onAgenda
            )
        )
        add(
            ActionItem(
                Icons.AutoMirrored.Filled.EventNote,
                tituloReservas,
                MaterialTheme.colorScheme.surfaceContainerHighest,
                onNavigateToAgendaCliente
            )
        )
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
        // [CAMBIO] Forma redondeada en lugar de CutCornerShape
        shape = RoundedCornerShape(20.dp),
        color = item.color,
        modifier = Modifier
            // [CAMBIO] Sombra también redondeada
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clickable { item.onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Surface(
                // [CAMBIO] Fondo del ícono circular para ser más "tierno"
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
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
    onNavigateToAgendaVeterinario: () -> Unit
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
        ),
        ActionItem(
            Icons.AutoMirrored.Filled.EventNote,
            "Mis Agendas (Vet)",
            MaterialTheme.colorScheme.tertiaryContainer,
            onNavigateToAgendaVeterinario
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