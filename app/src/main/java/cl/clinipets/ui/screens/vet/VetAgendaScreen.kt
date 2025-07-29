// package: cl.clinipets.ui.screens.vet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.ui.viewmodels.AppointmentDetail
import cl.clinipets.ui.viewmodels.VetViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetAgendaScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    vetViewModel: VetViewModel = hiltViewModel()
) {
    val vetState by vetViewModel.vetState.collectAsStateWithLifecycle()
    var selectedDate by remember { mutableStateOf(Date()) }
    var viewMode by remember { mutableStateOf(ViewMode.WEEK) }

    LaunchedEffect(selectedDate, viewMode) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
        when (viewMode) {
            ViewMode.DAY -> vetViewModel.loadDayAppointments(dateStr)
            ViewMode.WEEK -> vetViewModel.loadWeekAppointments(dateStr)
            ViewMode.MONTH -> vetViewModel.loadMonthAppointments(dateStr)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda de Citas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Selector de vista
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            onClick = { viewMode = ViewMode.DAY },
                            selected = viewMode == ViewMode.DAY
                        ) {
                            Text("Día")
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            onClick = { viewMode = ViewMode.WEEK },
                            selected = viewMode == ViewMode.WEEK
                        ) {
                            Text("Semana")
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            onClick = { viewMode = ViewMode.MONTH },
                            selected = viewMode == ViewMode.MONTH
                        ) {
                            Text("Mes")
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Selector de fecha
            DateSelector(
                selectedDate = selectedDate,
                onDateChange = { selectedDate = it },
                viewMode = viewMode
            )

            HorizontalDivider()

            // Contenido animado según el modo de vista
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "view_mode_animation"
            ) { mode ->
                when (mode) {
                    ViewMode.DAY -> DayView(
                        date = selectedDate,
                        appointments = vetState.agendaAppointments,
                        onNavigateToConsultation = onNavigateToConsultation,
                        onNavigateToPetDetail = onNavigateToPetDetail,
                        isLoading = vetState.isLoading
                    )

                    ViewMode.WEEK -> WeekView(
                        startDate = selectedDate,
                        appointments = vetState.agendaAppointments,
                        onNavigateToConsultation = onNavigateToConsultation,
                        onNavigateToPetDetail = onNavigateToPetDetail,
                        isLoading = vetState.isLoading
                    )

                    ViewMode.MONTH -> MonthView(
                        month = selectedDate,
                        appointments = vetState.agendaAppointments,
                        onNavigateToConsultation = onNavigateToConsultation,
                        onNavigateToPetDetail = onNavigateToPetDetail,
                        onDateSelected = {
                            selectedDate = it
                            viewMode = ViewMode.DAY
                        },
                        isLoading = vetState.isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSelector(
    selectedDate: Date,
    onDateChange: (Date) -> Unit,
    viewMode: ViewMode
) {
    val calendar = Calendar.getInstance()
    calendar.time = selectedDate

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                when (viewMode) {
                    ViewMode.DAY -> calendar.add(Calendar.DAY_OF_MONTH, -1)
                    ViewMode.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
                    ViewMode.MONTH -> calendar.add(Calendar.MONTH, -1)
                }
                onDateChange(calendar.time)
            }) {
                Icon(Icons.Default.ChevronLeft, "Anterior")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (viewMode) {
                        ViewMode.DAY -> SimpleDateFormat("EEEE", Locale("es")).format(selectedDate)
                        ViewMode.WEEK -> {
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            val start = calendar.time
                            calendar.add(Calendar.DAY_OF_WEEK, 6)
                            val end = calendar.time
                            "${SimpleDateFormat("d", Locale.getDefault()).format(start)} - ${
                                SimpleDateFormat("d 'de' MMMM", Locale("es")).format(end)
                            }"
                        }

                        ViewMode.MONTH -> SimpleDateFormat(
                            "MMMM",
                            Locale("es")
                        ).format(selectedDate)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (viewMode) {
                        ViewMode.DAY -> SimpleDateFormat("d 'de' MMMM, yyyy", Locale("es")).format(
                            selectedDate
                        )

                        ViewMode.WEEK, ViewMode.MONTH -> SimpleDateFormat(
                            "yyyy",
                            Locale.getDefault()
                        ).format(selectedDate)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = {
                calendar.time = selectedDate
                when (viewMode) {
                    ViewMode.DAY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                    ViewMode.WEEK -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    ViewMode.MONTH -> calendar.add(Calendar.MONTH, 1)
                }
                onDateChange(calendar.time)
            }) {
                Icon(Icons.Default.ChevronRight, "Siguiente")
            }
        }
    }
}

@Composable
private fun DayView(
    date: Date,
    appointments: List<AppointmentDetail>,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    isLoading: Boolean
) {
    val timeSlots = generateTimeSlots()
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    val dayAppointments = appointments.filter { it.appointment.date == dateStr }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(timeSlots) { timeSlot ->
                val slotAppointments = dayAppointments.filter { it.appointment.time == timeSlot }

                TimeSlotRow(
                    time = timeSlot,
                    appointments = slotAppointments,
                    onNavigateToConsultation = onNavigateToConsultation,
                    onNavigateToPetDetail = onNavigateToPetDetail
                )
            }
        }
    }
}

