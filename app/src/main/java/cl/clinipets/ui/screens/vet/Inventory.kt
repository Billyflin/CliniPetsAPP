package cl.clinipets.ui.screens.vet

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import cl.clinipets.data.model.MedicalConsultation
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.PaymentMethod
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.Vaccine
import cl.clinipets.data.model.VeterinaryService
import cl.clinipets.ui.viewmodels.VetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ====================== INVENTARIO ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: VetViewModel = hiltViewModel()
) {
    val vetState by viewModel.vetState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadInventorySummary()
    }

    LaunchedEffect(searchQuery) {
        viewModel.searchMedications(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Generar reporte */ }) {
                        Icon(Icons.Default.Assessment, contentDescription = "Reporte")
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
            vetState.inventorySummary?.let { summary ->
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
                                text = summary.totalItems.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total items",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${summary.totalValue}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Valor total",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = summary.lowStockCount.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.lowStockCount > 0)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Stock bajo",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Buscar medicamento...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                }
            )

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
                    medications = if (searchQuery.isNotEmpty())
                        vetState.filteredMedications
                    else vetState.medications,
                    onUpdateStock = { medicationId, newStock ->
                        viewModel.updateMedicationStock(
                            medicationId,
                            newStock,
                            "Ajuste manual de inventario"
                        )
                    }
                )

                1 -> VaccinesInventoryTab(
                    vaccines = vetState.vaccines
                )

                2 -> LowStockTab(
                    medications = vetState.lowStockMedications,
                    vaccines = vetState.lowStockVaccines
                )
            }
        }

        if (showAddMedicationDialog) {
            AddMedicationDialog(
                onMedicationAdded = {
                    showAddMedicationDialog = false
                    viewModel.loadInventorySummary()
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
                        text = "${medication.activeIngredient} - ${medication.presentation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = medication.laboratory,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${medication.stock} unidades",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            medication.stock <= medication.minStock -> MaterialTheme.colorScheme.error
                            medication.stock <= medication.minStock * 1.5 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "Min: ${medication.minStock}",
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
                medication.expirationDate?.let { expDate ->
                    val daysUntilExpiration =
                        ((expDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                    Text(
                        text = when {
                            daysUntilExpiration < 0 -> "VENCIDO"
                            daysUntilExpiration <= 30 -> "Vence en $daysUntilExpiration días"
                            daysUntilExpiration <= 90 -> "Vence en ${daysUntilExpiration / 30} meses"
                            else -> "Vence: ${
                                SimpleDateFormat(
                                    "MM/yyyy",
                                    Locale.getDefault()
                                ).format(Date(expDate))
                            }"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            daysUntilExpiration < 0 -> MaterialTheme.colorScheme.error
                            daysUntilExpiration <= 30 -> MaterialTheme.colorScheme.error
                            daysUntilExpiration <= 90 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (medication.isControlled) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "CONTROLADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
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
                    Text(
                        text = vaccine.manufacturer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Previene: ${vaccine.diseases.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${vaccine.stock} dosis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            vaccine.stock <= vaccine.minStock -> MaterialTheme.colorScheme.error
                            vaccine.stock <= vaccine.minStock * 1.5 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "Min: ${vaccine.minStock}",
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
                    text = "Especies: ${vaccine.species.joinToString(", ") { it.name }}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Precio: ${vaccine.unitPrice}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
internal fun ServicesTab(
    consultation: MedicalConsultation?,
    availableServices: List<VeterinaryService>,
    onAddService: (String, Double) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Servicios aplicados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
        }

        if (consultation?.services?.isEmpty() == true) {
            item {
                Card {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay servicios agregados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            consultation?.services?.forEach { service ->
                item {
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = service.category.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${service.price}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Total
        consultation?.let {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subtotal servicios",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${it.services.sumOf { s -> s.price }}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddServiceDialog(
            availableServices = availableServices,
            onServiceSelected = { serviceId, price ->
                onAddService(serviceId, price)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddServiceDialog(
    availableServices: List<VeterinaryService>,
    onServiceSelected: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedService by remember { mutableStateOf<VeterinaryService?>(null) }
    var customPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar servicio") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Selecciona un servicio:")
                availableServices.forEach { service ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedService = service
                                customPrice = service.basePrice.toString()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedService == service,
                            onClick = {
                                selectedService = service
                                customPrice = service.basePrice.toString()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(service.name)
                            Text(
                                text = "Precio base: ${service.basePrice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                selectedService?.let {
                    OutlinedTextField(
                        value = customPrice,
                        onValueChange = { customPrice = it },
                        label = { Text("Precio a cobrar") },
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedService?.let { service ->
                        val price = customPrice.toDoubleOrNull() ?: service.basePrice
                        onServiceSelected(service.id, price)
                    }
                },
                enabled = selectedService != null && customPrice.isNotBlank()
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

private fun getPaymentMethodName(method: PaymentMethod): String {
    return when (method) {
        PaymentMethod.CASH -> "Efectivo"
        PaymentMethod.DEBIT_CARD -> "Tarjeta de débito"
        PaymentMethod.CREDIT_CARD -> "Tarjeta de crédito"
        PaymentMethod.TRANSFER -> "Transferencia"
        PaymentMethod.CHECK -> "Cheque"
        PaymentMethod.OTHER -> "Otro"
    }
}


@Composable
internal fun MedicationsTab(
    consultation: MedicalConsultation?,
    availableMedications: List<Medication>,
    onAddMedication: (String, String, String, String, String, Int, Double) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Medicamentos recetados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
        }

        if (consultation?.medications?.isEmpty() == true) {
            item {
                Card {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay medicamentos recetados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            consultation?.medications?.forEach { medication ->
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = medication.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${medication.totalPrice}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Dosis: ${medication.dose} - ${medication.frequency}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Duración: ${medication.duration} - Vía: ${medication.route}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Cantidad: ${medication.quantity} unidades",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        // TODO: Implementar diálogo para agregar medicamento
    }
}

@Composable
internal fun BillingTab(
    consultation: MedicalConsultation?,
    onFinishConsultation: (Double, String?, PaymentMethod, Double) -> Unit
) {
    var discount by remember { mutableStateOf("0") }
    var discountReason by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var amountPaid by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val subtotal = consultation?.subtotal ?: 0.0
    val discountAmount = discount.toDoubleOrNull() ?: 0.0
    val total = subtotal - discountAmount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Resumen
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Resumen de cobros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Divider()

                    // Servicios
                    consultation?.services?.forEach { service ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(service.name)
                            Text("${service.price}")
                        }
                    }

                    // Medicamentos
                    consultation?.medications?.forEach { medication ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${medication.name} x${medication.quantity}")
                            Text("${medication.totalPrice}")
                        }
                    }

                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontWeight = FontWeight.Medium)
                        Text("$subtotal", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Descuento
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Descuento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Monto de descuento") },
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (discountAmount > 0) {
                        OutlinedTextField(
                            value = discountReason,
                            onValueChange = { discountReason = it },
                            label = { Text("Motivo del descuento") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Total
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL A PAGAR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$total",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Forma de pago
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Forma de pago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    PaymentMethod.values().forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getPaymentMethodName(method))
                        }
                    }
                }
            }
        }

        // Monto pagado
        item {
            OutlinedTextField(
                value = amountPaid,
                onValueChange = { amountPaid = it },
                label = { Text("Monto pagado") },
                prefix = { Text("$") },
                modifier = Modifier.fillMaxWidth()
            )

            val paid = amountPaid.toDoubleOrNull() ?: 0.0
            if (paid > 0 && paid < total) {
                Text(
                    text = "Saldo pendiente: ${total - paid}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (paid > total) {
                Text(
                    text = "Vuelto: ${paid - total}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Botón finalizar
        item {
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = amountPaid.isNotBlank()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finalizar consulta y cobrar")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar finalización") },
            text = {
                Column {
                    Text("¿Confirmas los siguientes datos?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total: $total")
                    Text("Pagado: ${amountPaid.toDoubleOrNull() ?: 0.0}")
                    Text("Método: ${getPaymentMethodName(selectedPaymentMethod)}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onFinishConsultation(
                            discountAmount,
                            discountReason.ifBlank { null },
                            selectedPaymentMethod,
                            amountPaid.toDoubleOrNull() ?: 0.0
                        )
                        showConfirmDialog = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


@Composable
internal fun LowStockTab(
    medications: List<Medication>,
    vaccines: List<Vaccine>
) {
    if (medications.isEmpty() && vaccines.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "¡Todo en orden!",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "No hay items con stock bajo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (medications.isNotEmpty()) {
                item {
                    Text(
                        text = "Medicamentos con stock bajo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(medications) { medication ->
                    LowStockCard(
                        name = medication.name,
                        currentStock = medication.stock,
                        minStock = medication.minStock,
                        subtitle = "${medication.presentation} - ${medication.laboratory}"
                    )
                }
            }

            if (vaccines.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Vacunas con stock bajo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(vaccines) { vaccine ->
                    LowStockCard(
                        name = vaccine.name,
                        currentStock = vaccine.stock,
                        minStock = vaccine.minStock,
                        subtitle = vaccine.manufacturer
                    )
                }
            }
        }
    }
}

@Composable
private fun LowStockCard(
    name: String,
    currentStock: Int,
    minStock: Int,
    subtitle: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currentStock / $minStock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Actual / Mínimo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
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
    onDismiss: () -> Unit
) {
    // TODO: Implementar formulario completo
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar medicamento") },
        text = { Text("Formulario de medicamento aquí") },
        confirmButton = {
            TextButton(onClick = onMedicationAdded) {
                Text("Guardar")
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
        AppointmentStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary to "En curso"
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
                Text(
                    text = appointment.serviceType.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
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

