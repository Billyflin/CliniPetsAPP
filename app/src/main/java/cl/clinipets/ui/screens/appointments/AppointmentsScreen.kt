// ui/screens/AppointmentsScreen.kt
package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppointmentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAppointment: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val appointmentsState by viewModel.appointmentsState.collectAsState()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Column {
        Text("Citas")
        Button(onClick = onNavigateToAddAppointment) { Text("Nueva Cita") }

        appointmentsState.appointments.forEach { appointment ->
            Card {
                Text("${appointment.petName} - ${appointment.serviceType}")
                Text(dateFormat.format(Date(appointment.dateTime)))
                Text("Estado: ${appointment.status}")
                if (appointment.status == "SCHEDULED") {
                    TextButton(onClick = { viewModel.cancelAppointment(appointment.id) }) { Text("Cancelar") }
                }
            }
        }

        Button(onClick = onNavigateBack) { Text("Volver") }
    }
}