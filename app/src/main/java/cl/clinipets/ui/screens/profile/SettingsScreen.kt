// ui/screens/SettingsScreen.kt
package cl.clinipets.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.ui.theme.Contrast
import cl.clinipets.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configuración", style = MaterialTheme.typography.headlineMedium)

        // Tema
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Apariencia", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modo oscuro")
                    Switch(
                        checked = settingsState.isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Colores dinámicos")
                    Switch(
                        checked = settingsState.isDynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }

                Text("Contraste", modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Contrast.entries.forEach { contrast ->
                        FilterChip(
                            selected = settingsState.contrast == contrast,
                            onClick = { viewModel.setContrast(contrast) },
                            label = { Text(contrast.name) }
                        )
                    }
                }
            }
        }

        // Notificaciones
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Notificaciones", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recordatorios de citas")
                    Switch(
                        checked = settingsState.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotifications(it) }
                    )
                }

                if (settingsState.notificationsEnabled) {
                    Text("Recordar ${settingsState.reminderHours} horas antes")
                    Slider(
                        value = settingsState.reminderHours.toFloat(),
                        onValueChange = { viewModel.setReminderHours(it.toInt()) },
                        valueRange = 1f..48f,
                        steps = 47
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}