// ui/screens/appointments/AppointmentsScreen.kt
package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAppointment: () -> Unit,
    onNavigateToAppointmentDetail: (String) -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val appointmentsState by viewModel.appointmentsState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Citas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddAppointment) {
                Icon(Icons.Default.Add, contentDescription = "Nueva cita")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Próximas") },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pasadas") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            if (appointmentsState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> UpcomingAppointmentsTab(
                        appointments = appointmentsState.upcomingAppointments,
                        onAppointmentClick = onNavigateToAppointmentDetail,
                        onCancelAppointment = { appointmentId ->
                            viewModel.cancelAppointment(appointmentId)
                        }
                    )
                    1 -> PastAppointmentsTab(
                        appointments = appointmentsState.pastAppointments,
                        onAppointmentClick = onNavigateToAppointmentDetail
                    )
                }
            }
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
private fun UpcomingAppointmentsTab(
    appointments: List<Appointment>,
    onAppointmentClick: (String) -> Unit,
    onCancelAppointment: (String) -> Unit
) {
    if (appointments.isEmpty()) {
        EmptyAppointmentsState(
            title = "No tienes citas próximas",
            message = "Agenda una cita para el cuidado de tu mascota"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(appointments) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    showActions = true,
                    onClick = { onAppointmentClick(appointment.id) },
                    onCancel = { onCancelAppointment(appointment.id) }
                )
            }
        }
    }
}

@Composable
private fun PastAppointmentsTab(
    appointments: List<Appointment>,
    onAppointmentClick: (String) -> Unit
) {
    if (appointments.isEmpty()) {
        EmptyAppointmentsState(
            title = "No hay citas pasadas",
            message = "Aquí aparecerán las citas completadas"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(appointments) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    showActions = false,
                    onClick = { onAppointmentClick(appointment.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentCard(
    appointment: Appointment,
    showActions: Boolean,
    onClick: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appointment.time,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                AppointmentStatusChip(appointment.status)
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Mascota: ${appointment.petId}", // TODO: Cargar nombre real
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (appointment.reason.isNotBlank()) {
                Text(
                    text = "Motivo: ${appointment.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showActions && appointment.status == AppointmentStatus.SCHEDULED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showCancelDialog = true }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar cita") },
            text = { Text("¿Estás seguro de que quieres cancelar esta cita?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancel?.invoke()
                        showCancelDialog = false
                    }
                ) {
                    Text("Cancelar cita", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Mantener cita")
                }
            }
        )
    }
}

@Composable
private fun AppointmentStatusChip(status: AppointmentStatus) {
    val (color, icon) = when (status) {
        AppointmentStatus.SCHEDULED -> MaterialTheme.colorScheme.primary to Icons.Default.Schedule
        AppointmentStatus.CONFIRMED -> MaterialTheme.colorScheme.tertiary to Icons.Default.CheckCircle
        AppointmentStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant to Icons.Default.Done
        AppointmentStatus.CANCELLED -> MaterialTheme.colorScheme.error to Icons.Default.Cancel
    }

    AssistChip(
        onClick = { },
        label = { Text(getStatusText(status)) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color,
            leadingIconContentColor = color
        )
    )
}

private fun getStatusText(status: AppointmentStatus): String {
    return when (status) {
        AppointmentStatus.SCHEDULED -> "Agendada"
        AppointmentStatus.CONFIRMED -> "Confirmada"
        AppointmentStatus.COMPLETED -> "Completada"
        AppointmentStatus.CANCELLED -> "Cancelada"
    }
}

@Composable
private fun EmptyAppointmentsState(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ====================== CREAR CITA ======================



@Composable
internal fun DatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Implementación temporal simple
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar fecha") },
        text = {
            // En producción, usar DatePicker de Material3
            Text("Selecciona una fecha del calendario")
        },
        confirmButton = {
            TextButton(onClick = {
                // Por ahora, seleccionar mañana
                val tomorrow = Calendar.getInstance()
                tomorrow.add(Calendar.DAY_OF_YEAR, 1)
                onDateSelected(tomorrow.timeInMillis)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}