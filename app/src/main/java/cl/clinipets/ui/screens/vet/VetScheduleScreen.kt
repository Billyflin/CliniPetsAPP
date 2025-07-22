// ui/screens/vet/VetDashboardScreen.kt
package cl.clinipets.ui.screens.vet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.theme.LocalExtendedColors
import cl.clinipets.ui.viewmodels.DaySchedule
import cl.clinipets.ui.viewmodels.VetViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetScheduleScreen(
    onNavigateBack: () -> Unit,
    viewModel: VetViewModel = hiltViewModel()
) {
    val vetState by viewModel.vetState.collectAsState()
    val extColors = LocalExtendedColors.current
    var isEditMode by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Estado temporal para edición
    var editingSchedule by remember { mutableStateOf(vetState.vetSchedule) }

    // Actualizar cuando cambie el estado
    LaunchedEffect(vetState.vetSchedule) {
        if (!isEditMode) {
            editingSchedule = vetState.vetSchedule
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Editar horario" else "Mi horario de atención",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showExitDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (!isEditMode) {
                        IconButton(
                            onClick = {
                                isEditMode = true
                                editingSchedule = vetState.vetSchedule
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FloatingActionButton(
                    onClick = {
                        // Guardar cambios
                        editingSchedule.forEach { (day, schedule) ->
                            viewModel.updateVetSchedule(
                                dayNumber = day,
                                isActive = schedule.isActive,
                                startTime = schedule.startTime,
                                endTime = schedule.endTime
                            )
                        }
                        isEditMode = false
                        hasUnsavedChanges = false
                    },
                    containerColor = extColors.mint.color,
                    contentColor = extColors.mint.onColor
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Guardar cambios")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Información general
            item {
                InfoCard()
            }

            // Resumen de horario
            item {
                ScheduleSummaryCard(
                    schedule = if (isEditMode) editingSchedule else vetState.vetSchedule,
                    extColors = extColors
                )
            }

            // Días de la semana
            item {
                Text(
                    text = "Configuración por día",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Lista de días
            val daysOfWeek = listOf(
                1 to "Lunes",
                2 to "Martes",
                3 to "Miércoles",
                4 to "Jueves",
                5 to "Viernes",
                6 to "Sábado",
                7 to "Domingo"
            )

            daysOfWeek.forEach { (dayNumber, dayName) ->
                item {
                    DayScheduleCard(
                        dayNumber = dayNumber,
                        dayName = dayName,
                        schedule = editingSchedule[dayNumber] ?: DaySchedule(),
                        isEditMode = isEditMode,
                        onScheduleChange = { newSchedule ->
                            editingSchedule = editingSchedule.toMutableMap().apply {
                                this[dayNumber] = newSchedule
                            }
                            hasUnsavedChanges = true
                        },
                        accentColor = when (dayNumber) {
                            1, 5 -> extColors.mint.color
                            2, 6 -> extColors.lavander.color
                            3, 7 -> extColors.peach.color
                            4 -> extColors.pink.color
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            // Botón cancelar edición
            if (isEditMode) {
                item {
                    OutlinedCard(
                        onClick = {
                            isEditMode = false
                            hasUnsavedChanges = false
                            editingSchedule = vetState.vetSchedule
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Cancelar edición",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Dialogo de confirmación al salir
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("¿Descartar cambios?") },
                text = { Text("Tienes cambios sin guardar. ¿Deseas descartarlos?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            isEditMode = false
                            hasUnsavedChanges = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Descartar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Continuar editando")
                    }
                }
            )
        }

        // Mostrar error si existe
        vetState.error?.let { error ->
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
private fun InfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Configura tu disponibilidad",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Define los días y horarios en los que atenderás consultas. Los clientes solo podrán agendar citas en estos horarios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ScheduleSummaryCard(
    schedule: Map<Int, DaySchedule>,
    extColors: cl.clinipets.ui.theme.ExtendedColorScheme
) {
    val activeDays = schedule.count { it.value.isActive }
    val totalHours = schedule.filter { it.value.isActive }.map { (_, daySchedule) ->
        calculateHours(daySchedule.startTime, daySchedule.endTime)
    }.sum()

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Resumen semanal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Días activos
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = extColors.mint.colorContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = activeDays.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = extColors.mint.onColorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Días activos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Horas semanales
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = extColors.lavander.colorContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = totalHours.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = extColors.lavander.onColorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Horas/semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayScheduleCard(
    dayNumber: Int,
    dayName: String,
    schedule: DaySchedule,
    isEditMode: Boolean,
    onScheduleChange: (DaySchedule) -> Unit,
    accentColor: Color
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val cardElevation by animateDpAsState(
        targetValue = if (schedule.isActive && isEditMode) 8.dp else 0.dp,
        label = "elevation"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (schedule.isActive) {
            accentColor.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "backgroundColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicador de día
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (schedule.isActive) accentColor
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (schedule.isActive) FontWeight.Bold else FontWeight.Normal
                        )
                        if (schedule.isActive) {
                            Text(
                                text = "${schedule.startTime} - ${schedule.endTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "No disponible",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (isEditMode) {
                    Switch(
                        checked = schedule.isActive,
                        onCheckedChange = { isActive ->
                            onScheduleChange(schedule.copy(isActive = isActive))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor,
                            checkedTrackColor = accentColor.copy(alpha = 0.5f)
                        )
                    )
                } else if (schedule.isActive) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Selector de horarios (solo en modo edición y si está activo)
            AnimatedVisibility(
                visible = isEditMode && schedule.isActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hora de inicio
                        TimeSelector(
                            label = "Desde",
                            time = schedule.startTime,
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f),
                            accentColor = accentColor
                        )

                        // Hora de fin
                        TimeSelector(
                            label = "Hasta",
                            time = schedule.endTime,
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f),
                            accentColor = accentColor
                        )
                    }

                    // Validación de horarios
                    val hoursWorked = calculateHours(schedule.startTime, schedule.endTime)
                    if (hoursWorked <= 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ El horario de fin debe ser posterior al de inicio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$hoursWorked horas de atención",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Diálogos de selección de hora
    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = schedule.startTime,
            onTimeSelected = { time ->
                onScheduleChange(schedule.copy(startTime = time))
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
            title = "Hora de inicio"
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = schedule.endTime,
            onTimeSelected = { time ->
                onScheduleChange(schedule.copy(endTime = time))
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false },
            title = "Hora de fin"
        )
    }
}

@Composable
private fun TimeSelector(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = accentColor.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String
) {
    val timeParts = initialTime.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
    val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                    onTimeSelected(timeString)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Función auxiliar para calcular horas entre dos tiempos
private fun calculateHours(startTime: String, endTime: String): Int {
    val startParts = startTime.split(":")
    val endParts = endTime.split(":")

    val startHour = startParts.getOrNull(0)?.toIntOrNull() ?: 0
    val startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0
    val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: 0
    val endMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0

    val startTotalMinutes = startHour * 60 + startMinute
    val endTotalMinutes = endHour * 60 + endMinute

    return ((endTotalMinutes - startTotalMinutes) / 60).coerceAtLeast(0)
}