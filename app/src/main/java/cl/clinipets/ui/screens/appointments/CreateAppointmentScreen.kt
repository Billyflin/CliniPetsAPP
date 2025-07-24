package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.ui.theme.ColorFamily
import cl.clinipets.ui.theme.LocalExtendedColors
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import cl.clinipets.ui.viewmodels.AvailableDay
import cl.clinipets.ui.viewmodels.TimeSlot

// package cl.clinipets.ui.screens.appointments

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAppointmentScreen(
    petId: String? = null,
    onAppointmentCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val appointmentsState by viewModel.appointmentsState.collectAsStateWithLifecycle()
    val extColors = LocalExtendedColors.current

    var selectedPetId by remember { mutableStateOf(petId ?: "") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(1) }

    // Navegar cuando se crea la cita
    LaunchedEffect(appointmentsState.isAppointmentCreated) {
        if (appointmentsState.isAppointmentCreated) {
            viewModel.clearState()
            onAppointmentCreated()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nueva Cita")
                        Text(
                            "Paso $currentStep de 4",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                progress = currentStep / 4f,
                modifier = Modifier.fillMaxWidth(),
                color = extColors.mint.color,
                trackColor = extColors.mint.colorContainer
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PASO 1: Seleccionar mascota
                item {
                    StepCard(
                        stepNumber = 1,
                        title = "Selecciona la mascota",
                        icon = Icons.Default.Pets,
                        color = extColors.pink,
                        isActive = currentStep >= 1
                    ) {
                        if (petId != null) {
                            // Mascota preseleccionada
                            val pet = appointmentsState.userPets.find { it.id == petId }
                            pet?.let {
                                SelectedPetCard(pet = it, color = extColors.pink)
                            }
                        } else {
                            // Lista de mascotas para seleccionar
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                appointmentsState.userPets.forEach { pet ->
                                    PetSelectionCard(
                                        pet = pet,
                                        isSelected = selectedPetId == pet.id,
                                        onClick = {
                                            selectedPetId = pet.id
                                            if (currentStep == 1) currentStep = 2
                                        },
                                        color = extColors.pink
                                    )
                                }
                            }
                        }
                    }
                }

                // PASO 2: Seleccionar fecha
                if (selectedPetId.isNotEmpty()) {
                    item {
                        StepCard(
                            stepNumber = 2,
                            title = "Selecciona la fecha",
                            icon = Icons.Default.DateRange,
                            color = extColors.peach,
                            isActive = currentStep >= 2
                        ) {
                            Surface(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedDate.isNotEmpty())
                                    extColors.peach.color
                                else
                                    extColors.peach.colorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = if (selectedDate.isNotEmpty())
                                                extColors.peach.onColor
                                            else
                                                extColors.peach.onColorContainer
                                        )
                                        Text(
                                            if (selectedDate.isEmpty()) {
                                                "Seleccionar fecha"
                                            } else {
                                                val day =
                                                    appointmentsState.availableDays.find { it.date == selectedDate }
                                                "${day?.dayName ?: ""} ${day?.displayDate ?: selectedDate}"
                                            },
                                            color = if (selectedDate.isNotEmpty())
                                                extColors.peach.onColor
                                            else
                                                extColors.peach.onColorContainer
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = if (selectedDate.isNotEmpty())
                                            extColors.peach.onColor
                                        else
                                            extColors.peach.onColorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // PASO 3: Seleccionar hora
                if (selectedDate.isNotEmpty()) {
                    item {
                        StepCard(
                            stepNumber = 3,
                            title = "Selecciona la hora",
                            icon = Icons.Default.AccessTime,
                            color = extColors.lavander,
                            isActive = currentStep >= 3
                        ) {
                            if (appointmentsState.isLoadingSlots) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = extColors.lavander.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else if (appointmentsState.availableTimeSlots.isEmpty()) {
                                EmptyTimeSlots(color = extColors.lavander)
                            } else {
                                TimeSlotGrid(
                                    timeSlots = appointmentsState.availableTimeSlots,
                                    selectedTime = selectedTime,
                                    onTimeSelected = {
                                        selectedTime = it
                                        if (currentStep == 3) currentStep = 4
                                    },
                                    color = extColors.lavander
                                )
                            }
                        }
                    }
                }

                // PASO 4: Motivo
                if (selectedTime.isNotEmpty()) {
                    item {
                        StepCard(
                            stepNumber = 4,
                            title = "Describe el motivo",
                            icon = Icons.Default.Description,
                            color = extColors.mint,
                            isActive = currentStep >= 4
                        ) {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = { Text("¿Por qué necesitas la consulta?") },
                                placeholder = { Text("Ej: Revisión general, vacunas, etc.") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = extColors.mint.color,
                                    focusedLabelColor = extColors.mint.color
                                )
                            )
                        }
                    }
                }

                // Resumen
                if (selectedPetId.isNotEmpty() && selectedDate.isNotEmpty() &&
                    selectedTime.isNotEmpty() && reason.isNotEmpty()
                ) {
                    item {
                        AppointmentSummaryCard(
                            pet = appointmentsState.userPets.find { it.id == selectedPetId },
                            date = selectedDate,
                            time = selectedTime,
                            reason = reason,
                            availableDays = appointmentsState.availableDays,
                            color = extColors.mint
                        )
                    }
                }

                // Espacio para el botón
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // Botón confirmar (sticky bottom)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.createAppointment(
                                selectedPetId,
                                selectedDate,
                                selectedTime,
                                reason
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedPetId.isNotBlank() &&
                                selectedDate.isNotBlank() &&
                                selectedTime.isNotBlank() &&
                                reason.isNotBlank() &&
                                !appointmentsState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extColors.mint.color,
                            contentColor = extColors.mint.onColor
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (appointmentsState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = extColors.mint.onColor
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmar Cita")
                        }
                    }
                }
            }
        }

        // Diálogo para seleccionar fecha
        if (showDatePicker) {
            DatePickerDialog(
                availableDays = appointmentsState.availableDays,
                onDateSelected = { day ->
                    selectedDate = day.date
                    selectedTime = "" // Reset hora
                    showDatePicker = false
                    if (currentStep == 2) currentStep = 3
                    // Cargar horarios para esta fecha
                    viewModel.loadAvailableTimeSlots(day.date)
                },
                onDismiss = { showDatePicker = false },
                color = extColors.peach
            )
        }

        // Mostrar errores
        appointmentsState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearState() }) {
                        Text("OK")
                    }
                },
                icon = {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Error") },
                text = { Text(error) }
            )
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: Int,
    title: String,
    icon: ImageVector,
    color: ColorFamily,
    isActive: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                color.colorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isActive) color.color else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stepNumber.toString(),
                            color = if (isActive) color.onColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isActive) color.onColorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) color.onColorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isActive) {
                content()
            }
        }
    }
}

