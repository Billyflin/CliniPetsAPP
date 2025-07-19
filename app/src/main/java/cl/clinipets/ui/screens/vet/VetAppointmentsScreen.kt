// ui/screens/VetAppointmentsScreen.kt
package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.VetAppointmentsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VetAppointmentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConsultation: (String) -> Unit,
    viewModel: VetAppointmentsViewModel = hiltViewModel()
) {
    val vetState by viewModel.vetAppointmentsState.collectAsState()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mis Citas", style = MaterialTheme.typography.headlineMedium)

        // Filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = vetState.filter == "TODAY",
                onClick = { viewModel.setFilter("TODAY") },
                label = { Text("Hoy") }
            )
            FilterChip(
                selected = vetState.filter == "PENDING",
                onClick = { viewModel.setFilter("PENDING") },
                label = { Text("Pendientes") }
            )
            FilterChip(
                selected = vetState.filter == "ALL",
                onClick = { viewModel.setFilter("ALL") },
                label = { Text("Todas") }
            )
        }

        if (vetState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(vetState.appointments) { appointment ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = appointment.petName,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                // Badge de estado
                                val (color, text) = when (appointment.status) {
                                    "SCHEDULED" -> MaterialTheme.colorScheme.primary to "Agendada"
                                    "CONFIRMED" -> MaterialTheme.colorScheme.secondary to "Confirmada"
                                    "IN_PROGRESS" -> MaterialTheme.colorScheme.tertiary to "En curso"
                                    "COMPLETED" -> MaterialTheme.colorScheme.outline to "Completada"
                                    else -> MaterialTheme.colorScheme.error to "Cancelada"
                                }

                                Surface(
                                    color = color.copy(alpha = 0.2f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = text,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color
                                    )
                                }
                            }

                            Text(
                                text = "${appointment.date} ${appointment.time}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Servicio: ${appointment.serviceType}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (appointment.notes.isNotBlank()) {
                                Text(
                                    text = "Notas: ${appointment.notes}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Acciones según el estado
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                when (appointment.status) {
                                    "SCHEDULED" -> {
                                        TextButton(
                                            onClick = { viewModel.confirmAppointment(appointment.id) }
                                        ) {
                                            Text("Confirmar")
                                        }
                                    }

                                    "CONFIRMED" -> {
                                        Button(
                                            onClick = { onNavigateToConsultation(appointment.id) }
                                        ) {
                                            Text("Atender")
                                        }
                                    }

                                    "COMPLETED" -> {
                                        TextButton(
                                            onClick = { /* Ver detalles de consulta */ }
                                        ) {
                                            Text("Ver consulta")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}