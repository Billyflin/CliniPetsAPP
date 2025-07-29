// ui/screens/pets/PetDetailScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.Consultation
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSex
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.ui.theme.ColorFamily
import cl.clinipets.ui.theme.LocalExtendedColors
import cl.clinipets.ui.viewmodels.PetsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// package cl.clinipets.ui.screens.pets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToNewAppointment: (String) -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    val petsState by viewModel.petsState.collectAsStateWithLifecycle()
    val extColors = LocalExtendedColors.current
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(petId) {
        viewModel.loadPetDetail(petId)
        viewModel.loadVaccinationRecords(petId)
    }

    val pet = petsState.selectedPet
    val backgroundColor = when (pet?.species) {
        PetSpecies.DOG -> extColors.peach
        PetSpecies.CAT -> extColors.lavander
        else -> extColors.mint
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(petsState.selectedPet?.name ?: "Mascota")
                        Text(
                            when (pet?.species) {
                                PetSpecies.DOG -> "Perrito"
                                PetSpecies.CAT -> "Gatito"
                                else -> "Mascota"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(petId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToNewAppointment(petId) },
                containerColor = extColors.mint.color,
                contentColor = extColors.mint.onColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Agendar cita")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agendar Cita")
            }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            // Header con avatar de mascota
            pet?.let {
                Surface(
                    color = backgroundColor.colorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = backgroundColor.color.copy(alpha = 0.2f),
                            modifier = Modifier.size(120.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (pet.species) {
                                        PetSpecies.DOG -> "🐕"
                                        PetSpecies.CAT -> "🐈"
                                        else -> "🐾"
                                    },
                                    style = MaterialTheme.typography.displayLarge
                                )
                            }
                        }
                    }
                }
            }

            // Tabs personalizados
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = "Información",
                        icon = Icons.Default.Info,
                        color = backgroundColor
                    )
                    TabChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = "Historial",
                        icon = Icons.Default.History,
                        color = backgroundColor
                    )
                    TabChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = "Vacunas",
                        icon = Icons.Default.Vaccines,
                        color = backgroundColor
                    )
                }
            }

            if (petsState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = backgroundColor.color)
                }
            } else {
                when (selectedTab) {
                    0 -> PetInfoTab(petsState.selectedPet, backgroundColor)
//                    1 -> MedicalHistoryTab(petsState.selectedPetConsultations, backgroundColor)
//                    2 -> VaccinationTab(petsState.vaccinationRecords, backgroundColor)
                }
            }
        }
    }
}

@Composable
private fun MedicalHistoryTab(consultations: List<Consultation>, color: ColorFamily) {
    if (consultations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.colorContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "📋",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
                Text(
                    "Sin historial médico",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Las consultas aparecerán aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(consultations) { consultation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = color.colorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header con fecha y total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = color.onColorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es"))
                                        .format(Date(consultation.createdAt)),
                                    fontWeight = FontWeight.Bold,
                                    color = color.onColorContainer
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = color.color
                            ) {
                                Text(
                                    "${consultation.total}",
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = color.onColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Diagnóstico
                        if (consultation.diagnosis.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = color.onColorContainer.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MedicalServices,
                                            contentDescription = null,
                                            tint = color.onColorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Diagnóstico",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = color.onColorContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Text(
                                        consultation.diagnosis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color.onColorContainer
                                    )
                                }
                            }
                        }

                        // Tratamiento
                        if (consultation.treatment.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = color.onColorContainer.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Healing,
                                            contentDescription = null,
                                            tint = color.onColorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Tratamiento",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = color.onColorContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Text(
                                        consultation.treatment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color.onColorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun TabChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    color: ColorFamily
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color.color else color.colorContainer,

        ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) color.onColor else color.onColorContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) color.onColor else color.onColorContainer
            )
        }
    }
}

@Composable
private fun PetInfoTab(pet: Pet?, color: ColorFamily) {
    pet?.let {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = color.colorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoItem(
                            icon = Icons.Default.Badge,
                            label = "Nombre",
                            value = pet.name,
                            color = color
                        )
                        InfoItem(
                            icon = Icons.Default.Pets,
                            label = "Especie",
                            value = when (pet.species) {
                                PetSpecies.DOG -> "Perro"
                                PetSpecies.CAT -> "Gato"
                                else -> "Otra"
                            },
                            color = color
                        )
                        InfoItem(
                            icon = Icons.Default.Category,
                            label = "Raza",
                            value = pet.breed,
                            color = color
                        )
                        InfoItem(
                            icon = when (pet.sex) {
                                PetSex.MALE -> Icons.Default.Male
                                PetSex.FEMALE -> Icons.Default.Female
                            },
                            label = "Sexo",
                            value = if (pet.sex == PetSex.MALE) "Macho" else "Hembra",
                            color = color
                        )
                        InfoItem(
                            icon = Icons.Default.Scale,
                            label = "Peso",
                            value = "${pet.weight} kg",
                            color = color
                        )
                        pet.birthDate?.let { date ->
                            InfoItem(
                                icon = Icons.Default.Cake,
                                label = "Fecha de nacimiento",
                                value = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es")).format(
                                    Date(date)
                                ),
                                color = color
                            )
                        }
                    }
                }
            }

            if (pet.notes.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Notas especiales",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                pet.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: ColorFamily
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.color.copy(alpha = 0.2f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color.onColorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = color.onColorContainer.copy(alpha = 0.7f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = color.onColorContainer
            )
        }
    }
}