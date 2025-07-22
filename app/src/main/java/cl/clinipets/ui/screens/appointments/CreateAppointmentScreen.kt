package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
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
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seleccionar mascota
            if (petId == null) {
                Text("Selecciona mascota", style = MaterialTheme.typography.titleMedium)
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
                        Text("${pet.name} - ${pet.species}")
                    }
                }
            }

            // Fecha
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { },
                label = { Text("Fecha") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                }
            )

            // Horarios disponibles
            if (appointmentsState.availableTimeSlots.isNotEmpty()) {
                Text("Horarios disponibles", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(appointmentsState.availableTimeSlots) { slot ->
                        FilterChip(
                            selected = selectedTime == slot.time,
                            onClick = { selectedTime = slot.time },
                            label = { Text(slot.time) },
                            enabled = slot.isAvailable
                        )
                    }
                }
            }

            // Motivo
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Motivo de la consulta") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Botón crear
            Button(
                onClick = {
                    viewModel.createAppointment(selectedPetId, selectedDate, selectedTime, reason)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPetId.isNotBlank() &&
                        selectedDate.isNotBlank() &&
                        selectedTime.isNotBlank() &&
                        !appointmentsState.isLoading
            ) {
                if (appointmentsState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Crear Cita")
                }
            }
        }

        // Date picker simple
        if (showDatePicker) {
            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Seleccionar fecha") },
                confirmButton = {
                    TextButton(onClick = {
                        // Por simplicidad, usar fecha de mañana
                        val tomorrow = Calendar.getInstance()
                        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
                        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(tomorrow.time)
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

