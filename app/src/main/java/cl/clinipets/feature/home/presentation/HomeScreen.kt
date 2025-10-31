package cl.clinipets.feature.home.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.clinipets.R
import cl.clinipets.feature.auth.presentation.AuthUiState

enum class HomeAction {
    MIS_MASCOTAS,
    DESCUBRIR,
    PERFIL,
    VETERINARIO,
}

private data class QuickAction(
    val titulo: String,
    val descripcion: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector,
    val action: HomeAction,
)

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
    val perfil = estadoAuth.perfil
    val nombreVisible = perfil?.nombre?.takeIf { it.isNotBlank() }
        ?: perfil?.email?.substringBefore("@")
        ?: "Hola!"
    val esVeterinario = perfil?.roles?.contains("VETERINARIO") == true
    val acciones = buildList {
        add(
            QuickAction(
                titulo = "Mis mascotas",
                descripcion = "Vacunas, controles y fichas clínicas",
                icono = Icons.Default.Pets,
                action = HomeAction.MIS_MASCOTAS,
            ),
        )
        add(
            QuickAction(
                titulo = "Descubrir servicios",
                descripcion = "Encuentra veterinarios y especialistas",
                icono = Icons.Default.Search,
                action = HomeAction.DESCUBRIR,
            ),
        )
        add(
            QuickAction(
                titulo = "Mi perfil",
                descripcion = "Actualiza tus datos y preferencias",
                icono = Icons.Default.Person,
                action = HomeAction.PERFIL,
            ),
        )
        if (esVeterinario) {
            add(
                QuickAction(
                    titulo = "Panel veterinario",
                    descripcion = "Gestiona agenda y disponibilidad",
                    icono = Icons.Default.CalendarMonth,
                    action = HomeAction.VETERINARIO,
                ),
            )
        }
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                HeroSection(nombre = nombreVisible, esVeterinario = esVeterinario)
            }

            item {
                QuickActionsSection(
                    acciones = acciones,
                    onNavigate = onNavigate,
                )
            }

            if (!esVeterinario) {
                item {
                    InviteVeterinarianCard(onNavigate = onNavigate)
                }
            }

            estadoAuth.error?.let { error ->
                item {
                    WarningCard(mensaje = error.mensaje ?: "No pudimos actualizar tus datos. Intenta refrescar.")
                }
            }
        }
    }
}

@Composable
private fun HeroSection(nombre: String, esVeterinario: Boolean) {
    val fondo = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        ),
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .background(fondo, shape = RoundedCornerShape(28.dp))
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = "Hola, $nombre",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                Text(
                    text = if (esVeterinario) {
                        "Gestiona tus pacientes, agenda y disponibilidad desde un solo lugar."
                    } else {
                        "Administra la salud de tus mascotas y encuentra ayuda veterinaria cuando lo necesites."
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    ),
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (esVeterinario) "Profesional verificado" else "Cuenta de tutor",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    acciones: List<QuickAction>,
    onNavigate: (HomeAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Accesos rápidos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            acciones.forEach { accion ->
                QuickActionCard(
                    action = accion,
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    onNavigate: (HomeAction) -> Unit,
) {
    Card(
        onClick = { onNavigate(action.action) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Icon(
                    imageVector = action.icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = action.titulo,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = action.descripcion,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun InviteVeterinarianCard(onNavigate: (HomeAction) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logopastel),
                    contentDescription = null,
                    modifier = Modifier.height(48.dp),
                )
                Text(
                    text = "¿Eres veterinario?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Text(
                text = "Completa tu solicitud para aparecer en CliniPets, recibir reservas y gestionar tu agenda en línea.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { onNavigate(HomeAction.PERFIL) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Activar modo profesional")
            }
        }
    }
}

@Composable
private fun WarningCard(mensaje: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Algo no salió bien",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
