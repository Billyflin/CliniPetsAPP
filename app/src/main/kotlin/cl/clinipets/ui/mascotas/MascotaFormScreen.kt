package cl.clinipets.ui.mascotas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.ListarRazasRequest
import cl.clinipets.openapi.models.Raza
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val UI_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MascotaFormScreen(
    mascotaId: UUID?,
    onBack: () -> Unit,
    vm: MascotasViewModel = hiltViewModel()
) {
    val error by vm.error.collectAsState()
    val cargando by vm.cargando.collectAsState()
    val mascotaSeleccionada by vm.seleccionada.collectAsState()
    val razas by vm.razas.collectAsState()

    val isEditing = mascotaId != null
    var didPrefill by rememberSaveable { mutableStateOf(false) }

    // --- Estados del Formulario ---
    var nombre by rememberSaveable { mutableStateOf("") }
    var especie by rememberSaveable { mutableStateOf(CrearMascota.Especie.PERRO) }
    var selectedRaza by remember { mutableStateOf<Raza?>(null) }
    var sexo by rememberSaveable { mutableStateOf(CrearMascota.Sexo.MACHO) }
    var fechaNacimiento by rememberSaveable { mutableStateOf("") }
    var esFechaAproximada by rememberSaveable { mutableStateOf(false) }
    var pesoKg by rememberSaveable { mutableStateOf("") }
    var pelaje by remember { mutableStateOf<CrearMascota.Pelaje?>(null) }
    var patron by remember { mutableStateOf<CrearMascota.Patron?>(null) }
    var colores by remember { mutableStateOf(setOf<CrearMascota.Colores>()) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    // NUEVO: Estado para controlar si el campo "nombre" ha sido tocado
    var nombreIsDirty by rememberSaveable { mutableStateOf(false) }
    // --- Fin Estados ---

    // === Lógica de Validación ===
    // Define si el nombre es válido (requisito obligatorio)
    val isNombreValido = nombre.isNotBlank()
    // Define si se debe mostrar el error: solo si el campo ha sido tocado Y no es válido
    val showNombreError = nombreIsDirty && !isNombreValido
    // ===========================

    val handleBack = {
        vm.limpiarSeleccion()
        onBack()
    }

    // --- Lógica de Carga y Pre-llenado ---
    LaunchedEffect(mascotaId) {
        if (isEditing && !didPrefill) {
            vm.detalle(mascotaId!!)
        }
    }

    LaunchedEffect(mascotaSeleccionada) {
        val mascota = mascotaSeleccionada
        if (isEditing && mascota != null && mascota.id == mascotaId && !didPrefill) {
            nombre = mascota.nombre
            especie = CrearMascota.Especie.valueOf(mascota.especie.value)
            selectedRaza = mascota.raza
            sexo = mascota.sexo?.value?.let { CrearMascota.Sexo.valueOf(it) }
                ?: CrearMascota.Sexo.MACHO
            fechaNacimiento = mascota.fechaNacimiento?.format(UI_DATE_FORMATTER) ?: ""
            esFechaAproximada = mascota.esFechaAproximada
            pesoKg = mascota.pesoKg?.toString().orEmpty()
            pelaje = mascota.pelaje?.value?.let { CrearMascota.Pelaje.valueOf(it) }
            patron = mascota.patron?.value?.let { CrearMascota.Patron.valueOf(it) }
            colores =
                mascota.colores.mapNotNull { it.value?.let { v -> CrearMascota.Colores.valueOf(v) } }
                    .toSet()

            didPrefill = true
            nombreIsDirty = true // CAMBIO: Si estamos editando, asumimos que el campo ya está "sucio"
        }
    }

    LaunchedEffect(especie) {
        val dtoEspecie = ListarRazasRequest.Especie.valueOf(especie.value)
        vm.cargarRazas(dtoEspecie)
    }
    // --- Fin Lógica de Carga ---

    // --- Lógica del DatePicker ---
    val selectedDateMillis = remember(fechaNacimiento) {
        if (fechaNacimiento.isBlank()) null
        else {
            try {
                LocalDate.parse(fechaNacimiento, UI_DATE_FORMATTER)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                null
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis ?: Instant.now().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            fechaNacimiento = selectedDate.format(UI_DATE_FORMATTER)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    // --- FIN DatePicker ---

    // --- Lógica del ColorPicker (Diálogo) ---
    if (showColorPicker) {
        MultiSelectDialog(
            title = "Seleccionar Colores",
            options = CrearMascota.Colores.entries,
            selected = colores,
            onDismiss = { showColorPicker = false },
            onConfirm = { nuevosColores ->
                colores = nuevosColores
                showColorPicker = false
            },
            optionToString = { it.value }
        )
    }
    // --- FIN ColorPicker ---


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Mascota" else "Nueva Mascota") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // CAMBIO: Si el formulario no es válido...
                    if (!isNombreValido || cargando) {
                        // NUEVO: ...forzamos que el campo "nombre" se marque como "sucio"...
                        nombreIsDirty = true
                        // ...y detenemos la ejecución.
                        return@FloatingActionButton
                    }

                    // Si llegamos aquí, el formulario es válido
                    val dtoRazaId = selectedRaza?.id
                    val dtoPeso = pesoKg.toDoubleOrNull()
                    val dtoFecha = try {
                        if (fechaNacimiento.isBlank()) null
                        else LocalDate.parse(fechaNacimiento, UI_DATE_FORMATTER)
                    } catch (e: Exception) {
                        null
                    }

                    if (isEditing) {
                        vm.actualizar(
                            mascotaId!!,
                            ActualizarMascota(
                                nombre = nombre,
                                razaId = dtoRazaId,
                                sexo = ActualizarMascota.Sexo.valueOf(sexo.value),
                                fechaNacimiento = dtoFecha,
                                esFechaAproximada = esFechaAproximada,
                                pesoKg = dtoPeso,
                                pelaje = pelaje?.let { ActualizarMascota.Pelaje.valueOf(it.value) },
                                patron = patron?.let { ActualizarMascota.Patron.valueOf(it.value) },
                                colores = colores.map { ActualizarMascota.Colores.valueOf(it.value) }
                                    .toSet()
                            )
                        )
                    } else {
                        vm.crear(
                            CrearMascota(
                                nombre = nombre,
                                especie = especie,
                                razaId = dtoRazaId,
                                sexo = sexo,
                                fechaNacimiento = dtoFecha,
                                esFechaAproximada = esFechaAproximada,
                                pesoKg = dtoPeso,
                                pelaje = pelaje,
                                patron = patron,
                                colores = colores
                            )
                        )
                    }
                    handleBack()
                },
                // CAMBIO: Usar la variable de validación para el color
                containerColor = if (!isNombreValido || cargando) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                if (cargando && !didPrefill) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Guardar")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (cargando && isEditing && !didPrefill) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // --- Título de Sección ---
            item {
                Text(
                    "Información Básica",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // === CAMBIO: Campo Nombre con Mensaje de Error ===
            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        // NUEVO: Marcar el campo como "sucio" en cuanto el usuario escribe
                        nombreIsDirty = true
                    },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    // CAMBIO: El error se muestra solo si 'showNombreError' es verdadero
                    isError = showNombreError,
                    // NUEVO: Mensaje de error descriptivo
                    supportingText = {
                        if (showNombreError) {
                            Text("El nombre es un campo obligatorio")
                        }
                    }
                )
            }
            // === FIN CAMBIO ===

            item {
                Text("Especie *", style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrearMascota.Especie.entries.forEach { e ->
                        FilterChip(
                            selected = (e == especie),
                            onClick = {
                                if (e != especie) selectedRaza = null
                                especie = e
                            },
                            label = { Text(e.value) },
                            enabled = !isEditing
                        )
                    }
                }
            }

            item {
                RazaSelector(
                    razas = razas,
                    selected = selectedRaza,
                    onSelected = { selectedRaza = it },
                    enabled = razas.isNotEmpty()
                )
            }

            item {
                Text("Sexo", style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrearMascota.Sexo.entries.forEach { s ->
                        FilterChip(
                            selected = (s == sexo),
                            onClick = { sexo = s },
                            label = { Text(s.value) }
                        )
                    }
                }
            }

            // --- Título de Sección ---
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Detalles Físicos y Salud",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = pesoKg,
                    onValueChange = { pesoKg = it.replace(",", ".") },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Campo fecha con calendario
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = fechaNacimiento,
                        onValueChange = {}, // No editable manualmente
                        label = { Text("Fecha Nacimiento") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Seleccionar fecha"
                            )
                        }
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = esFechaAproximada,
                        onCheckedChange = { esFechaAproximada = it }
                    )
                    Text(
                        text = "La fecha de nacimiento es aproximada",
                        modifier = Modifier.clickable { esFechaAproximada = !esFechaAproximada }
                    )
                }
            }

            item {
                EnumDropdownSelector(
                    label = "Pelaje",
                    options = CrearMascota.Pelaje.entries,
                    selected = pelaje,
                    onSelected = { pelaje = it },
                    optionToString = { it.value },
                    enabled = true
                )
            }

            item {
                EnumDropdownSelector(
                    label = "Patrón",
                    options = CrearMascota.Patron.entries,
                    selected = patron,
                    onSelected = { patron = it },
                    optionToString = { it.value },
                    enabled = true
                )
            }

            item {
                val coloresText = if (colores.isEmpty()) {
                    "Selecciona colores"
                } else {
                    colores.sortedBy { it.value }.joinToString(", ") { it.value }
                }

                Box(modifier = Modifier.clickable { showColorPicker = true }) {
                    OutlinedTextField(
                        value = coloresText,
                        onValueChange = {},
                        label = { Text("Colores") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Abrir selector de colores")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }


            if (error != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { vm.limpiarError() }) { Text("Entendido") }
                }
            }

            item { Spacer(Modifier.height(80.dp)) } // Espacio para el FAB
        }
    }
}

