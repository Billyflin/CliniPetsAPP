package cl.clinipets.ui.agenda

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import cl.clinipets.openapi.models.CitaResponse
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.ServicioMedicoDto
import cl.clinipets.ui.agenda.BookingViewModel
import cl.clinipets.ui.agenda.CartItem

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    serviceId: String?, // Deprecated/Optional now, can be used to pre-select
    preselectedPetId: String? = null,
    onBack: () -> Unit,
    onAddPet: () -> Unit,
    onSuccess: (CitaResponse) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    
    // Initialize
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    // Pre-select logic if needed (omitted for brevity/cleanliness as VM handles defaults)

    // Date Picker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    LaunchedEffect(uiState.bookingResult) {
        uiState.bookingResult?.let {
            onSuccess(it)
            viewModel.resetBookingState()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDateTime.ofEpochSecond(millis / 1000, 0, ZoneOffset.UTC).toLocalDate()
                        viewModel.selectDate(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agendar Visita") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- SECTION 1: BUILD ORDER ---
                SectionTitle("1. Arma tu visita")
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pet Selector
                        var expandedPet by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedPet,
                            onExpandedChange = { expandedPet = !expandedPet }
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedPet?.nombre ?: "Selecciona una mascota",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mascota") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPet) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expandedPet, onDismissRequest = { expandedPet = false }) {
                                uiState.pets.forEach { pet ->
                                    DropdownMenuItem(
                                        text = { Text(pet.nombre) },
                                        onClick = {
                                            viewModel.selectPet(pet)
                                            expandedPet = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Nueva Mascota")
                                        }
                                    },
                                    onClick = {
                                        onAddPet()
                                        expandedPet = false
                                    }
                                )
                            }
                        }

                        // Service Selector
                        var expandedService by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedService,
                            onExpandedChange = { expandedService = !expandedService }
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedService?.nombre ?: "Selecciona un servicio",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Servicio") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedService) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expandedService, onDismissRequest = { expandedService = false }) {
                                uiState.services.forEach { service ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(service.nombre, style = MaterialTheme.typography.bodyLarge)
                                                Text("${service.precioBase}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectService(service)
                                            expandedService = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.addToCart() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.selectedPet != null && uiState.selectedService != null
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Agregar a la visita")
                        }
                    }
                }

                // Cart List
                if (uiState.cart.isNotEmpty()) {
                    Text("Servicios agregados:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.cart.forEach { item ->
                            CartItemCard(
                                item = item,
                                onRemove = { viewModel.removeFromCart(item) }
                            )
                        }
                        
                        // Totals
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tiempo total estim.: ${uiState.totalDuration} min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ${uiState.totalPrice}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // --- SECTION 2: AGENDA ---
                if (uiState.cart.isNotEmpty()) {
                    HorizontalDivider()
                    SectionTitle("2. Agenda tu visita")

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = uiState.selectedDate?.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.forLanguageTag("es-ES")))
                                ?: "Seleccionar Fecha"
                        )
                    }

                    if (uiState.selectedDate != null) {
                        if (uiState.availableSlots.isEmpty()) {
                            Text("No hay horas disponibles.", color = MaterialTheme.colorScheme.error)
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                uiState.availableSlots.forEach { slot ->
                                    FilterChip(
                                        selected = uiState.selectedSlot == slot,
                                        onClick = { viewModel.selectSlot(slot) },
                                        label = { Text(slot.format(timeFormatter)) },
                                        leadingIcon = if (uiState.selectedSlot == slot) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // Confirm Button
                    Button(
                        onClick = { viewModel.createReservation() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = uiState.selectedDate != null && uiState.selectedSlot != null
                    ) {
                        Text("Confirmar Reserva (${uiState.totalPrice})")
                    }
                } else {
                    Text(
                        "Agrega al menos un servicio para continuar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemCard(item: CartItem, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.servicio.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${item.mascota.nombre} (${item.mascota.especie})", style = MaterialTheme.typography.bodyMedium)
                Text("${item.precio}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}