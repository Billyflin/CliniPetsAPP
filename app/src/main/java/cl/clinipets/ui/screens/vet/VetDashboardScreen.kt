// package: cl.clinipets.ui.screens.vet

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.ui.viewmodels.AppointmentDetail
import cl.clinipets.ui.viewmodels.PetsViewModel
import cl.clinipets.ui.viewmodels.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetDashboardScreen(
    onNavigateToAgenda: () -> Unit,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToCreatePet: () -> Unit,
    vetViewModel: VetViewModel = hiltViewModel(),
    petsViewModel: PetsViewModel = hiltViewModel()
) {
    val vetState by vetViewModel.vetState.collectAsStateWithLifecycle()
    val petsState by petsViewModel.petsState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vetViewModel.loadTodayAppointments()
        Log.d("VetDashboardScreen", "Loading today appointments")

        vetViewModel.loadWeekStats()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            vetViewModel.searchPetsAndOwners(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Veterinario") },
                actions = {
                    IconButton(onClick = { vetViewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Barra de búsqueda rápida
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    showSearchResults = it.isNotEmpty()
                },
                onSearch = { },
                active = showSearchResults,
                onActiveChange = { showSearchResults = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar mascota o dueño...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            showSearchResults = false
                        }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                }
            ) {
                // Resultados de búsqueda
                LazyColumn {
                    items(vetState.searchResults) { result ->
                        ListItem(
                            headlineContent = { Text(result.pet.name) },
                            supportingContent = {
                                Column {
                                    Text(
                                        "Dueño: ${result.ownerName ?: "Sin asignar"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Coincidencia: ${result.matchType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            leadingContent = {
                                Icon(
                                    when (result.pet.species) {
                                        PetSpecies.DOG -> Icons.Default.Pets
                                        PetSpecies.CAT -> Icons.Default.Pets
                                        else -> Icons.Default.Pets
                                    },
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                showSearchResults = false
                                onNavigateToPetDetail(result.pet.id)
                            }
                        )
                    }
                }
            }

            // Estadísticas rápidas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Hoy",
                    value = vetState.todayAppointments.size.toString(),
                    subtitle = "citas",
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Semana",
                    value = vetState.weekAppointments.toString(),
                    subtitle = "citas",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pendientes",
                    value = vetState.pendingConsultations.toString(),
                    subtitle = "consultas",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            }

            // Acciones rápidas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Acciones Rápidas",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CalendarMonth,
                            label = "Ver Agenda",
                            onClick = onNavigateToAgenda
                        )
                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Add,
                            label = "Nueva Mascota",
                            onClick = onNavigateToCreatePet
                        )
                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Inventory,
                            label = "Inventario",
                            onClick = onNavigateToInventory
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Schedule,
                            label = "Horarios",
                            onClick = onNavigateToSchedule
                        )
                        QuickActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.People,
                            label = "Clientes",
                            onClick = { /* TODO */ }
                        )
                    }
                }
            }

            // Citas de hoy
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Citas de Hoy",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onNavigateToAgenda) {
                            Text("Ver todas")
                        }
                    }

                    if (vetState.todayAppointments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No hay citas programadas para hoy",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        vetState.todayAppointments.forEach { appointmentDetail ->
                            AppointmentItem(
                                appointmentDetail = appointmentDetail,
                                onStartConsultation = {
                                    onNavigateToConsultation(appointmentDetail.appointment.id)
                                },
                                onViewPet = {
                                    appointmentDetail.pet?.let {
                                        onNavigateToPetDetail(it.id)
                                    }
                                }
                            )
                            if (appointmentDetail != vetState.todayAppointments.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            // Alertas y recordatorios
            AnimatedVisibility(visible = vetState.alerts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Alertas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        vetState.alerts.forEach { alert ->
                            Text(
                                "• $alert",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 8.dp, start = 32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Mostrar errores
    vetState.error?.let { error ->
        LaunchedEffect(error) {
            // Mostrar Snackbar o diálogo de error
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AppointmentItem(
    appointmentDetail: AppointmentDetail,
    onStartConsultation: () -> Unit,
    onViewPet: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hora
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                appointmentDetail.appointment.time,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                appointmentDetail.pet?.name ?: "Mascota desconocida",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                appointmentDetail.ownerName ?: "Sin dueño asignado",
                style = MaterialTheme.typography.bodySmall,
                color = if (appointmentDetail.ownerName == null)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                appointmentDetail.appointment.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Estado
        StatusChip(appointmentDetail.appointment.status)

        // Acciones
        Row {
            IconButton(
                onClick = onViewPet,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Pets,
                    contentDescription = "Ver mascota",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (appointmentDetail.appointment.status == AppointmentStatus.SCHEDULED ||
                appointmentDetail.appointment.status == AppointmentStatus.CONFIRMED
            ) {
                IconButton(
                    onClick = onStartConsultation,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Iniciar consulta",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: AppointmentStatus) {
    val (containerColor, contentColor, icon, text) = when (status) {
        AppointmentStatus.SCHEDULED -> listOf(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.Schedule,
            "Agendada"
        )

        AppointmentStatus.CONFIRMED -> listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle,
            "Confirmada"
        )

        AppointmentStatus.COMPLETED -> listOf(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.Done,
            "Completada"
        )

        AppointmentStatus.CANCELLED -> listOf(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Cancel,
            "Cancelada"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor as Color,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon as ImageVector,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor as Color
            )
            Text(
                text as String,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}