package cl.clinipets.ui.screens.appointments

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.ui.theme.ColorFamily
import cl.clinipets.ui.theme.ExtendedColorScheme
import cl.clinipets.ui.theme.LocalExtendedColors
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// package cl.clinipets.ui.screens.appointments

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAppointment: () -> Unit,
    onNavigateToAppointmentDetail: (String) -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val appointmentsState by viewModel.appointmentsState.collectAsStateWithLifecycle()
    val extColors = LocalExtendedColors.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf<AppointmentStatus?>(null) }

    // Filtrar citas según tab y estado
    val displayedAppointments = remember(selectedTab, filterStatus, appointmentsState) {
        val baseList = if (selectedTab == 0)
            appointmentsState.upcomingAppointments
        else
            appointmentsState.pastAppointments

        if (filterStatus != null) {
            baseList.filter { it.status == filterStatus }
        } else {
            baseList
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Citas")
                        Text(
                            "${appointmentsState.appointments.size} citas en total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        BadgedBox(
                            badge = {
                                if (filterStatus != null) {
                                    Badge(
                                        containerColor = extColors.pink.color
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filtrar",
                                tint = if (filterStatus != null)
                                    extColors.pink.color
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddAppointment,
                containerColor = extColors.mint.color,
                contentColor = extColors.mint.onColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva cita")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Cita")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Custom Tab Row con animación
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomTab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = "Próximas",
                        count = appointmentsState.upcomingAppointments.size,
                        icon = Icons.Default.Event,
                        color = extColors.peach,
                        modifier = Modifier.weight(1f)
                    )
                    CustomTab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = "Pasadas",
                        count = appointmentsState.pastAppointments.size,
                        icon = Icons.Default.History,
                        color = extColors.lavander,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Contenido
            when {
                appointmentsState.isLoading -> {
                    LoadingState(color = if (selectedTab == 0) extColors.peach else extColors.lavander)
                }

                displayedAppointments.isEmpty() -> {
                    EmptyAppointmentsState(
                        isPast = selectedTab == 1,
                        onCreateAppointment = onNavigateToAddAppointment,
                        color = if (selectedTab == 0) extColors.peach else extColors.lavander
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Agrupar por mes
                        val groupedAppointments = displayedAppointments.groupBy { appointment ->
                            SimpleDateFormat("MMMM yyyy", Locale("es")).format(
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(
                                    appointment.date
                                )!!
                            ).replaceFirstChar { it.uppercase() }
                        }

                        groupedAppointments.forEach { (month, appointments) ->
                            item {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(
                                items = appointments,
                                key = { it.id }
                            ) { appointment ->
                                AppointmentCard(
                                    appointment = appointment,
                                    isPast = selectedTab == 1,
                                    onClick = { onNavigateToAppointmentDetail(appointment.id) },
                                    onCancel = { viewModel.cancelAppointment(appointment.id) },
                                    onConfirm = { viewModel.confirmAppointment(appointment.id) },
                                    extColors = extColors
                                )
                            }
                        }

                        // Espacio para el FAB
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Diálogo de filtros
    if (showFilterDialog) {
        FilterDialog(
            currentFilter = filterStatus,
            onFilterSelected = { status ->
                filterStatus = status
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
            color = if (selectedTab == 0) extColors.peach else extColors.lavander
        )
    }

    // Mostrar errores
    appointmentsState.error?.let { error ->
        LaunchedEffect(error) {
            // Aquí podrías mostrar un Snackbar
            viewModel.clearState()
        }
    }
}

@Composable
private fun CustomTab(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    count: Int,
    icon: ImageVector,
    color: ColorFamily,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) color.color else color.colorContainer,
        animationSpec = tween(300),
        label = "tabColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = animatedColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) color.onColor else color.onColorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) color.onColor else color.onColorContainer
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected) color.onColor.copy(alpha = 0.2f) else color.onColorContainer.copy(
                        alpha = 0.1f
                    )
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) color.onColor else color.onColorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentCard(
    appointment: Appointment,
    isPast: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    extColors: ExtendedColorScheme
) {
    val statusColor = when (appointment.status) {
        AppointmentStatus.SCHEDULED -> extColors.peach
        AppointmentStatus.CONFIRMED -> extColors.mint
        AppointmentStatus.COMPLETED -> extColors.lavander
        AppointmentStatus.CANCELLED -> extColors.pink
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.colorContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = statusColor.color.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con fecha y hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusColor.color,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = getStatusText(appointment.status),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor.onColorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = statusColor.onColorContainer
                            )
                            Text(
                                text = formatDate(appointment.date),
                                fontWeight = FontWeight.Medium,
                                color = statusColor.onColorContainer
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = statusColor.onColorContainer
                            )
                            Text(
                                text = appointment.time,
                                fontWeight = FontWeight.Medium,
                                color = statusColor.onColorContainer
                            )
                        }
                    }
                }

                // Pet emoji basado en el ID (simulado)
                Text(
                    text = "🐾",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Razón de la cita
            if (appointment.reason.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.onColorContainer.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = statusColor.onColorContainer
                        )
                        Text(
                            text = appointment.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor.onColorContainer
                        )
                    }
                }
            }

            // Acciones según estado
            if (!isPast && appointment.status == AppointmentStatus.SCHEDULED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extColors.mint.color,
                            contentColor = extColors.mint.onColor
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar")
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de cancelación
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            },
            title = { Text("¿Cancelar cita?") },
            text = {
                Text("Esta acción no se puede deshacer. ¿Estás seguro de que deseas cancelar esta cita?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancel()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sí, cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, mantener")
                }
            }
        )
    }
}