@Composable
private fun SelectedPetCard(pet: Pet, color: ColorFamily) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when (pet.species) {
                    PetSpecies.DOG -> "🐕"
                    PetSpecies.CAT -> "🐈"
                    else -> "🐾"
                },
                style = MaterialTheme.typography.headlineMedium
            )
            Column {
                Text(
                    pet.name,
                    fontWeight = FontWeight.Bold,
                    color = color.onColor
                )
                Text(
                    "${pet.species} - ${pet.breed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = color.onColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetSelectionCard(
    pet: Pet,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: ColorFamily
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.color else color.colorContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (isSelected) color.onColor else color.onColorContainer,
                    unselectedColor = color.onColorContainer
                )
            )
            Text(
                text = when (pet.species) {
                    PetSpecies.DOG -> "🐕"
                    PetSpecies.CAT -> "🐈"
                    else -> "🐾"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Column {
                Text(
                    pet.name,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) color.onColor else color.onColorContainer
                )
                Text(
                    pet.breed,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) color.onColor.copy(alpha = 0.8f) else color.onColorContainer.copy(
                        alpha = 0.7f
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyTimeSlots(color: ColorFamily) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                "No hay horarios disponibles para esta fecha",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun TimeSlotGrid(
    timeSlots: List<TimeSlot>,
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    color: ColorFamily
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        timeSlots.chunked(3).forEach { rowSlots ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSlots.forEach { slot ->
                    TimeSlotChip(
                        slot = slot,
                        isSelected = selectedTime == slot.time,
                        onClick = { onTimeSelected(slot.time) },
                        color = color,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Rellenar espacios vacíos
                repeat(3 - rowSlots.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSlotChip(
    slot: TimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: ColorFamily,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = slot.isAvailable,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !slot.isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                isSelected -> color.color
                else -> color.colorContainer
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                slot.time,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    !slot.isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    isSelected -> color.onColor
                    else -> color.onColorContainer
                }
            )
        }
    }
}

@Composable
private fun AppointmentSummaryCard(
    pet: Pet?,
    date: String,
    time: String,
    reason: String,
    availableDays: List<AvailableDay>,
    color: ColorFamily
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.colorContainer
        ),
        border = BorderStroke(2.dp, color.color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color.color,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Resumen de tu cita",
                    fontWeight = FontWeight.Bold,
                    color = color.onColorContainer
                )
            }

            Divider(color = color.onColorContainer.copy(alpha = 0.2f))

            // Mascota
            SummaryItem(
                icon = Icons.Default.Pets,
                label = "Mascota",
                value = pet?.name ?: "",
                color = color
            )

            // Fecha
            val day = availableDays.find { it.date == date }
            SummaryItem(
                icon = Icons.Default.CalendarMonth,
                label = "Fecha",
                value = "${day?.dayName ?: ""} ${day?.displayDate ?: date}",
                color = color
            )

            // Hora
            SummaryItem(
                icon = Icons.Default.AccessTime,
                label = "Hora",
                value = time,
                color = color
            )

            // Motivo
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.onColorContainer.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = color.onColorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Motivo",
                            style = MaterialTheme.typography.labelMedium,
                            color = color.onColorContainer.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color.onColorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: ColorFamily
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color.onColorContainer,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color.onColorContainer.copy(alpha = 0.7f)
            )
            Text(
                value,
                fontWeight = FontWeight.Medium,
                color = color.onColorContainer
            )
        }
    }
}

@Composable
private fun DatePickerDialog(
    availableDays: List<AvailableDay>,
    onDateSelected: (AvailableDay) -> Unit,
    onDismiss: () -> Unit,
    color: ColorFamily
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Selecciona una fecha",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableDays.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📅", style = MaterialTheme.typography.displayMedium)
                                    Text(
                                        "No hay días disponibles",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        availableDays.take(14).forEach { day ->
                            item {
                                DayCard(
                                    day = day,
                                    onClick = { onDateSelected(day) },
                                    color = color
                                )
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCard(
    day: AvailableDay,
    onClick: () -> Unit,
    color: ColorFamily
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.colorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    day.dayName,
                    fontWeight = FontWeight.Bold,
                    color = color.onColorContainer
                )
                Text(
                    day.displayDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color.onColorContainer
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.color.copy(alpha = 0.2f)
            ) {
                Text(
                    "${day.startTime} - ${day.endTime}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = color.onColorContainer
                )
            }
        }
    }
}