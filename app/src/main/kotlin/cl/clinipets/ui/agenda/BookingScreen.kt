package cl.clinipets.ui.agenda

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import cl.clinipets.openapi.models.CitaResponse
import cl.clinipets.ui.features.booking.CartItem
import java.time.ZoneId

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
                // --- SECTION 1: SELECCIONAR MASCOTAS ---
                SectionTitle("1. ¿A quién atenderemos?")
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.pets.forEach { pet ->
                            val isSelected = uiState.selectedPets.contains(pet.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.togglePetSelection(pet.id) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.togglePetSelection(pet.id) }
                                )
                                Text(pet.nombre, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Text(pet.especie.name.lowercase().capitalize(), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        TextButton(onClick = onAddPet, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Agregar otra mascota")
                        }
                    }
                }

                // --- SECTION 2: SELECCIONAR SERVICIOS POR MASCOTA ---
                if (uiState.selectedPets.isNotEmpty()) {
                    SectionTitle("2. Elige los servicios")
                    
                    uiState.pets.filter { uiState.selectedPets.contains(it.id) }.forEach { pet ->
                        PetServiceSelection(
                            pet = pet,
                            availableServices = viewModel.getFilteredServicesForPet(pet),
                            currentCart = uiState.cart.filter { it.mascota.id == pet.id },
                            onAddService = { viewModel.addServiceToPet(pet, it) },
                            onRemoveService = { viewModel.removeServiceFromPet(it) }
                        )
                    }
                }

                // --- SECTION 3: AGENDA ---
                if (uiState.cart.isNotEmpty()) {
                    HorizontalDivider()
                    SectionTitle("3. Agenda tu visita")

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
                            Text("No hay horas disponibles para esta duración.", color = MaterialTheme.colorScheme.error)
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                uiState.availableSlots.forEach { slot ->
                                    val localTime = slot.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime()
                                    val formattedTime = localTime.format(timeFormatter)
                                    
                                    FilterChip(
                                        selected = uiState.selectedSlot == slot,
                                        onClick = { viewModel.selectSlot(slot) },
                                        label = { Text(formattedTime) },
                                        leadingIcon = if (uiState.selectedSlot == slot) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // --- SUMMARY SECTION ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Resumen de Cita", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Duración estimada: ${uiState.totalDuration} min")
                            Text("Total a pagar: $${uiState.totalPrice}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Button(
                        onClick = { viewModel.createReservation() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = uiState.selectedDate != null && uiState.selectedSlot != null && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Confirmar Cita")
                        }
                    }
                } else if (uiState.selectedPets.isNotEmpty()) {
                    Text(
                        "Selecciona al menos un servicio para tus mascotas.",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetServiceSelection(
    pet: cl.clinipets.openapi.models.MascotaResponse,
    availableServices: List<cl.clinipets.openapi.models.ServicioMedicoDto>,
    currentCart: List<CartItem>,
    onAddService: (cl.clinipets.openapi.models.ServicioMedicoDto) -> Unit,
    onRemoveService: (CartItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "Servicios para ${pet.nombre}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        
        // List already selected services for this pet
        currentCart.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(item.servicio.nombre, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemoveService(item) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Dropdown to add more
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Añadir servicio a ${pet.nombre}") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                textStyle = MaterialTheme.typography.bodySmall
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                availableServices.forEach { service ->
                    DropdownMenuItem(
                        text = { Text("${service.nombre} ($${service.precioBase})") },
                        onClick = {
                            onAddService(service)
                            expanded = false
                        }
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
                Text("$${item.precio}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}