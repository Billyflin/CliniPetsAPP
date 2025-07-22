// ui/screens/vet/MedicalConsultationScreen.kt
package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import cl.clinipets.data.model.MedicalConsultation
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.PaymentMethod
import cl.clinipets.data.model.Vaccine
import cl.clinipets.data.model.VeterinaryService
import cl.clinipets.ui.viewmodels.ConsultationViewModel
import cl.clinipets.ui.viewmodels.InventoryViewModel
import cl.clinipets.ui.viewmodels.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalConsultationScreen(
    appointmentId: String,
    onConsultationFinished: () -> Unit,
    onNavigateBack: () -> Unit,
    consultationViewModel: ConsultationViewModel = hiltViewModel(),
    vetViewModel: VetViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val consultationState by consultationViewModel.consultationState.collectAsState()
    val vetState by vetViewModel.vetState.collectAsState()
    val inventoryState by inventoryViewModel.inventoryState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Iniciar consulta
    LaunchedEffect(appointmentId) {
        consultationViewModel.startConsultation(appointmentId)
        vetViewModel.loadServices()
        inventoryViewModel.loadInventory()
    }

    // Procesar movimientos de inventario al finalizar
    LaunchedEffect(consultationState.isConsultationFinished) {
        if (consultationState.isConsultationFinished && consultationState.pendingInventoryMovements.isNotEmpty()) {
            inventoryViewModel.processConsultationInventoryMovements(
                consultationId = consultationState.activeConsultation?.id ?: "",
                movements = consultationState.pendingInventoryMovements
            )
            consultationViewModel.clearState()
            onConsultationFinished()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Consulta médica")
                        consultationState.currentPet?.let { pet ->
                            Text(
                                "${pet.name} - ${pet.species.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
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
            // Tabs simplificados
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Clínico") },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Servicios") },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Medicamentos") },
                    icon = { Icon(Icons.Default.Medication, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Cobrar") },
                    icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> ClinicalDataTab(
                    consultation = consultationState.activeConsultation,
                    onSave = { weight, temp, hr, rr, symptoms, exam, diagnosis, treatment, obs, rec ->
                        consultationViewModel.updateClinicalData(
                            weight = weight,
                            temperature = temp,
                            heartRate = hr,
                            respiratoryRate = rr,
                            symptoms = symptoms,
                            clinicalExam = exam,
                            diagnosis = diagnosis,
                            treatment = treatment,
                            observations = obs,
                            recommendations = rec
                        )
                    }
                )

                1 -> ServicesTab(
                    consultation = consultationState.activeConsultation,
                    availableServices = vetState.services,
                    onAddService = { serviceId, price ->
                        consultationViewModel.addService(serviceId, price)
                    }
                )

                2 -> MedicationsTab(
                    consultation = consultationState.activeConsultation,
                    availableMedications = inventoryState.medications,
                    availableVaccines = inventoryState.vaccines,
                    onAddMedication = { medicationId, dose, frequency, duration, route, quantity ->
                        consultationViewModel.addMedication(
                            medicationId, dose, frequency, duration, route, quantity
                        )
                    },
                    onAddVaccine = { vaccineId, batch, expirationDate, nextDoseDate ->
                        consultationViewModel.addVaccine(
                            vaccineId, batch, expirationDate, nextDoseDate
                        )
                    }
                )

                3 -> BillingTab(
                    consultation = consultationState.activeConsultation,
                    onFinishConsultation = { discount, discountReason, paymentMethod, amountPaid ->
                        consultationViewModel.finishConsultation(
                            discount, discountReason, paymentMethod, amountPaid
                        )
                    }
                )
            }
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("¿Salir de la consulta?") },
                text = { Text("Los cambios no guardados se perderán") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            consultationViewModel.clearState()
                            onNavigateBack()
                        }
                    ) {
                        Text("Salir", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Continuar")
                    }
                }
            )
        }
    }
}

@Composable
private fun ClinicalDataTab(
    consultation: MedicalConsultation?,
    onSave: (Float?, Float?, Int?, Int?, String, String, String, String, String, String) -> Unit
) {
    var weight by remember { mutableStateOf(consultation?.weight?.toString() ?: "") }
    var temperature by remember { mutableStateOf(consultation?.temperature?.toString() ?: "") }
    var heartRate by remember { mutableStateOf(consultation?.heartRate?.toString() ?: "") }
    var respiratoryRate by remember {
        mutableStateOf(
            consultation?.respiratoryRate?.toString() ?: ""
        )
    }
    var symptoms by remember { mutableStateOf(consultation?.symptoms ?: "") }
    var clinicalExam by remember { mutableStateOf(consultation?.clinicalExam ?: "") }
    var diagnosis by remember { mutableStateOf(consultation?.diagnosis ?: "") }
    var treatment by remember { mutableStateOf(consultation?.treatment ?: "") }
    var observations by remember { mutableStateOf(consultation?.observations ?: "") }
    var recommendations by remember { mutableStateOf(consultation?.recommendations ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Signos vitales", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Peso (kg)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("Temp (°C)") },
                            modifier = Modifier.weight(1f)
                        )
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
                minLines = 2
            )
        }

        item {
            Button(
                onClick = {
                    onSave(
                        weight.toFloatOrNull(),
                        temperature.toFloatOrNull(),
                        heartRate.toIntOrNull(),
                        respiratoryRate.toIntOrNull(),
                        symptoms,
                        clinicalExam,
                        diagnosis,
                        treatment,
                        observations,
                        recommendations
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar datos clínicos")
            }
        }
    }
}

