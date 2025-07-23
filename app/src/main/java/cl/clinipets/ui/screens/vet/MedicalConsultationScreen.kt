// package cl.clinipets.ui.screens.vet

import android.util.Log
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.Service
import cl.clinipets.data.model.ServiceCategory
import cl.clinipets.data.model.Vaccine
import cl.clinipets.ui.viewmodels.ConsultationViewModel
import cl.clinipets.ui.viewmodels.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalConsultationScreen(
    appointmentId: String,
    onConsultationFinished: () -> Unit,
    onNavigateBack: () -> Unit,
    consultationViewModel: ConsultationViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val consultationState by consultationViewModel.consultationState.collectAsStateWithLifecycle()
    val inventoryState by inventoryViewModel.inventoryState.collectAsStateWithLifecycle()

    var weight by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showAddVaccineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appointmentId) {
        consultationViewModel.startConsultation(appointmentId)
        inventoryViewModel.loadInventory()
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                                Text("Peso registrado: ${pet.weight} kg")
                                Text("Dueño ID: ${pet.ownerId}")
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

                // Servicios aplicados
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

                // Medicamentos
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
                Log.d(
                    "MedicalConsultationScreen",
                    "Medicamentos: ${consultationState.activeConsultation?.medications}"
                )
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

                // Vacunas
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vacunas", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAddVaccineDialog = true }) {
                            Text("Agregar")
                        }
                    }
                }

                consultationState.activeConsultation?.vaccines?.forEach { vaccine ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(vaccine.name)
                                    vaccine.nextDoseDate?.let { nextDate ->
                                        Text(
                                            "Próxima: ${
                                                SimpleDateFormat(
                                                    "dd/MM/yyyy",
                                                    Locale.getDefault()
                                                ).format(Date(nextDate))
                                            }",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Text("$${vaccine.price}")
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
                services = inventoryState.services,
                onServiceAdded = { service ->
                    consultationViewModel.addService(service.id, service.name, service.basePrice)
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

        if (showAddVaccineDialog) {
            AddVaccineDialog(
                vaccines = inventoryState.vaccines,
                onVaccineAdded = { vaccine ->
                    consultationViewModel.addVaccine(
                        vaccine.id,
                        vaccine.name,
                        vaccine.unitPrice,
                        null
                    )
                    showAddVaccineDialog = false
                },
                onDismiss = { showAddVaccineDialog = false }
            )
        }
    }
}

@Composable
fun AddServiceDialog(
    services: List<Service>,
    onServiceAdded: (Service) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedService by remember { mutableStateOf<Service?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Servicio") },
        text = {
            Column {
                Text("Selecciona un servicio")
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Log.d("AddServiceDialog", "Services: $services")
                    services.forEach { service ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedService = service }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedService == service,
                                onClick = { selectedService = service }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row {
                                    Text(
                                        text = when (service.category) {
                                            ServiceCategory.CONSULTATION -> "Consulta"
                                            ServiceCategory.VACCINATION -> "Vacunación"
                                            ServiceCategory.SURGERY -> "Cirugía"
                                            ServiceCategory.GROOMING -> "Peluquería"
                                            ServiceCategory.OTHER -> "Otro"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = " - $${service.basePrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedService?.let { service ->
                        onServiceAdded(service)
                    }
                },
                enabled = selectedService != null
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

@Composable
fun AddVaccineDialog(
    vaccines: List<Vaccine>,
    onVaccineAdded: (Vaccine) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVaccine by remember { mutableStateOf<Vaccine?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Vacuna") },
        text = {
            Column {
                Text("Selecciona una vacuna")
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    vaccines.forEach { vaccine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVaccine = vaccine }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVaccine == vaccine,
                                onClick = { selectedVaccine = vaccine }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = vaccine.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Stock: ${vaccine.stock} - $${vaccine.unitPrice}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedVaccine?.let { vaccine ->
                        onVaccineAdded(vaccine)
                    }
                },
                enabled = selectedVaccine != null
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

@Composable
fun AddMedicationDialog(
    medications: List<Medication>,
    onMedicationAdded: (Medication, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMedication by remember { mutableStateOf<Medication?>(null) }
    var dose by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Medicamento") },
        text = {
            Column {
                Text("Selecciona un medicamento")
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    medications.forEach { medication ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMedication = medication }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMedication == medication,
                                onClick = { selectedMedication = medication }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = medication.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${medication.presentation} - $${medication.unitPrice}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("Dosis") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: 1 tableta cada 8 horas") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedMedication?.let { med ->
                        onMedicationAdded(med, dose)
                    }
                },
                enabled = selectedMedication != null && dose.isNotBlank()
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