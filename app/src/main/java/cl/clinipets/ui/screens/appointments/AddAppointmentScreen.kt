// ui/screens/appointments/AddAppointmentScreen.kt
package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.AppointmentsViewModel

@Composable
fun AddAppointmentScreen(
    petId: String? = null, // Opcional, si viene desde mascota
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

    val appointmentsState by viewModel.appointmentsState.collectAsState()

    LaunchedEffect(appointmentsState.isAppointmentAdded) {
        if (appointmentsState.isAppointmentAdded) {
            viewModel.clearState()
            onAppointmentAdded()
        }
    }

    Column {
        Text("Nueva Cita")

        // Selector de mascota (si no viene preseleccionada)
        if (petId == null) {
            Text("Selecciona mascota:")
            appointmentsState.availablePets.forEach { pet ->
                TextButton(onClick = { selectedPetId = pet.id }) {
                    Text(if (selectedPetId == pet.id) "[X] ${pet.name}" else "[ ] ${pet.name}")
                }
            }
        } else {
            val pet = appointmentsState.availablePets.find { it.id == petId }
            Text("Mascota: ${pet?.name ?: "No encontrada"}")
        }

        // Selector de veterinario
        Text("Selecciona veterinaria:")
        appointmentsState.availableVets.forEach { vet ->
            TextButton(onClick = {
                selectedVetId = vet.id
                // Cargar horarios disponibles cuando se selecciona veterinario y fecha
                if (date.isNotBlank()) {
                    viewModel.loadVetAvailability(vet.id, date)
                }
            }) {
                Text(if (selectedVetId == vet.id) "[X] Dra. ${vet.name}" else "[ ] Dra. ${vet.name}")
            }
        }

        // Selector de servicio
        Text("Tipo de servicio:")
        listOf("Consulta", "Vacunación", "Emergencia", "Control", "Cirugía").forEach { service ->
            TextButton(onClick = { selectedService = service }) {
                Text(if (selectedService == service) "[X] $service" else "[ ] $service")
            }
        }

        OutlinedTextField(
            value = date,
            onValueChange = {
                date = it
                // Si hay veterinario seleccionado, cargar horarios
                if (selectedVetId.isNotBlank() && it.length == 10) { // DD/MM/YYYY
                    viewModel.loadVetAvailability(selectedVetId, it)
                }
            },
            label = { Text("Fecha DD/MM/AAAA") }
        )

        // Mostrar slots disponibles si hay veterinario y fecha
        if (selectedVetId.isNotBlank() && date.isNotBlank()) {
            if (appointmentsState.isLoadingSlots) {
                CircularProgressIndicator()
            } else {
                Text("Horarios disponibles:")
                appointmentsState.availableTimeSlots.forEach { slot ->
                    TextButton(
                        onClick = { selectedTimeSlot = slot.time },
                        enabled = slot.isAvailable
                    ) {
                        Text(
                            if (!slot.isAvailable) "[OCUPADO] ${slot.time}"
                            else if (selectedTimeSlot == slot.time) "[X] ${slot.time}"
                            else "[ ] ${slot.time}"
                        )
                    }
                }
            }
        }

        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") })

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
            enabled = !appointmentsState.isLoading
        ) {
            if (appointmentsState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Agendar")
            }
        }

        OutlinedButton(onClick = onNavigateBack) { Text("Cancelar") }

        appointmentsState.error?.let { Text(it) }
    }
}