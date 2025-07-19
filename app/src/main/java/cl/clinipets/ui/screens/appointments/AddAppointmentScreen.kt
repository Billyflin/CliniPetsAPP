// ui/screens/AddAppointmentScreen.kt
package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
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
    onAppointmentAdded: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    var selectedPetId by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
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

        // Selector simple de mascota
        appointmentsState.availablePets.forEach { pet ->
            TextButton(onClick = { selectedPetId = pet.id }) {
                Text(if (selectedPetId == pet.id) "[X] ${pet.name}" else "[ ] ${pet.name}")
            }
        }

        // Selector simple de servicio
        listOf("Consulta", "Vacunación", "Emergencia").forEach { service ->
            TextButton(onClick = { selectedService = service }) {
                Text(if (selectedService == service) "[X] $service" else "[ ] $service")
            }
        }

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Fecha DD/MM/AAAA") })
        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Hora HH:MM") })
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") })

        Button(onClick = {
            viewModel.addAppointment(
                selectedPetId,
                selectedService,
                date,
                time,
                notes
            )
        }) {
            Text("Agendar")
        }
        OutlinedButton(onClick = onNavigateBack) { Text("Cancelar") }
        appointmentsState.error?.let { Text(it) }
    }
}