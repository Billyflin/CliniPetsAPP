// ui/screens/vet/ConsultationDialogs.kt
package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.PaymentMethod
import cl.clinipets.data.model.ServiceCategory
import cl.clinipets.data.model.Vaccine
import cl.clinipets.data.model.VeterinaryService

@Composable
fun ServiceSelectionDialog(
    services: List<VeterinaryService>,
    onServiceSelected: (VeterinaryService, Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedService by remember { mutableStateOf<VeterinaryService?>(null) }
    var customPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar servicio") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(services.groupBy { it.category }
                        .toList()) { (category, categoryServices) ->
                        Text(
                            text = getCategoryName(category),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        categoryServices.forEach { service ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedService = service
                                        customPrice = service.basePrice.toString()
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedService == service,
                                    onClick = {
                                        selectedService = service
                                        customPrice = service.basePrice.toString()
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(service.name)
                                    Text(
                                        "Precio base: $${service.basePrice}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                selectedService?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customPrice,
                        onValueChange = { customPrice = it },
                        label = { Text("Precio a cobrar") },
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedService?.let { service ->
                        onServiceSelected(service, customPrice.toDoubleOrNull())
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
fun MedicationSelectionDialog(
    medications: List<Medication>,
    onMedicationAdded: (String, String, String, String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMedication by remember { mutableStateOf<Medication?>(null) }
    var dose by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("Oral") }
    var quantity by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar medicamento") },
        text = {
            Column {
                // Selección de medicamento
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(medications) { medication ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMedication = medication }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMedication == medication,
                                onClick = { selectedMedication = medication }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(medication.name)
                                Text(
                                    "${medication.presentation} - Stock: ${medication.stock}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                selectedMedication?.let { med ->
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = { Text("Dosis (ej: 2.5 ml)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frecuencia (ej: cada 8 horas)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duración (ej: 7 días)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedMedication?.let { med ->
                        onMedicationAdded(
                            med.id,
                            dose,
                            frequency,
                            duration,
                            route,
                            quantity.toIntOrNull() ?: 1
                        )
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

@Composable
fun VaccineSelectionDialog(
    vaccines: List<Vaccine>,
    onVaccineAdded: (String, String, Long, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVaccine by remember { mutableStateOf<Vaccine?>(null) }
    var batch by remember { mutableStateOf("") }
    var hasNextDose by remember { mutableStateOf(false) }
    var nextDoseDays by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar vacuna") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(vaccines) { vaccine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVaccine = vaccine }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVaccine == vaccine,
                                onClick = { selectedVaccine = vaccine }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vaccine.name)
                                Text(
                                    "Stock: ${vaccine.stock} - $${vaccine.unitPrice}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                selectedVaccine?.let {
                    Spacer(modifier = Modifier.height(8.dp))

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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedVaccine?.let { vaccine ->
                        val nextDoseDate = if (hasNextDose) {
                            System.currentTimeMillis() + (nextDoseDays.toLongOrNull()
                                ?: 30) * 24 * 60 * 60 * 1000
                        } else null

                        onVaccineAdded(
                            vaccine.id,
                            batch,
                            System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L, // 1 año expiration
                            nextDoseDate
                        )
                    }
                },
                enabled = selectedVaccine != null && batch.isNotBlank()
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun getCategoryName(category: ServiceCategory): String {
    return when (category) {
        ServiceCategory.CONSULTATION -> "Consulta"
        ServiceCategory.VACCINATION -> "Vacunación"
        ServiceCategory.SURGERY -> "Cirugía"
        ServiceCategory.LABORATORY -> "Laboratorio"
        ServiceCategory.RADIOLOGY -> "Radiología"
        ServiceCategory.ULTRASOUND -> "Ecografía"
        ServiceCategory.GROOMING -> "Peluquería"
        ServiceCategory.HOSPITALIZATION -> "Hospitalización"
        ServiceCategory.EMERGENCY -> "Emergencia"
        ServiceCategory.DEWORMING -> "Desparasitación"
        ServiceCategory.DENTAL -> "Dental"
        ServiceCategory.OTHER -> "Otro"
    }
}

internal fun getPaymentMethodName(method: PaymentMethod): String {
    return when (method) {
        PaymentMethod.CASH -> "Efectivo"
        PaymentMethod.DEBIT_CARD -> "Débito"
        PaymentMethod.CREDIT_CARD -> "Crédito"
        PaymentMethod.TRANSFER -> "Transferencia"
        PaymentMethod.CHECK -> "Cheque"
        PaymentMethod.OTHER -> "Otro"
    }
}