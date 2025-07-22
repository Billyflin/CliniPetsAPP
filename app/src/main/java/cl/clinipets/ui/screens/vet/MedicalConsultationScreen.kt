// ui/screens/vet/MedicalConsultationScreen.kt
package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.ConsultationViewModel
import cl.clinipets.ui.viewmodels.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalConsultationScreen(
    appointmentId: String,
    onConsultationFinished: () -> Unit,
    onNavigateBack: () -> Unit,
    consultationViewModel: ConsultationViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val consultationState by consultationViewModel.consultationState.collectAsState()
    val inventoryState by inventoryViewModel.inventoryState.collectAsState()

    var weight by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appointmentId) {
        consultationViewModel.startConsultation(appointmentId)
    }

    LaunchedEffect(consultationState.isConsultationFinished) {
        if (consultationState.isConsultationFinished) {
            consultationViewModel.clearState()
            onConsultationFinished()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consulta Médica") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (consultationState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info de la mascota
                item {
                    consultationState.currentPet?.let { pet ->
                        Card(Modifier.fillMaxWidth()) {

                            Column(Modifier.padding(16.dp)) {
                                Text("Mascota: ${pet.name}", fontWeight = FontWeight.Bold)
                                Text("${pet.species} - ${pet.breed}")
                                Text("Peso actual: ${pet.weight} kg")
                            }
                        }
                    }
                }

                // Datos clínicos
                item {
                    Text("Datos clínicos", style = MaterialTheme.typography.titleMedium)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Peso (kg)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("Temp (°C)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
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
                        minLines = 2
                    )
                }

                // Servicios y medicamentos
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Servicios aplicados", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAddServiceDialog = true }) {
                            Text("Agregar")
                        }
                    }
                }

                consultationState.activeConsultation?.services?.forEach { service ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(service.name)
                                Text("$${service.price}")
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Medicamentos", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAddMedicationDialog = true }) {
                            Text("Agregar")
                        }
                    }
                }

                consultationState.activeConsultation?.medications?.forEach { medication ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(medication.name)
                                    Text(
                                        medication.dose,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text("$${medication.price}")
                            }
                        }
                    }
                }

                // Total
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold)
                            Text(
                                "$${consultationState.activeConsultation?.total ?: 0.0}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Botón finalizar
                item {
                    Button(
                        onClick = {
                            consultationViewModel.updateClinicalData(
                                weight.toFloatOrNull(),
                                temperature.toFloatOrNull(),
                                symptoms, diagnosis, treatment, ""
                            )
                            consultationViewModel.finishConsultation(true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finalizar Consulta")
                    }
                }
            }
        }

        // Diálogos
        if (showAddServiceDialog) {
            AddServiceDialog(
                onServiceAdded = { name, price ->
                    consultationViewModel.addService(name, price)
                    showAddServiceDialog = false
                },
                onDismiss = { showAddServiceDialog = false }
            )
        }

        if (showAddMedicationDialog) {
            AddMedicationDialog(
                medications = inventoryState.medications,
                onMedicationAdded = { medication, dose ->
                    consultationViewModel.addMedication(
                        medication.id,
                        medication.name,
                        dose,
                        medication.unitPrice
                    )
                    showAddMedicationDialog = false
                },
                onDismiss = { showAddMedicationDialog = false }
            )
        }
    }
}

@Composable
fun AddServiceDialog(
    onServiceAdded: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var serviceName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Servicio") },
        text = {
            Column {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Servicio") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    price.toDoubleOrNull()?.let { p ->
                        onServiceAdded(serviceName, p)
                    }
                },
                enabled = serviceName.isNotBlank() && price.toDoubleOrNull() != null
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}