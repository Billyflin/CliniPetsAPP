// ui/screens/appointments/AppointmentsScreen.kt
package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.*
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import java.text.SimpleDateFormat
import java.util.*

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
                Text(
                    text = appointment.serviceType.name,
                    style = MaterialTheme.typography.bodyMedium
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
        AppointmentStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary to Icons.Default.PlayArrow
        AppointmentStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant to Icons.Default.Done
        AppointmentStatus.CANCELLED -> MaterialTheme.colorScheme.error to Icons.Default.Cancel
        AppointmentStatus.NO_SHOW -> MaterialTheme.colorScheme.error to Icons.Default.PersonOff
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
        AppointmentStatus.IN_PROGRESS -> "En curso"
        AppointmentStatus.COMPLETED -> "Completada"
        AppointmentStatus.CANCELLED -> "Cancelada"
        AppointmentStatus.NO_SHOW -> "No asistió"
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
    var currentStep by remember { mutableStateOf(if (petId != null) 1 else 0) }

    LaunchedEffect(appointmentsState.isAppointmentCreated) {
        if (appointmentsState.isAppointmentCreated) {
            viewModel.clearState()
            onAppointmentCreated()
        }
    }

    LaunchedEffect(selectedVetId, selectedDate) {
        if (selectedVetId.isNotBlank() && selectedDate.isNotBlank()) {
            viewModel.loadVetAvailability(selectedVetId, selectedDate)
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
                        // Paso 2: Seleccionar veterinario
                        item {
                            Text(
                                text = "Paso 2: Selecciona el veterinario",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        items(appointmentsState.veterinarians) { vet ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedVetId = vet.id },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedVetId == vet.id)
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
                                        selected = selectedVetId == vet.id,
                                        onClick = { selectedVetId = vet.id }
                                    )
                                    Column {
                                        Text(
                                            text = vet.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = vet.email ?: "",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Paso 3: Seleccionar servicio
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
                    3 -> {
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
                                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                                    }
                                }
                            )
                        }
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
                                    val selectedPet = appointmentsState.userPets.find { it.id == selectedPetId }
                                    val selectedVet = appointmentsState.veterinarians.find { it.id == selectedVetId }

                                    InfoRow("Mascota", selectedPet?.name ?: "")
                                    InfoRow("Veterinario", selectedVet?.name ?: "")
                                    InfoRow("Servicio", selectedServiceType?.let { getServiceCategoryName(it) } ?: "")
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
                                        veterinarianId = selectedVetId,
                                        serviceType = service,
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
                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
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
        ServiceCategory.LABORATORY -> "Exámenes de laboratorio"
        ServiceCategory.RADIOLOGY -> "Radiografías"
        ServiceCategory.ULTRASOUND -> "Ecografías"
        ServiceCategory.GROOMING -> "Peluquería"
        ServiceCategory.HOSPITALIZATION -> "Hospitalización"
        ServiceCategory.EMERGENCY -> "Emergencia"
        ServiceCategory.DEWORMING -> "Desparasitación"
        ServiceCategory.DENTAL -> "Dental"
        ServiceCategory.OTHER -> "Otro"
    }
}

@Composable
private fun DatePickerDialog(
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