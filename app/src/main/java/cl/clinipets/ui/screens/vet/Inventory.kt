package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.MedicationPresentation
import cl.clinipets.data.model.ServiceCategory
import cl.clinipets.ui.viewmodels.InventoryViewModel

// ====================== INVENTARIO ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val inventoryState by viewModel.inventoryState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showAddVaccineDialog by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }

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
                onClick = {
                    when (selectedTab) {
                        0 -> showAddMedicationDialog = true
                        1 -> showAddVaccineDialog = true
                        2 -> showAddServiceDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
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
                    text = { Text("Servicios") }
                )
            }

            if (inventoryState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(inventoryState.medications) { medication ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(medication.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    when (medication.presentation) {
                                                        MedicationPresentation.TABLET -> "Tableta"
                                                        MedicationPresentation.SYRUP -> "Jarabe"
                                                        MedicationPresentation.INJECTION -> "Inyección"
                                                        MedicationPresentation.CREAM -> "Crema"
                                                        MedicationPresentation.DROPS -> "Gotas"
                                                        MedicationPresentation.OTHER -> "Otro"
                                                    }
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Stock: ${medication.stock}")
                                                Text("$${medication.unitPrice}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(inventoryState.vaccines) { vaccine ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(vaccine.name, fontWeight = FontWeight.Bold)
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Stock: ${vaccine.stock}")
                                                Text("$${vaccine.unitPrice}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(inventoryState.services) { service ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(service.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    when (service.category) {
                                                        ServiceCategory.CONSULTATION -> "Consulta"
                                                        ServiceCategory.VACCINATION -> "Vacunación"
                                                        ServiceCategory.SURGERY -> "Cirugía"
                                                        ServiceCategory.GROOMING -> "Peluquería"
                                                        ServiceCategory.OTHER -> "Otro"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text("$${service.basePrice}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogos
        if (showAddMedicationDialog) {
            CreateMedicationDialog(
                onMedicationCreated = { name, presentation, stock, price ->
                    viewModel.addMedication(name, presentation, stock, price)
                    showAddMedicationDialog = false
                },
                onDismiss = { showAddMedicationDialog = false }
            )
        }

        if (showAddVaccineDialog) {
            CreateVaccineDialog(
                onVaccineCreated = { name, stock, price ->
                    viewModel.addVaccine(name, stock, price)
                    showAddVaccineDialog = false
                },
                onDismiss = { showAddVaccineDialog = false }
            )
        }

        if (showAddServiceDialog) {
            CreateServiceDialog(
                onServiceCreated = { name, category, price ->
                    viewModel.addService(name, category, price)
                    showAddServiceDialog = false
                },
                onDismiss = { showAddServiceDialog = false }
            )
        }
    }
}

@Composable
fun CreateMedicationDialog(
    onMedicationCreated: (String, MedicationPresentation, Int, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedPresentation by remember { mutableStateOf(MedicationPresentation.TABLET) }
    var stock by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Medicamento") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Presentación", style = MaterialTheme.typography.bodySmall)
                Column {
                    MedicationPresentation.values().forEach { presentation ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPresentation = presentation }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPresentation == presentation,
                                onClick = { selectedPresentation = presentation }
                            )
                            Text(
                                text = when (presentation) {
                                    MedicationPresentation.TABLET -> "Tableta"
                                    MedicationPresentation.SYRUP -> "Jarabe"
                                    MedicationPresentation.INJECTION -> "Inyección"
                                    MedicationPresentation.CREAM -> "Crema"
                                    MedicationPresentation.DROPS -> "Gotas"
                                    MedicationPresentation.OTHER -> "Otro"
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { char -> char.isDigit() } },
                    label = { Text("Stock inicial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Precio unitario") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val stockInt = stock.toIntOrNull() ?: 0
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && stockInt > 0 && priceDouble > 0) {
                        onMedicationCreated(name, selectedPresentation, stockInt, priceDouble)
                    }
                },
                enabled = name.isNotBlank() && stock.isNotBlank() && price.isNotBlank()
            ) {
                Text("Agregar")
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
fun CreateVaccineDialog(
    onVaccineCreated: (String, Int, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Vacuna") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { char -> char.isDigit() } },
                    label = { Text("Stock inicial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Precio unitario") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val stockInt = stock.toIntOrNull() ?: 0
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && stockInt > 0 && priceDouble > 0) {
                        onVaccineCreated(name, stockInt, priceDouble)
                    }
                },
                enabled = name.isNotBlank() && stock.isNotBlank() && price.isNotBlank()
            ) {
                Text("Agregar")
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
fun CreateServiceDialog(
    onServiceCreated: (String, ServiceCategory, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ServiceCategory.CONSULTATION) }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Servicio") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría", style = MaterialTheme.typography.bodySmall)
                Column {
                    ServiceCategory.values().forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = category }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                            Text(
                                text = when (category) {
                                    ServiceCategory.CONSULTATION -> "Consulta"
                                    ServiceCategory.VACCINATION -> "Vacunación"
                                    ServiceCategory.SURGERY -> "Cirugía"
                                    ServiceCategory.GROOMING -> "Peluquería"
                                    ServiceCategory.OTHER -> "Otro"
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Precio base") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && priceDouble > 0) {
                        onServiceCreated(name, selectedCategory, priceDouble)
                    }
                },
                enabled = name.isNotBlank() && price.isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}