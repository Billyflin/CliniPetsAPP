// ui/screens/vet/VetScreens.kt
package cl.clinipets.ui.screens.vet

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.ui.viewmodels.VetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ====================== DASHBOARD VETERINARIO ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConsultation: (String) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    viewModel: VetViewModel = hiltViewModel()
) {
    val vetState by viewModel.vetState.collectAsState()

    if (!vetState.isVeterinarian) {
        NoVetAccessScreen(onNavigateBack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Veterinario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Notificaciones */ }) {

                        Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")

                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen del día
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Hoy, ${
                                SimpleDateFormat(
                                    "EEEE d 'de' MMMM",
                                    Locale("es")
                                ).format(Date())
                            }",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCard(
                                value = vetState.todayAppointments.size.toString(),
                                label = "Citas hoy",
                                icon = Icons.Default.CalendarMonth
                            )
                            StatCard(
                                value = vetState.todayAppointments.count {
                                    it.first.status == AppointmentStatus.COMPLETED
                                }.toString(),
                                label = "Completadas",
                                icon = Icons.Default.CheckCircle
                            )
                            StatCard(
                                value = vetState.todayAppointments.count {
                                    it.first.status == AppointmentStatus.SCHEDULED
                                }.toString(),
                                label = "Pendientes",
                                icon = Icons.Default.Schedule
                            )
                        }
                    }
                }
            }

            // Accesos rápidos
            item {
                Text(
                    text = "Accesos rápidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Inventario",
                        subtitle = "${vetState.inventorySummary?.lowStockCount ?: 0} items bajo stock",
                        icon = Icons.Default.Inventory,
                        onClick = onNavigateToInventory,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Mi Horario",
                        subtitle = "Configurar disponibilidad",
                        icon = Icons.Default.AccessTime,
                        onClick = onNavigateToSchedule,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Servicios",
                        subtitle = "Gestionar precios",
                        icon = Icons.Default.MedicalServices,
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Citas del día
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Citas de hoy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { /* TODO: Ver todas */ }) {
                        Text("Ver todas")
                    }
                }
            }

            if (vetState.todayAppointments.isEmpty()) {
                item {
                    Card {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay citas programadas para hoy",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(vetState.todayAppointments) { (appointment, pet) ->
                    VetAppointmentCard(
                        appointment = appointment,
                        pet = pet,
                        onClick = {
                            if (appointment.status == AppointmentStatus.SCHEDULED) {
                                onNavigateToConsultation(appointment.id)
                            } else if (appointment.consultationId != null) {
                                onNavigateToConsultation(appointment.consultationId)
                            }
                        }
                    )
                }
            }

            // Alertas de inventario
            if (vetState.lowStockMedications.isNotEmpty() || vetState.lowStockVaccines.isNotEmpty()) {
                item {
                    Text(
                        text = "Alertas de inventario",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Items con stock bajo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            vetState.lowStockMedications.take(3).forEach { medication ->
                                Text("• ${medication.name}: ${medication.stock} unidades")
                            }
                            if (vetState.lowStockMedications.size > 3) {
                                Text("... y ${vetState.lowStockMedications.size - 3} más")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

