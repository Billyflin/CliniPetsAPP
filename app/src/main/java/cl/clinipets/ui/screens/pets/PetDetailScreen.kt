// ui/screens/pets/PetDetailScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.Consultation
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.VaccinationRecord
import cl.clinipets.ui.viewmodels.PetsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToNewAppointment: (String) -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    val petsState by viewModel.petsState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(petId) {
        viewModel.loadPetDetail(petId)
    }

    LaunchedEffect(petsState.isPetDeleted) {
        if (petsState.isPetDeleted) {
            viewModel.clearState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(petsState.selectedPet?.name ?: "Mascota") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(petId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToNewAppointment(petId) }
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Agendar cita")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (petsState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                petsState.selectedPet?.let { pet ->
                    // Tabs
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Información") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Historial") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Vacunas") }
                        )
                    }

                    // Contenido según tab
                    when (selectedTab) {
                        0 -> PetInfoTab(pet)
                        1 -> MedicalHistoryTab(
                            consultations = petsState.selectedPetConsultations
                        )

                        2 -> VaccinationTab(
                            vaccinationRecords = petsState.vaccinationRecords
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar mascota") },
                text = { Text("¿Estás seguro de que quieres eliminar a ${petsState.selectedPet?.name}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePet(petId)
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun VaccinationTab(vaccinationRecords: List<VaccinationRecord>) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Vacunas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Divider()
        }
        items(vaccinationRecords) { record ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Vacuna: ${record.vaccineName}")
                    Text(
                        "Fecha de aplicación: ${
                            SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                            ).format(record.applicationDate)
                        }"
                    )
                    Text(
                        "Próxima dosis: ${
                            SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                            ).format(record.nextDoseDate)
                        }"
                    )
                }
            }
        }
    }
    TODO("Not yet implemented")
}

@Composable
fun MedicalHistoryTab(consultations: List<Consultation>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Historial médico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Divider()
        }
        items(consultations) { consultation ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Fecha: ${
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                                consultation.createdAt
                            )
                        }"
                    )
                    Text("Descripción: ${consultation.diagnosis}")
                    Text("Veterinario: ${consultation.veterinarianId}")
                }
            }
        }
    }
    TODO("Not yet implemented")
}

@Composable
private fun PetInfoTab(pet: Pet) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Información básica",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Divider()
                    Text("Nombre: ${pet.name}")
                    Text("Especie: ${pet.species.name}")
                    Text("Raza: ${pet.breed}")
                    Text(
                        "Fecha de nacimiento: ${
                            pet.birthDate?.let {
                                SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale.getDefault()
                                ).format(it)
                            } ?: "Desconocida"
                        }")
                    Text("Peso: ${pet.weight} kg")
                }
            }
        }
    }
}