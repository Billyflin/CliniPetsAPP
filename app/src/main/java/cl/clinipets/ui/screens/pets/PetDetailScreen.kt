// ui/screens/pets/PetDetailScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.*
import cl.clinipets.ui.viewmodels.PetsViewModel
import java.text.SimpleDateFormat
import java.util.*

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
    var showVaccineDialog by remember { mutableStateOf(false) }

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

                    // Contenido según tab seleccionado
                    when (selectedTab) {
                        0 -> PetInfoTab(pet)
                        1 -> MedicalHistoryTab(
                            medicalHistory = petsState.selectedPetMedicalHistory,
                            onConsultationClick = { /* TODO: Navegar a detalle de consulta */ }
                        )
                        2 -> VaccinationTab(
                            vaccinationRecords = petsState.vaccinationRecords,
                            onAddVaccine = { showVaccineDialog = true }
                        )
                    }
                }
            }
        }

        // Diálogos
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar mascota") },
                text = { Text("¿Estás seguro de que quieres eliminar a ${petsState.selectedPet?.name}? Esta acción no se puede deshacer.") },
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

        if (showVaccineDialog) {
            AddVaccineDialog(
                petId = petId,
                onDismiss = { showVaccineDialog = false },
                onVaccineAdded = {
                    showVaccineDialog = false
                    viewModel.loadVaccinationRecords(petId)
                }
            )
        }

        petsState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearState() }) {
                        Text("Cerrar")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun PetInfoTab(pet: Pet) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    InfoRow("Especie", pet.species.name)
                    InfoRow("Raza", pet.breed)
                    pet.birthDate?.let { birthDate ->
                        val age = calculateAge(birthDate)
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(birthDate))
                        InfoRow("Fecha de nacimiento", "$dateStr ($age años)")
                    }
                    InfoRow("Sexo", if (pet.sex == PetSex.MALE) "Macho" else "Hembra")
                    InfoRow("Esterilizado", if (pet.isNeutered) "Sí" else "No")
                    InfoRow("Peso", "${pet.weight} kg")
                    pet.microchipId?.let {
                        InfoRow("Microchip", it)
                    }
                }
            }
        }

        if (pet.notes.isNotBlank()) {
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Divider()
                        Text(pet.notes)
                    }
                }
            }
        }

        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Acciones rápidas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO: Generar carnet */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Carnet")
                        }
                        OutlinedButton(
                            onClick = { /* TODO: Exportar historial */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalHistoryTab(
    medicalHistory: MedicalHistory?,
    onConsultationClick: (String) -> Unit
) {
    if (medicalHistory == null || medicalHistory.consultations.isEmpty()) {
        EmptyState(
            icon = Icons.Default.MedicalServices,
            title = "Sin historial médico",
            message = "Aún no hay consultas registradas para esta mascota"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(medicalHistory.consultations) { entry ->
                ConsultationCard(
                    entry = entry,
                    onClick = { onConsultationClick(entry.consultationId) }
                )
            }

            if (medicalHistory.allergies.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alergias conocidas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            medicalHistory.allergies.forEach { allergy ->
                                Text("• $allergy")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaccinationTab(
    vaccinationRecords: List<VaccinationRecord>,
    onAddVaccine: () -> Unit
) {
    if (vaccinationRecords.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Vaccines,
            title = "Sin vacunas registradas",
            message = "Registra las vacunas de tu mascota para llevar un control",
            actionLabel = "Agregar vacuna",
            onAction = onAddVaccine
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Registro de vacunas",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onAddVaccine) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar")
                    }
                }
            }

            items(vaccinationRecords) { record ->
                VaccinationCard(record)
            }
        }
    }
}

@Composable
private fun ConsultationCard(
    entry: MedicalHistoryEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(entry.date)),
                    style = MaterialTheme.typography.titleSmall
                )
                if (entry.diagnosis.isNotBlank()) {
                    Text(
                        text = "Diagnóstico: ${entry.diagnosis}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (entry.treatment.isNotBlank()) {
                    Text(
                        text = "Tratamiento: ${entry.treatment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                entry.weight?.let {
                    Text(
                        text = "Peso: $it kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun VaccinationCard(record: VaccinationRecord) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.vaccineName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(record.applicationDate)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Lote: ${record.batch}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            record.certificateNumber?.let {
                Text(
                    text = "Certificado: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            record.nextDoseDate?.let { nextDate ->
                val daysUntilNext = ((nextDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                if (daysUntilNext > 0) {
                    Text(
                        text = "Próxima dosis en $daysUntilNext días",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else if (daysUntilNext == 0) {
                    Text(
                        text = "Próxima dosis HOY",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Próxima dosis VENCIDA hace ${-daysUntilNext} días",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddVaccineDialog(
    petId: String,
    onDismiss: () -> Unit,
    onVaccineAdded: () -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    var vaccineName by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var hasNextDose by remember { mutableStateOf(false) }
    var nextDoseDays by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar vacuna") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = vaccineName,
                    onValueChange = { vaccineName = it },
                    label = { Text("Nombre de la vacuna") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = batch,
                    onValueChange = { batch = it },
                    label = { Text("Número de lote") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Requiere próxima dosis?")
                    Switch(
                        checked = hasNextDose,
                        onCheckedChange = { hasNextDose = it }
                    )
                }
                if (hasNextDose) {
                    OutlinedTextField(
                        value = nextDoseDays,
                        onValueChange = { nextDoseDays = it },
                        label = { Text("Días hasta próxima dosis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val nextDoseDate = if (hasNextDose) {
                        System.currentTimeMillis() + (nextDoseDays.toIntOrNull() ?: 30) * 24 * 60 * 60 * 1000L
                    } else null

                    viewModel.addVaccinationRecord(
                        petId = petId,
                        vaccineId = "", // Temporal
                        vaccineName = vaccineName,
                        batch = batch,
                        expirationDate = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L), // 1 año
                        veterinarianId = "", // Se obtendría del usuario actual
                        nextDoseDate = nextDoseDate
                    )
                    onVaccineAdded()
                },
                enabled = vaccineName.isNotBlank() && batch.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

private fun calculateAge(birthDate: Long): Int {
    val birth = Calendar.getInstance().apply { timeInMillis = birthDate }
    val now = Calendar.getInstance()
    var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return age
}