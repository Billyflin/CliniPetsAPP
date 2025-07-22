package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.Vaccine
import cl.clinipets.ui.viewmodels.InventoryViewModel

// ====================== INVENTARIO ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val vetState by viewModel.inventoryState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadInventory()
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMedicationDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Resumen
            vetState.let { summary ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = summary.medications.size.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total items",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Medicamentos") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Vacunas") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Stock bajo") }
                )
            }

            when (selectedTab) {
                0 -> MedicationsInventoryTab(
                    medications = vetState.medications,
                    onUpdateStock = { medicationId, newStock ->
                        viewModel.updateMedicationStock(
                            medicationId,
                            newStock,
                        )
                    }
                )

                1 -> VaccinesInventoryTab(
                    vaccines = vetState.vaccines
                )
            }
        }

        if (showAddMedicationDialog) {
            AddMedicationDialog(
                onMedicationAdded = {
                    showAddMedicationDialog = false
                    viewModel.loadInventory()
                },
                onDismiss = { showAddMedicationDialog = false }
            )
        }
    }
}


@Composable
private fun MedicationsInventoryTab(
    medications: List<Medication>,
    onUpdateStock: (String, Int) -> Unit
) {
    if (medications.isEmpty()) {
        EmptyInventoryState(
            title = "No hay medicamentos",
            message = "Agrega medicamentos al inventario"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(medications) { medication ->
                MedicationInventoryCard(
                    medication = medication,
                    onUpdateStock = { newStock ->
                        onUpdateStock(medication.id, newStock)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationInventoryCard(
    medication: Medication,
    onUpdateStock: (Int) -> Unit
) {
    var showUpdateDialog by remember { mutableStateOf(false) }

    Card(
        onClick = { showUpdateDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${medication.presentation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = medication.unitPrice.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Precio: ${medication.unitPrice}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showUpdateDialog) {
        UpdateStockDialog(
            medicationName = medication.name,
            currentStock = medication.stock,
            onUpdateStock = { newStock ->
                onUpdateStock(newStock)
                showUpdateDialog = false
            },
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
private fun VaccinesInventoryTab(vaccines: List<Vaccine>) {
    if (vaccines.isEmpty()) {
        EmptyInventoryState(
            title = "No hay vacunas",
            message = "Agrega vacunas al inventario"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vaccines) { vaccine ->
                VaccineInventoryCard(vaccine)
            }
        }
    }
}

@Composable
private fun VaccineInventoryCard(vaccine: Vaccine) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vaccine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Precio: ${vaccine.unitPrice}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
private fun EmptyInventoryState(
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
            imageVector = Icons.Default.Inventory,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddMedicationDialog(
    onMedicationAdded: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {

}

@Composable
private fun UpdateStockDialog(
    medicationName: String,
    currentStock: Int,
    onUpdateStock: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newStock by remember { mutableStateOf(currentStock.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actualizar stock") },
        text = {
            Column {
                Text(medicationName)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newStock,
                    onValueChange = { newStock = it },
                    label = { Text("Nuevo stock") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Stock actual: $currentStock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    newStock.toIntOrNull()?.let { stock ->
                        onUpdateStock(stock)
                    }
                },
                enabled = newStock.toIntOrNull() != null && newStock.toIntOrNull() != currentStock
            ) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun AppointmentStatusBadge(status: AppointmentStatus) {
    val (color, text) = when (status) {
        AppointmentStatus.SCHEDULED -> MaterialTheme.colorScheme.primary to "Agendada"
        AppointmentStatus.CONFIRMED -> MaterialTheme.colorScheme.secondary to "Confirmada"
        AppointmentStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Completada"
        else -> MaterialTheme.colorScheme.surfaceVariant to status.name
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VetAppointmentCard(
    appointment: Appointment,
    pet: Pet?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hora
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = appointment.time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AppointmentStatusBadge(appointment.status)
            }

            // Información
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pet?.name ?: "Mascota",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${pet?.species?.name ?: ""} - ${pet?.breed ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Acción
            if (appointment.status == AppointmentStatus.SCHEDULED) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Iniciar", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
    }
}