@Composable
private fun LoadingState(color: ColorFamily) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = color.color,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Cargando tus citas...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyAppointmentsState(
    isPast: Boolean,
    onCreateAppointment: () -> Unit,
    color: ColorFamily
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.colorContainer,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isPast) "📋" else "📅",
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }

            Text(
                text = if (isPast) "Sin citas pasadas" else "No tienes citas próximas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isPast)
                    "Aquí aparecerán tus citas anteriores"
                else
                    "¡Agenda una cita para tu mascota!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isPast) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateAppointment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color.color,
                        contentColor = color.onColor
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agendar Primera Cita")
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    currentFilter: AppointmentStatus?,
    onFilterSelected: (AppointmentStatus?) -> Unit,
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
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Filtrar por estado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Opción para mostrar todas
                FilterOption(
                    selected = currentFilter == null,
                    onClick = { onFilterSelected(null) },
                    text = "Todas las citas",
                    color = color
                )

                Divider()

                // Opciones de estado
                AppointmentStatus.values().forEach { status ->
                    FilterOption(
                        selected = currentFilter == status,
                        onClick = { onFilterSelected(status) },
                        text = getStatusText(status),
                        statusColor = getStatusColor(status, color.colorContainer),
                        color = color
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterOption(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    statusColor: Color? = null,
    color: ColorFamily
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                color.colorContainer
            else
                Color.Transparent
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
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = color.color,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (statusColor != null) {
                Surface(
                    shape = CircleShape,
                    color = statusColor,
                    modifier = Modifier.size(8.dp)
                ) {}
            }

            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected)
                    color.onColorContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Funciones auxiliares
private fun getStatusText(status: AppointmentStatus): String = when (status) {
    AppointmentStatus.SCHEDULED -> "Agendada"
    AppointmentStatus.CONFIRMED -> "Confirmada"
    AppointmentStatus.COMPLETED -> "Completada"
    AppointmentStatus.CANCELLED -> "Cancelada"
}

private fun getStatusColor(status: AppointmentStatus, fallback: Color): Color = when (status) {
    AppointmentStatus.SCHEDULED -> Color(0xFFFFB74D)
    AppointmentStatus.CONFIRMED -> Color(0xFF81C784)
    AppointmentStatus.COMPLETED -> Color(0xFF9575CD)
    AppointmentStatus.CANCELLED -> Color(0xFFE57373)
}

private fun formatDate(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        SimpleDateFormat("d 'de' MMM", Locale("es")).format(date!!)
    } catch (e: Exception) {
        dateString
    }
}