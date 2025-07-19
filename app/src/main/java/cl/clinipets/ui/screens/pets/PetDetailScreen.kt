// ui/screens/pets/PetDetailScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.MedicalRecord
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.data.model.RecordType
import cl.clinipets.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNewAppointment: () -> Unit,
    viewModel: PetDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val extColors = LocalExtendedColors.current

    LaunchedEffect(petId) {
        viewModel.loadPetDetail(petId)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        uiState.pet?.name ?: "Cargando...",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Edit pet */ }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { /* TODO: Share */ }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Compartir")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewAppointment,
                containerColor = extColors.mint.color,
                contentColor = extColors.mint.onColor,
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp)
                )
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agendar Cita")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.pet != null) {
            PetDetailContent(
                pet = uiState.pet!!,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun PetDetailContent(
    pet: Pet,
    modifier: Modifier = Modifier
) {
    val extColors = LocalExtendedColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card con información básica
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (pet.species) {
                    PetSpecies.DOG -> extColors.mint.colorContainer
                    PetSpecies.CAT -> extColors.lavander.colorContainer
                    else -> extColors.peach.colorContainer
                }
            )
        ) {
            Box {
                // Background gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pet Avatar
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (pet.species) {
                                PetSpecies.DOG -> "🐕"
                                PetSpecies.CAT -> "🐈"
                                PetSpecies.RABBIT -> "🐰"
                                PetSpecies.BIRD -> "🦜"
                                else -> "🐾"
                            },
                            style = MaterialTheme.typography.displayLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pet Info Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InfoChip(
                            icon = Icons.Outlined.Cake,
                            label = "Edad",
                            value = "${pet.age} años"
                        )
                        InfoChip(
                            icon = Icons.Outlined.Monitor,
                            label = "Peso",
                            value = "${pet.weight} kg"
                        )
                        InfoChip(
                            icon = Icons.Outlined.Pets,
                            label = "Raza",
                            value = pet.breed
                        )
                    }
                }
            }
        }

        // Quick Actions
        Text(
            "Acciones Rápidas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Filled.Vaccines,
                title = "Vacunas",
                subtitle = "Ver historial",
                color = extColors.pink.colorContainer,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Filled.Emergency,
                title = "Emergencia",
                subtitle = "Llamar ahora",
                color = MaterialTheme.colorScheme.errorContainer,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
        }

        // Medical History Section
        if (pet.medicalHistory.isNotEmpty()) {
            Text(
                "Historial Médico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            pet.medicalHistory.forEach { record ->
                MedicalRecordCard(record)
            }
        }

        // Stats Section
        Text(
            "Estadísticas de Salud",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Última visita",
                    value = "Hace 2 meses"
                )
                StatRow(
                    icon = Icons.Outlined.Vaccines,
                    label = "Vacunas al día",
                    value = "✓ Completas"
                )
                StatRow(
                    icon = Icons.Outlined.LocalHospital,
                    label = "Próximo control",
                    value = "15 de Febrero"
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
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
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MedicalRecordCard(record: MedicalRecord) {
    val extColors = LocalExtendedColors.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (record.type) {
                    RecordType.VACCINATION -> extColors.mint.colorContainer
                    RecordType.SURGERY -> extColors.pink.colorContainer
                    RecordType.EMERGENCY -> MaterialTheme.colorScheme.errorContainer
                    else -> extColors.lavander.colorContainer
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (record.type) {
                            RecordType.VACCINATION -> Icons.Filled.Vaccines
                            RecordType.SURGERY -> Icons.Filled.LocalHospital
                            RecordType.EMERGENCY -> Icons.Filled.Emergency
                            RecordType.CHECKUP -> Icons.Filled.HealthAndSafety
                            RecordType.CONSULTATION -> Icons.Filled.MedicalServices
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}