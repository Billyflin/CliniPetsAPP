// ui/screens/VetScheduleScreen.kt
package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.VetScheduleViewModel

@Composable
fun VetScheduleScreen(
    onNavigateBack: () -> Unit,
    viewModel: VetScheduleViewModel = hiltViewModel()
) {
    val scheduleState by viewModel.scheduleState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Configurar Horarios de Atención", style = MaterialTheme.typography.headlineMedium)

        // Días de la semana
        val daysOfWeek = listOf(
            "Lunes" to 2,
            "Martes" to 3,
            "Miércoles" to 4,
            "Jueves" to 5,
            "Viernes" to 6,
            "Sábado" to 7
        )

        daysOfWeek.forEach { (dayName, dayNumber) ->
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dayName)
                        Switch(
                            checked = scheduleState.schedule[dayNumber]?.isActive == true,
                            onCheckedChange = { isActive ->
                                viewModel.toggleDay(dayNumber, isActive)
                            }
                        )
                    }

                    if (scheduleState.schedule[dayNumber]?.isActive == true) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = scheduleState.schedule[dayNumber]?.startTime ?: "09:00",
                                onValueChange = { viewModel.updateStartTime(dayNumber, it) },
                                label = { Text("Desde") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = scheduleState.schedule[dayNumber]?.endTime ?: "18:00",
                                onValueChange = { viewModel.updateEndTime(dayNumber, it) },
                                label = { Text("Hasta") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.saveSchedule() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !scheduleState.isLoading
        ) {
            if (scheduleState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Guardar Horarios")
            }
        }

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }

        scheduleState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (scheduleState.isSaved) {
            LaunchedEffect(Unit) {
                onNavigateBack()
            }
        }
    }
}