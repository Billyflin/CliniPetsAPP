// ui/screens/appointments/AddAppointmentScreen.kt
package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentScreen(
    petId: String? = null,
    onAppointmentAdded: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    var selectedPetId by remember { mutableStateOf(petId ?: "") }
    var selectedVetId by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var selectedTimeSlot by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val state by viewModel.appointmentsState.collectAsState()

    LaunchedEffect(state.isAppointmentAdded) {
        if (state.isAppointmentAdded) {
            viewModel.clearState()
            onAppointmentAdded()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Nueva Cita", style = MaterialTheme.typography.headlineSmall)

        if (petId == null) {
            Text("Mascota")
            ChipsRow(state.availablePets.map { it.id to it.name }, selectedPetId) {
                selectedPetId = it
            }
        } else {
            Text("Mascota: ${state.availablePets.find { it.id == petId }?.name ?: ""}")
        }

        Text("Veterinaria")
        ChipsRow(state.availableVets.map { it.id to "Dra. ${it.name}" }, selectedVetId) {
            selectedVetId = it
            if (date.isNotBlank()) viewModel.loadVetAvailability(it, date)
        }

        Text("Servicio")
        val services = listOf("Consulta", "Vacunación", "Emergencia", "Control", "Cirugía")
        ChipsRow(services.map { it to it }, selectedService) { selectedService = it }

        OutlinedTextField(
            value = date,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text("Fecha") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        )

        if (selectedVetId.isNotBlank() && date.isNotBlank()) {
            Text("Horarios")
            if (state.isLoadingSlots) {
                CircularProgressIndicator()
            } else {
                ChipsRow(
                    state.availableTimeSlots.map { it.time to it.time },
                    selectedTimeSlot,
                    enabledCondition = { time ->
                        state.availableTimeSlots.find { it.time == time }?.isAvailable == true
                    }
                ) { selectedTimeSlot = it }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text("Notas") }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    viewModel.addAppointment(
                        selectedPetId,
                        selectedVetId,
                        selectedService,
                        date,
                        selectedTimeSlot,
                        notes
                    )
                },
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator() else Text("Agendar")
            }
            OutlinedButton(onClick = onNavigateBack) { Text("Cancelar") }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            date =
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                            if (selectedVetId.isNotBlank()) viewModel.loadVetAvailability(
                                selectedVetId,
                                date
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun ChipsRow(
    options: List<Pair<String, String>>,
    selected: String,
    enabledCondition: (String) -> Boolean = { true },
    onSelect: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                enabled = enabledCondition(value),
                label = { Text(label) },
                leadingIcon = if (selected == value) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