@Composable
private fun ServicesTab(
    consultation: MedicalConsultation?,
    availableServices: List<VeterinaryService>,
    onAddService: (String, Double) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Servicios aplicados", fontWeight = FontWeight.Bold)
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Agregar")
                }
            }
        }

        consultation?.services?.let { services ->
            items(services) { service ->
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(service.name, fontWeight = FontWeight.Medium)
                            Text(
                                service.category.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "$${service.price}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal servicios", fontWeight = FontWeight.Bold)
                    Text(
                        "$${consultation?.services?.sumOf { it.price } ?: 0.0}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ServiceSelectionDialog(
            services = availableServices,
            onServiceSelected = { service, customPrice ->
                onAddService(service.id, customPrice ?: service.basePrice)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun MedicationsTab(
    consultation: MedicalConsultation?,
    availableMedications: List<Medication>,
    availableVaccines: List<Vaccine>,
    onAddMedication: (String, String, String, String, String, Int) -> Unit,
    onAddVaccine: (String, String, Long, Long?) -> Unit
) {
    var showMedicationDialog by remember { mutableStateOf(false) }
    var showVaccineDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Medicamentos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Medicamentos", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { showMedicationDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Agregar")
                }
            }
        }

        consultation?.medications?.let { medications ->
            items(medications) { medication ->
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(medication.name, fontWeight = FontWeight.Medium)
                            Text("$${medication.totalPrice}")
                        }
                        Text(
                            "Dosis: ${medication.dose} - ${medication.frequency}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Cantidad: ${medication.quantity} unidades",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Vacunas
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vacunas aplicadas", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { showVaccineDialog = true }) {
                    Icon(Icons.Default.Vaccines, contentDescription = null)
                    Text("Agregar")
                }
            }
        }

        consultation?.vaccines?.let { vaccines ->
            items(vaccines) { vaccine ->
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(vaccine.name, fontWeight = FontWeight.Medium)
                            Text(
                                "Lote: ${vaccine.batch}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text("$${vaccine.price}")
                    }
                }
            }
        }

        // Total medicamentos y vacunas
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal medicamentos")
                        Text("$${consultation?.medications?.sumOf { it.totalPrice } ?: 0.0}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal vacunas")
                        Text("$${consultation?.vaccines?.sumOf { it.price } ?: 0.0}")
                    }
                }
            }
        }
    }

    if (showMedicationDialog) {
        MedicationSelectionDialog(
            medications = availableMedications,
            onMedicationAdded = onAddMedication,
            onDismiss = { showMedicationDialog = false }
        )
    }

    if (showVaccineDialog) {
        VaccineSelectionDialog(
            vaccines = availableVaccines,
            onVaccineAdded = onAddVaccine,
            onDismiss = { showVaccineDialog = false }
        )
    }
}

@Composable
private fun BillingTab(
    consultation: MedicalConsultation?,
    onFinishConsultation: (Double, String?, PaymentMethod, Double) -> Unit
) {
    var discount by remember { mutableStateOf("0") }
    var discountReason by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var amountPaid by remember { mutableStateOf("") }

    val subtotal = consultation?.subtotal ?: 0.0
    val discountAmount = discount.toDoubleOrNull() ?: 0.0
    val total = subtotal - discountAmount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Resumen de cobros
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Resumen de cobros", fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Servicios")
                        Text("$${consultation?.services?.sumOf { it.price } ?: 0.0}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Medicamentos")
                        Text("$${consultation?.medications?.sumOf { it.totalPrice } ?: 0.0}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vacunas")
                        Text("$${consultation?.vaccines?.sumOf { it.price } ?: 0.0}")
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontWeight = FontWeight.Medium)
                        Text("$$subtotal", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Descuento
        item {
            OutlinedTextField(
                value = discount,
                onValueChange = { discount = it },
                label = { Text("Descuento") },
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (discountAmount > 0) {
            item {
                OutlinedTextField(
                    value = discountReason,
                    onValueChange = { discountReason = it },
                    label = { Text("Motivo del descuento") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Total
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL A PAGAR", fontWeight = FontWeight.Bold)
                    Text("$$total", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        // Forma de pago
        item {
            Text("Forma de pago", fontWeight = FontWeight.Bold)
            Column {
                PaymentMethod.values().forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPaymentMethod = method }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentMethod == method,
                            onClick = { selectedPaymentMethod = method }
                        )
                        Text(getPaymentMethodName(method))
                    }
                }
            }
        }

        // Monto pagado
        item {
            OutlinedTextField(
                value = amountPaid,
                onValueChange = { amountPaid = it },
                label = { Text("Monto pagado") },
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth()
            )

            val paid = amountPaid.toDoubleOrNull() ?: 0.0
            when {
                paid > total -> Text(
                    "Vuelto: $${paid - total}",
                    color = MaterialTheme.colorScheme.primary
                )

                paid < total -> Text(
                    "Saldo pendiente: $${total - paid}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Finalizar
        item {
            Button(
                onClick = {
                    onFinishConsultation(
                        discountAmount,
                        discountReason.ifBlank { null },
                        selectedPaymentMethod,
                        amountPaid.toDoubleOrNull() ?: 0.0
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amountPaid.isNotBlank()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finalizar consulta y cobrar")
            }
        }
    }
}