// ... (El resto de los Composables: RazaSelector, EnumDropdownSelector, MultiSelectDialog quedan exactamente igual)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RazaSelector(
    razas: List<Raza>,
    selected: Raza?,
    onSelected: (Raza?) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.nombre ?: "Selecciona una raza",
            onValueChange = {},
            readOnly = true,
            label = { Text("Raza") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled
                ),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Ninguna (Mestizo)") },
                onClick = {
                    onSelected(razas.find { it.nombre.equals("Mestizo", ignoreCase = true) })
                    expanded = false
                }
            )
            razas
                .filterNot { it.nombre.equals("Mestizo", ignoreCase = true) }
                .forEach { raza ->
                    DropdownMenuItem(
                        text = { Text(raza.nombre) },
                        onClick = {
                            onSelected(raza)
                            expanded = false
                        }
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <E> EnumDropdownSelector(
    label: String,
    options: List<E>,
    selected: E?,
    onSelected: (E?) -> Unit,
    optionToString: (E) -> String,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = selected?.let { optionToString(it) } ?: "Selecciona"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled
                ),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("No especificado") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionToString(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <E> MultiSelectDialog(
    title: String,
    options: List<E>,
    selected: Set<E>,
    onDismiss: () -> Unit,
    onConfirm: (Set<E>) -> Unit,
    optionToString: (E) -> String
) {
    var tempSelected by remember { mutableStateOf(selected) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options) { option ->
                    val isSelected = tempSelected.contains(option)
                    ListItem(
                        headlineContent = { Text(optionToString(option)) },
                        leadingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                        },
                        modifier = Modifier.clickable {
                            tempSelected = if (isSelected) {
                                tempSelected - option
                            } else {
                                tempSelected + option
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(tempSelected)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}