// ui/screens/MedicalConsultationScreen.kt
package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.MedicalConsultationViewModel

@Composable
fun MedicalConsultationScreen(
    appointmentId: String,
    onConsultationCompleted: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MedicalConsultationViewModel = hiltViewModel()
) {
    val consultationState by viewModel.consultationState.collectAsState()

    var symptoms by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var nextCheckup by remember { mutableStateOf("") }

    // Para agregar medicamentos
    var showMedicationDialog by remember { mutableStateOf(false) }
    var searchMedication by remember { mutableStateOf("") }

    LaunchedEffect(appointmentId) {
        viewModel.loadAppointmentData(appointmentId)
        viewModel.loadMedications()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Consulta Médica", style = MaterialTheme.typography.headlineMedium)

            // Información del paciente
            consultationState.appointment?.let { appointment ->
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Paciente: ${appointment.petName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Servicio: ${appointment.serviceType}")
                        Text("Fecha: ${appointment.date} ${appointment.time}")
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = symptoms,
                onValueChange = { symptoms = it },
                label = { Text("Síntomas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        item {
            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("Diagnóstico") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        item {
            OutlinedTextField(
                value = treatment,
                onValueChange = { treatment = it },
                label = { Text("Tratamiento") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        // Sección de medicamentos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Medicamentos aplicados", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showMedicationDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar medicamento")
                }
            }
        }

        items(consultationState.appliedMedications) { medication ->
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(medication.medicationName, style = MaterialTheme.typography.titleSmall)
                        Text("Dosis: ${medication.dose}")
                        Text("Vía: ${medication.route}")
                        if (medication.frequency.isNotBlank()) {
                            Text("Frecuencia: ${medication.frequency}")
                        }
                        if (medication.duration.isNotBlank()) {
                            Text("Duración: ${medication.duration}")
                        }
                    }
                    IconButton(
                        onClick = { viewModel.removeMedication(medication) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = observations,
                onValueChange = { observations = it },
                label = { Text("Observaciones") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        item {
            OutlinedTextField(
                value = nextCheckup,
                onValueChange = { nextCheckup = it },
                label = { Text("Próximo control (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveConsultation(
                            appointmentId = appointmentId,
                            symptoms = symptoms,
                            diagnosis = diagnosis,
                            treatment = treatment,
                            observations = observations,
                            nextCheckup = nextCheckup.ifBlank { null }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !consultationState.isLoading &&
                            symptoms.isNotBlank() &&
                            diagnosis.isNotBlank() &&
                            treatment.isNotBlank()
                ) {
                    if (consultationState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Finalizar consulta")
                    }
                }

                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
            }
        }

        item {
            consultationState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    // Diálogo para agregar medicamentos
    if (showMedicationDialog) {
        MedicationDialog(
            medications = consultationState.availableMedications,
            onDismiss = { showMedicationDialog = false },
            onMedicationSelected = { medication ->
                showMedicationDialog = false
                // Aquí podrías abrir otro diálogo para los detalles de dosis
                viewModel.addMedication(medication)
            }
        )
    }

    // Navegar cuando se complete
    LaunchedEffect(consultationState.isConsultationSaved) {
        if (consultationState.isConsultationSaved) {
            onConsultationCompleted()
        }
    }
}

@Composable
fun MedicationDialog(
    medications: List<Medication>,
    onDismiss: () -> Unit,
    onMedicationSelected: (Medication) -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar medicamento") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Buscar medicamento") },
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filteredMedications = medications.filter {
                        it.name.contains(searchText, ignoreCase = true) ||
                                it.activeIngredient.contains(searchText, ignoreCase = true)
                    }

                    items(filteredMedications) { medication ->
                        Card(
                            onClick = { onMedicationSelected(medication) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(medication.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${medication.activeIngredient} - ${medication.presentation}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Tipo Medication para evitar conflictos
data class Medication(
    val id: String = "",
    val name: String = "",
    val activeIngredient: String = "",
    val presentation: String = ""
)