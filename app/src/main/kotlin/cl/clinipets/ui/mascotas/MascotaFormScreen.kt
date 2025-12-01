package cl.clinipets.ui.mascotas

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.MascotaCreateRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.material.icons.filled.CalendarToday
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotaFormScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: MascotaFormViewModel = hiltViewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var especie by remember { mutableStateOf(MascotaCreateRequest.Especie.PERRO) }
    var peso by remember { mutableStateOf("") }
    var fechaNacimientoStr by remember { mutableStateOf("") } 

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // State for date picker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is MascotaFormUiState.Success -> {
                Toast.makeText(context, "Mascota creada con éxito", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            is MascotaFormUiState.Error -> {
                Toast.makeText(context, (uiState as MascotaFormUiState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        fechaNacimientoStr = date.toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Mascota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Especie:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(MascotaCreateRequest.Especie.PERRO, MascotaCreateRequest.Especie.GATO).forEach { option ->
                    FilterChip(
                        selected = especie == option,
                        onClick = { especie = option },
                        label = { Text(option.name) }
                    )
                }
            }

            OutlinedTextField(
                value = peso,
                onValueChange = { peso = it },
                label = { Text("Peso (Kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = fechaNacimientoStr,
                onValueChange = { }, 
                label = { Text("Fecha Nacimiento") },
                placeholder = { Text("Selecciona una fecha") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val pesoVal = peso.toDoubleOrNull()
                    val fechaVal = try {
                        LocalDate.parse(fechaNacimientoStr)
                    } catch (e: Exception) {
                        null
                    }

                    if (nombre.isNotBlank() && pesoVal != null && fechaVal != null) {
                        viewModel.crearMascota(nombre, especie, pesoVal, fechaVal)
                    } else {
                        Toast.makeText(context, "Datos inválidos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is MascotaFormUiState.Loading
            ) {
                if (uiState is MascotaFormUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar Mascota")
                }
            }
        }
    }
}
