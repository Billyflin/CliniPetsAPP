package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import cl.clinipets.ui.viewmodels.AppointmentsViewModel

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
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Navegar cuando se crea la cita
    LaunchedEffect(appointmentsState.isAppointmentCreated) {
        if (appointmentsState.isAppointmentCreated) {
            viewModel.clearState()
            onAppointmentCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Cita") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PASO 1: Seleccionar mascota
            Text("1. Selecciona la mascota", style = MaterialTheme.typography.titleMedium)

            if (petId != null) {
                // Mascota preseleccionada
                val pet = appointmentsState.userPets.find { it.id == petId }
                pet?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${it.name} - ${it.species}")
                        }
                    }
                }
            } else {
                // Lista de mascotas para seleccionar
                appointmentsState.userPets.forEach { pet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPetId = pet.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPetId == pet.id,
                            onClick = { selectedPetId = pet.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${pet.name} - ${pet.species}")
                    }
                }
            }

            Divider()

            // PASO 2: Seleccionar fecha
            Text("2. Selecciona la fecha", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedDate.isEmpty()) {
                        "Seleccionar fecha"
                    } else {
                        val day = appointmentsState.availableDays.find { it.date == selectedDate }
                        if (day != null) {
                            "${day.dayName} ${day.displayDate}"
                        } else {
                            selectedDate
                        }
                    }
                )
            }

            // PASO 3: Seleccionar hora (solo si hay fecha seleccionada)
            if (selectedDate.isNotEmpty()) {
                Divider()
                Text("3. Selecciona la hora", style = MaterialTheme.typography.titleMedium)

                if (appointmentsState.isLoadingSlots) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (appointmentsState.availableTimeSlots.isEmpty()) {
                    Text(
                        "No hay horarios disponibles para esta fecha",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // Mostrar horarios en filas
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        appointmentsState.availableTimeSlots.chunked(4).forEach { rowSlots ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowSlots.forEach { slot ->
                                    FilterChip(
                                        selected = selectedTime == slot.time,
                                        onClick = {
                                            if (slot.isAvailable) {
                                                selectedTime = slot.time
                                            }
                                        },
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

            // PASO 4: Motivo (solo si hay fecha y hora)
            if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
                Divider()
                Text("4. Describe el motivo", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo de la consulta") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            // Resumen
            if (selectedPetId.isNotEmpty() && selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
                Divider()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumen de la cita", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        val pet = appointmentsState.userPets.find { it.id == selectedPetId }
                        pet?.let {
                            Text("Mascota: ${it.name}")
                        }

                        val day = appointmentsState.availableDays.find { it.date == selectedDate }
                        Text("Fecha: ${day?.dayName ?: ""} ${day?.displayDate ?: selectedDate}")
                        Text("Hora: $selectedTime")

                        if (reason.isNotEmpty()) {
                            Text("Motivo: $reason")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón crear
            Button(
                onClick = {
                    viewModel.createAppointment(selectedPetId, selectedDate, selectedTime, reason)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPetId.isNotBlank() &&
                        selectedDate.isNotBlank() &&
                        selectedTime.isNotBlank() &&
                        reason.isNotBlank() &&
                        !appointmentsState.isLoading
            ) {
                if (appointmentsState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Confirmar Cita")
                }
            }

            // Mostrar errores
            appointmentsState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Diálogo para seleccionar fecha
        if (showDatePicker) {
            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Seleccionar fecha") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (appointmentsState.availableDays.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No hay días disponibles configurados")
                            }
                        } else {
                            appointmentsState.availableDays.take(14).forEach { day ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDate = day.date
                                            selectedTime = "" // Reset hora
                                            showDatePicker = false
                                            // Cargar horarios para esta fecha
                                            viewModel.loadAvailableTimeSlots(day.date)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                day.dayName,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(day.displayDate)
                                        }
                                        Text(
                                            "Horario: ${day.startTime} - ${day.endTime}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}