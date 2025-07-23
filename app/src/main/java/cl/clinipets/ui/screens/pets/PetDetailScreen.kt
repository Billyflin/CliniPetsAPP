// ui/screens/pets/PetDetailScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.Consultation
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSex
import cl.clinipets.data.model.VaccinationRecord
import cl.clinipets.ui.screens.profile.InfoRow
import cl.clinipets.ui.viewmodels.PetsViewModel
import java.text.SimpleDateFormat
import java.util.Date
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
    val petsState by viewModel.petsState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(petId) {
        viewModel.loadPetDetail(petId)
        viewModel.loadVaccinationRecords(petId)
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
        Column(Modifier.padding(paddingValues)) {
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

            if (petsState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> PetInfoTab(petsState.selectedPet)
                    1 -> MedicalHistoryTab(petsState.selectedPetConsultations)
                    2 -> VaccinationTab(petsState.vaccinationRecords)
                }
            }
        }
    }
}

@Composable
private fun PetInfoTab(pet: Pet?) {
    pet?.let {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        InfoRow("Nombre", pet.name)
                        InfoRow("Especie", pet.species.name)
                        InfoRow("Raza", pet.breed)
                        InfoRow("Sexo", if (pet.sex == PetSex.MALE) "Macho" else "Hembra")
                        InfoRow("Peso", "${pet.weight} kg")
                        pet.birthDate?.let { date ->
                            InfoRow(
                                "Fecha nacimiento",
                                SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale.getDefault()
                                ).format(Date(date))
                            )
                        }
                    }
                }
            }

            if (pet.notes.isNotBlank()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Notas", fontWeight = FontWeight.Bold)
                            Text(pet.notes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalHistoryTab(consultations: List<Consultation>) {
    if (consultations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin historial médico")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(consultations) { consultation ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    .format(Date(consultation.createdAt)),
                                fontWeight = FontWeight.Bold
                            )
                            Text("$${consultation.total}")
                        }
                        if (consultation.diagnosis.isNotBlank()) {
                            Text("Diagnóstico: ${consultation.diagnosis}")
                        }
                        if (consultation.treatment.isNotBlank()) {
                            Text("Tratamiento: ${consultation.treatment}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaccinationTab(records: List<VaccinationRecord>) {
    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin vacunas registradas")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(record.vaccineName, fontWeight = FontWeight.Bold)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Aplicada: ${
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        .format(Date(record.applicationDate))
                                }"
                            )
                            record.nextDoseDate?.let { nextDate ->
                                if (nextDate > System.currentTimeMillis()) {
                                    Text(
                                        "Próxima: ${
                                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                                .format(Date(nextDate))
                                        }",
                                        color = MaterialTheme.colorScheme.primary
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