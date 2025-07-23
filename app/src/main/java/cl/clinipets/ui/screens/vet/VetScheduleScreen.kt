// Actualizar el archivo app/src/main/java/cl/clinipets/ui/screens/vet/VetScheduleScreen.kt

// ui/screens/vet/VetScheduleScreen.kt
package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import cl.clinipets.ui.viewmodels.VetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetScheduleScreen(
    onNavigateBack: () -> Unit,
    viewModel: VetViewModel = hiltViewModel()
) {
    val vetState by viewModel.vetState.collectAsState()

    // Estados para cada día
    var schedules by remember {
        mutableStateOf(
            mapOf(
                1 to DaySchedule("Lunes", true, "09:00", "18:00"),
                2 to DaySchedule("Martes", true, "09:00", "18:00"),
                3 to DaySchedule("Miércoles", true, "09:00", "18:00"),
                4 to DaySchedule("Jueves", true, "09:00", "18:00"),
                5 to DaySchedule("Viernes", true, "09:00", "18:00"),
                6 to DaySchedule("Sábado", false, "09:00", "13:00"),
                7 to DaySchedule("Domingo", false, "09:00", "13:00")
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadVetSchedules()
    }

    LaunchedEffect(vetState.vetSchedules) {
        if (vetState.vetSchedules.isNotEmpty()) {
            val updatedSchedules = schedules.toMutableMap()
            vetState.vetSchedules.forEach { schedule ->
                updatedSchedules[schedule.dayOfWeek] = DaySchedule(
                    name = getDayName(schedule.dayOfWeek),
                    active = schedule.active,
                    startTime = schedule.startTime,
                    endTime = schedule.endTime
                )
            }
            schedules = updatedSchedules
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Horarios de Atención") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Configure los horarios de atención para cada día de la semana",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            schedules.forEach { (dayNumber, schedule) ->
                DayScheduleCard(
                    daySchedule = schedule,
                    onToggleActive = { active ->
                        schedules = schedules.toMutableMap().apply {
                            this[dayNumber] = schedule.copy(active = active)
                        }
                    },
                    onTimeChange = { start, end ->
                        schedules = schedules.toMutableMap().apply {
                            this[dayNumber] = schedule.copy(startTime = start, endTime = end)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.saveVetSchedules(schedules)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Horarios")
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    daySchedule: DaySchedule,
    onToggleActive: (Boolean) -> Unit,
    onTimeChange: (String, String) -> Unit
) {
    var showTimeDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = daySchedule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = daySchedule.active,
                    onCheckedChange = onToggleActive
                )
            }

            if (daySchedule.active) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${daySchedule.startTime} - ${daySchedule.endTime}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = { showTimeDialog = true }) {
                        Text("Cambiar")
                    }
                }
            }
        }
    }

    if (showTimeDialog) {
        TimeRangeDialog(
            startTime = daySchedule.startTime,
            endTime = daySchedule.endTime,
            onConfirm = { start, end ->
                onTimeChange(start, end)
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false }
        )
    }
}

@Composable
private fun TimeRangeDialog(
    startTime: String,
    endTime: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStart by remember { mutableStateOf(startTime) }
    var selectedEnd by remember { mutableStateOf(endTime) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Horario") },
        text = {
            Column {
                Text("Horario de apertura:")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    listOf("08:00", "09:00", "10:00", "11:00").forEach { time ->
                        OutlinedButton(
                            onClick = { selectedStart = time },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(time, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Horario de cierre:")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    listOf("13:00", "17:00", "18:00", "20:00").forEach { time ->
                        OutlinedButton(
                            onClick = { selectedEnd = time },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(time, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Horario seleccionado: $selectedStart - $selectedEnd",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedStart, selectedEnd) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

internal data class DaySchedule(
    val name: String,
    val active: Boolean,
    val startTime: String,
    val endTime: String
)

private fun getDayName(dayNumber: Int): String {
    return when (dayNumber) {
        1 -> "Lunes"
        2 -> "Martes"
        3 -> "Miércoles"
        4 -> "Jueves"
        5 -> "Viernes"
        6 -> "Sábado"
        7 -> "Domingo"
        else -> ""
    }
}