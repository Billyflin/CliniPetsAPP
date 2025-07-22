package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import cl.clinipets.ui.viewmodels.VetViewModel

// ====================== CONSULTA MÉDICA ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalConsultationScreen(
    appointmentId: String,
    onConsultationFinished: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: VetViewModel = hiltViewModel(),
    appointmentsViewModel: AppointmentsViewModel = hiltViewModel()
) {
    val vetState by viewModel.vetState.collectAsState()
    val appointmentsState by appointmentsViewModel.appointmentsState.collectAsState()
    var currentTab by remember { mutableStateOf(0) }

    // Datos clínicos
    var weight by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    var respiratoryRate by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var clinicalExam by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var recommendations by remember { mutableStateOf("") }

    // Estado de la consulta
    var consultationStarted by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appointmentId) {
        if (!consultationStarted) {
            appointmentsViewModel.startConsultation(appointmentId)
            consultationStarted = true
        }
    }

    LaunchedEffect(appointmentsState.isConsultationFinished) {
        if (appointmentsState.isConsultationFinished) {
            appointmentsViewModel.clearState()
            onConsultationFinished()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consulta médica") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showFinishDialog = true },
                        enabled = appointmentsState.activeConsultationId != null
                    ) {
                        Text("Finalizar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = currentTab) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("Datos clínicos") }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("Servicios") }
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    text = { Text("Medicamentos") }
                )
                Tab(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    text = { Text("Facturación") }
                )
            }

            when (currentTab) {
                0 -> ClinicalDataTab(
                    weight = weight,
                    onWeightChange = { weight = it },
                    temperature = temperature,
                    onTemperatureChange = { temperature = it },
                    heartRate = heartRate,
                    onHeartRateChange = { heartRate = it },
                    respiratoryRate = respiratoryRate,
                    onRespiratoryRateChange = { respiratoryRate = it },
                    symptoms = symptoms,
                    onSymptomsChange = { symptoms = it },
                    clinicalExam = clinicalExam,
                    onClinicalExamChange = { clinicalExam = it },
                    diagnosis = diagnosis,
                    onDiagnosisChange = { diagnosis = it },
                    treatment = treatment,
                    onTreatmentChange = { treatment = it },
                    observations = observations,
                    onObservationsChange = { observations = it },
                    recommendations = recommendations,
                    onRecommendationsChange = { recommendations = it },
                    onSave = {
                        appointmentsState.activeConsultationId?.let { consultationId ->
                            appointmentsViewModel.updateConsultation(
                                consultationId = consultationId,
                                weight = weight.toFloatOrNull(),
                                temperature = temperature.toFloatOrNull(),
                                heartRate = heartRate.toIntOrNull(),
                                respiratoryRate = respiratoryRate.toIntOrNull(),
                                symptoms = symptoms,
                                clinicalExam = clinicalExam,
                                diagnosis = diagnosis,
                                treatment = treatment,
                                observations = observations,
                                recommendations = recommendations,
                                nextCheckupDays = null,
                                nextCheckupReason = null
                            )
                        }
                    }
                )

                1 -> ServicesTab(
                    consultation = appointmentsState.selectedConsultation,
                    availableServices = vetState.services,
                    onAddService = { serviceId, price ->
                        appointmentsState.activeConsultationId?.let { consultationId ->
                            appointmentsViewModel.addServiceToConsultation(
                                consultationId,
                                serviceId,
                                price
                            )
                        }
                    }
                )

                2 -> MedicationsTab(
                    consultation = appointmentsState.selectedConsultation,
                    availableMedications = vetState.medications,
                    onAddMedication = { medicationId, dose, frequency, duration, route, quantity, unitPrice ->
                        appointmentsState.activeConsultationId?.let { consultationId ->
                            appointmentsViewModel.addMedicationToConsultation(
                                consultationId,
                                medicationId,
                                dose,
                                frequency,
                                duration,
                                route,
                                quantity,
                                unitPrice
                            )
                        }
                    }
                )

                3 -> BillingTab(
                    consultation = appointmentsState.selectedConsultation,
                    onFinishConsultation = { discount, discountReason, paymentMethod, amountPaid ->
                        appointmentsState.activeConsultationId?.let { consultationId ->
                            appointmentsViewModel.finishConsultation(
                                consultationId, discount, discountReason, paymentMethod, amountPaid
                            )
                        }
                    }
                )
            }
        }

        if (showFinishDialog) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text("Finalizar consulta") },
                text = { Text("¿Estás seguro de finalizar la consulta? Asegúrate de haber guardado todos los datos y procesado el pago.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFinishDialog = false
                            currentTab = 3 // Ir a facturación
                        }
                    ) {
                        Text("Ir a facturación")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun ClinicalDataTab(
    weight: String,
    onWeightChange: (String) -> Unit,
    temperature: String,
    onTemperatureChange: (String) -> Unit,
    heartRate: String,
    onHeartRateChange: (String) -> Unit,
    respiratoryRate: String,
    onRespiratoryRateChange: (String) -> Unit,
    symptoms: String,
    onSymptomsChange: (String) -> Unit,
    clinicalExam: String,
    onClinicalExamChange: (String) -> Unit,
    diagnosis: String,
    onDiagnosisChange: (String) -> Unit,
    treatment: String,
    onTreatmentChange: (String) -> Unit,
    observations: String,
    onObservationsChange: (String) -> Unit,
    recommendations: String,
    onRecommendationsChange: (String) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Signos vitales
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Signos vitales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = onWeightChange,
                            label = { Text("Peso (kg)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = onTemperatureChange,
                            label = { Text("Temp (°C)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = heartRate,
                            onValueChange = onHeartRateChange,
                            label = { Text("FC (lpm)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = respiratoryRate,
                            onValueChange = onRespiratoryRateChange,
                            label = { Text("FR (rpm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Anamnesis
        item {
            OutlinedTextField(
                value = symptoms,
                onValueChange = onSymptomsChange,
                label = { Text("Síntomas / Motivo de consulta") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        // Examen clínico
        item {
            OutlinedTextField(
                value = clinicalExam,
                onValueChange = onClinicalExamChange,
                label = { Text("Examen físico") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        // Diagnóstico
        item {
            OutlinedTextField(
                value = diagnosis,
                onValueChange = onDiagnosisChange,
                label = { Text("Diagnóstico") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        // Tratamiento
        item {
            OutlinedTextField(
                value = treatment,
                onValueChange = onTreatmentChange,
                label = { Text("Plan de tratamiento") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        // Observaciones
        item {
            OutlinedTextField(
                value = observations,
                onValueChange = onObservationsChange,
                label = { Text("Observaciones") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        // Recomendaciones
        item {
            OutlinedTextField(
                value = recommendations,
                onValueChange = onRecommendationsChange,
                label = { Text("Recomendaciones para el propietario") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        // Botón guardar
        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar datos clínicos")
            }
        }
    }
}