@Composable
private fun TimeSlotRow(
    time: String,
    appointments: List<AppointmentDetail>,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToPetDetail: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hora
        Card(
            modifier = Modifier.width(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (appointments.isNotEmpty())
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = time,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                fontWeight = if (appointments.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
            )
        }

        // Citas
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (appointments.isEmpty()) {
                Text(
                    "Disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                appointments.forEach { appointmentDetail ->
                    AppointmentCard(
                        appointmentDetail = appointmentDetail,
                        onNavigateToConsultation = onNavigateToConsultation,
                        onNavigateToPetDetail = onNavigateToPetDetail,
                        expanded = true
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekView(
    startDate: Date,
    appointments: List<AppointmentDetail>,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    isLoading: Boolean
) {
    val calendar = Calendar.getInstance()
    calendar.time = startDate
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    val weekDays = (0..6).map { dayOffset ->
        val cal = Calendar.getInstance()
        cal.time = calendar.time
        cal.add(Calendar.DAY_OF_MONTH, dayOffset)
        cal.time
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(weekDays) { date ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val dayAppointments = appointments.filter { it.appointment.date == dateStr }
                val isToday = isSameDay(date, Date())

                DayCard(
                    date = date,
                    appointments = dayAppointments,
                    isToday = isToday,
                    onNavigateToConsultation = onNavigateToConsultation,
                    onNavigateToPetDetail = onNavigateToPetDetail
                )
            }
        }
    }
}

@Composable
private fun DayCard(
    date: Date,
    appointments: List<AppointmentDetail>,
    isToday: Boolean,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToPetDetail: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isToday) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        } else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        SimpleDateFormat("EEEE", Locale("es")).format(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        SimpleDateFormat("d 'de' MMMM", Locale("es")).format(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (appointments.isNotEmpty()) {
                    Badge(
                        containerColor = if (isToday)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    ) {
                        Text(appointments.size.toString())
                    }
                }
            }

            if (appointments.isEmpty()) {
                Text(
                    "Sin citas programadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                appointments.sortedBy { it.appointment.time }.forEach { appointmentDetail ->
                    AppointmentCard(
                        appointmentDetail = appointmentDetail,
                        onNavigateToConsultation = onNavigateToConsultation,
                        onNavigateToPetDetail = onNavigateToPetDetail,
                        expanded = false
                    )
                    if (appointmentDetail != appointments.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthView(
    month: Date,
    appointments: List<AppointmentDetail>,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    onDateSelected: (Date) -> Unit,
    isLoading: Boolean
) {
    val calendar = Calendar.getInstance()
    calendar.time = month
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Calcular días del mes anterior para llenar la primera semana
    val previousMonthDays = firstDayOfWeek - Calendar.SUNDAY - 1

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Encabezados de días
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid de días
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Días vacíos del inicio
                items(previousMonthDays) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }

                // Días del mes
                items(daysInMonth) { day ->
                    val dayCalendar = Calendar.getInstance()
                    dayCalendar.time = month
                    dayCalendar.set(Calendar.DAY_OF_MONTH, day + 1)

                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(dayCalendar.time)
                    val dayAppointments = appointments.filter { it.appointment.date == dateStr }
                    val isToday = isSameDay(dayCalendar.time, Date())

                    DayCell(
                        day = day + 1,
                        appointmentCount = dayAppointments.size,
                        isToday = isToday,
                        onClick = { onDateSelected(dayCalendar.time) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    appointmentCount: Int,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = when {
            isToday -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )

            appointmentCount > 0 -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )

            else -> CardDefaults.cardColors()
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isToday)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday || appointmentCount > 0)
                        FontWeight.Bold
                    else
                        FontWeight.Normal
                )
                if (appointmentCount > 0) {
                    Text(
                        text = appointmentCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appointmentDetail: AppointmentDetail,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    expanded: Boolean
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onNavigateToPetDetail(appointmentDetail.pet?.id ?: "") }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hora y estado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!expanded) {
                    Text(
                        appointmentDetail.appointment.time,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusChip(appointmentDetail.appointment.status)
            }

            // Información
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        appointmentDetail.pet?.name ?: "Sin mascota",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (appointmentDetail.ownerName == null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        appointmentDetail.ownerName ?: "Sin dueño asignado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (appointmentDetail.ownerName == null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expanded || appointmentDetail.appointment.reason.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            appointmentDetail.appointment.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Acción
            if (appointmentDetail.appointment.status == AppointmentStatus.SCHEDULED ||
                appointmentDetail.appointment.status == AppointmentStatus.CONFIRMED
            ) {
                IconButton(
                    onClick = { onNavigateToConsultation(appointmentDetail.appointment.id) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Iniciar consulta",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: AppointmentStatus) {
    val (containerColor, contentColor, text) = when (status) {
        AppointmentStatus.SCHEDULED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Agendada"
        )

        AppointmentStatus.CONFIRMED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Confirmada"
        )

        AppointmentStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Completada"
        )

        AppointmentStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Cancelada"
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

// Funciones auxiliares
private fun generateTimeSlots(): List<String> {
    val slots = mutableListOf<String>()
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 8)
    calendar.set(Calendar.MINUTE, 0)

    while (calendar.get(Calendar.HOUR_OF_DAY) < 20) {
        slots.add(SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time))
        calendar.add(Calendar.MINUTE, 30)
    }

    return slots
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }

    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

enum class ViewMode { DAY, WEEK, MONTH }