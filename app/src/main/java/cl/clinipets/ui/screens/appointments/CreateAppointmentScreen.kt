package cl.clinipets.ui.screens.appointments

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.ServiceCategory
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAppointmentScreen(
    petId: String? = null,
    onAppointmentCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val appointmentsState by viewModel.appointmentsState.collectAsState()
    var selectedPetId by remember { mutableStateOf(petId ?: "") }
    var selectedVetId by remember { mutableStateOf("") }
    var selectedServiceType by remember { mutableStateOf<ServiceCategory?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(if (petId != null) 1 else 0) }

    LaunchedEffect(appointmentsState.isAppointmentCreated) {
        if (appointmentsState.isAppointmentCreated) {
            viewModel.clearState()
            onAppointmentCreated()
        }
    }

    LaunchedEffect(selectedDate) {
        if (selectedDate.isNotBlank()) {
            viewModel.loadAvailableTimeSlots(selectedDate)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva cita") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentStep + 1) / 5f,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    0 -> {
                        // Paso 1: Seleccionar mascota
                        item {
                            Text(
                                text = "Paso 1: Selecciona la mascota",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        items(appointmentsState.userPets) { pet ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPetId = pet.id },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedPetId == pet.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedPetId == pet.id,
                                        onClick = { selectedPetId = pet.id }
                                    )
                                    Column {
                                        Text(
                                            text = pet.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${pet.species.name} - ${pet.breed}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Paso 2: Seleccionar servicio
                        item {
                            Text(
                                text = "Paso 3: Selecciona el tipo de servicio",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ServiceCategory.values().forEach { category ->
                                    FilterChip(
                                        selected = selectedServiceType == category,
                                        onClick = { selectedServiceType = category },
                                        label = { Text(getServiceCategoryName(category)) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = { Text("Motivo de la consulta") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }

                    2 -> {
                        // Paso 4: Seleccionar fecha y hora
                        item {
                            Text(
                                text = "Paso 4: Selecciona fecha y hora",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = selectedDate,
                                onValueChange = { },
                                label = { Text("Fecha") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = "Seleccionar fecha"
                                        )
                                    }
                                }
                            )
                        }
                        Log.d("CreateAppointmentScreen", "selectedDate: $selectedDate")
                        Log.d(
                            "CreateAppointmentScreen",
                            "availableTimeSlots: ${appointmentsState.availableTimeSlots}"
                        )

                        if (appointmentsState.isLoadingSlots) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (appointmentsState.availableTimeSlots.isNotEmpty()) {
                            Log.d(
                                "CreateAppointmentScreen",
                                "availableTimeSlots: ${appointmentsState.availableTimeSlots}"
                            )
                            item {
                                Text(
                                    text = "Horarios disponibles",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            item {
                                val chunkedSlots = appointmentsState.availableTimeSlots.chunked(4)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chunkedSlots.forEach { rowSlots ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowSlots.forEach { slot ->
                                                FilterChip(
                                                    selected = selectedTime == slot.time,
                                                    onClick = { selectedTime = slot.time },
                                                    label = { Text(slot.time) },
                                                    enabled = slot.isAvailable,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            // Rellenar espacios vacíos
                                            repeat(4 - rowSlots.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // Paso 5: Confirmación
                        item {
                            Text(
                                text = "Paso 5: Confirma tu cita",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        item {
                            Card {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val selectedPet =
                                        appointmentsState.userPets.find { it.id == selectedPetId }

                                    InfoRow("Mascota", selectedPet?.name ?: "")
                                    InfoRow(
                                        "Servicio",
                                        selectedServiceType?.let { getServiceCategoryName(it) }
                                            ?: "")
                                    InfoRow("Fecha", selectedDate)
                                    InfoRow("Hora", selectedTime)
                                    if (reason.isNotBlank()) {
                                        Text(
                                            text = "Motivo:",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = reason,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Botones de navegación
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Anterior")
                    }
                }

                Button(
                    onClick = {
                        when (currentStep) {
                            4 -> {
                                // Crear cita
                                selectedServiceType?.let { service ->
                                    viewModel.createAppointment(
                                        petId = selectedPetId,
                                        date = selectedDate,
                                        time = selectedTime,
                                        reason = reason
                                    )
                                }
                            }

                            else -> currentStep++
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = when (currentStep) {
                        0 -> selectedPetId.isNotBlank()
                        1 -> selectedVetId.isNotBlank()
                        2 -> selectedServiceType != null
                        3 -> selectedDate.isNotBlank() && selectedTime.isNotBlank()
                        4 -> !appointmentsState.isLoading
                        else -> false
                    }
                ) {
                    if (currentStep == 4 && appointmentsState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (currentStep == 4) "Confirmar" else "Siguiente")
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDateSelected = { date ->
                    selectedDate =
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        appointmentsState.error?.let { error ->
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

private fun getServiceCategoryName(category: ServiceCategory): String {
    return when (category) {
        ServiceCategory.CONSULTATION -> "Consulta general"
        ServiceCategory.VACCINATION -> "Vacunación"
        ServiceCategory.SURGERY -> "Cirugía"
        ServiceCategory.GROOMING -> "Peluquería"
        ServiceCategory.OTHER -> "Otro"
    }
}