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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

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
    var fechaNacimiento by rememberSaveable { mutableStateOf("") } // Sigue siendo String para el TextField
    var esFechaAproximada by rememberSaveable { mutableStateOf(false) }
    var pesoKg by rememberSaveable { mutableStateOf("") }
    var pelaje by remember { mutableStateOf<CrearMascota.Pelaje?>(null) }
    var patron by remember { mutableStateOf<CrearMascota.Patron?>(null) }
    var colores by remember { mutableStateOf(setOf<CrearMascota.Colores>()) }
    // --- Fin Estados ---

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
            // --- INICIO CORRECCIÓN ---
            nombre = mascota.nombre
            especie = CrearMascota.Especie.valueOf(mascota.especie.value)
            selectedRaza = mascota.raza
            sexo = mascota.sexo?.value?.let { CrearMascota.Sexo.valueOf(it) } ?: CrearMascota.Sexo.MACHO
            // CORRECCIÓN: 'fechaNacimiento' es LocalDate?, convertir a String para el TextField
            fechaNacimiento = mascota.fechaNacimiento?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""
            esFechaAproximada = mascota.esFechaAproximada
            pesoKg = mascota.pesoKg?.toString().orEmpty()
            pelaje = mascota.pelaje?.value?.let { CrearMascota.Pelaje.valueOf(it) }
            patron = mascota.patron?.value?.let { CrearMascota.Patron.valueOf(it) }
            colores = mascota.colores.mapNotNull { it.value?.let { v -> CrearMascota.Colores.valueOf(v) } }.toSet()
            // --- FIN CORRECCIÓN ---

            didPrefill = true
        }
    }

    LaunchedEffect(especie) {
        // --- INICIO CORRECCIÓN ---
        val dtoEspecie = ListarRazasRequest.Especie.valueOf(especie.value)
        vm.cargarRazas(dtoEspecie)
        // --- FIN CORRECCIÓN ---
    }
    // --- Fin Lógica de Carga ---

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
                    val dtoRazaId = selectedRaza?.id
                    val dtoPeso = pesoKg.toDoubleOrNull()
                    // --- CORRECCIÓN: Convertir el String del TextField a LocalDate? ---
                    val dtoFecha = try { LocalDate.parse(fechaNacimiento) } catch (e: Exception) { null }

                    if (isEditing) {
                        vm.actualizar(
                            mascotaId!!,
                            ActualizarMascota(
                                nombre = nombre,
                                razaId = dtoRazaId,
                                // --- CORRECCIÓN: Mapear enums de 'Crear' a 'Actualizar' ---
                                sexo = ActualizarMascota.Sexo.valueOf(sexo.value),
                                fechaNacimiento = dtoFecha, // <-- Corregido
                                esFechaAproximada = esFechaAproximada,
                                pesoKg = dtoPeso,
                                pelaje = pelaje?.let { ActualizarMascota.Pelaje.valueOf(it.value) },
                                patron = patron?.let { ActualizarMascota.Patron.valueOf(it.value) },
                                colores = colores.map { ActualizarMascota.Colores.valueOf(it.value) }.toSet()
                            )
                        )
                    } else {
                        vm.crear(
                            CrearMascota(
                                nombre = nombre,
                                especie = especie,
                                razaId = dtoRazaId,
                                sexo = sexo,
                                fechaNacimiento = dtoFecha, // <-- Corregido
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
                containerColor = if (nombre.isBlank() || cargando) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
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
                    // --- CORRECCIÓN: Añadido Box ---
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nombre.isBlank()
                )
            }

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

            item {
                OutlinedTextField(
                    value = pesoKg,
                    onValueChange = { pesoKg = it.replace(",",".") },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            item {
                OutlinedTextField(
                    value = fechaNacimiento,
                    onValueChange = { fechaNacimiento = it },
                    label = { Text("Fecha Nacimiento (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
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

            item { Text("Pelaje", style = MaterialTheme.typography.bodyLarge) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrearMascota.Pelaje.entries.forEach { p ->
                        FilterChip(
                            selected = (p == pelaje),
                            onClick = { pelaje = if (pelaje == p) null else p },
                            label = { Text(p.value) }
                        )
                    }
                }
            }

            item { Text("Patrón", style = MaterialTheme.typography.bodyLarge) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrearMascota.Patron.entries.forEach { p ->
                        FilterChip(
                            selected = (p == patron),
                            onClick = { patron = if (patron == p) null else p },
                            label = { Text(p.value) }
                        )
                    }
                }
            }

            item { Text("Colores", style = MaterialTheme.typography.bodyLarge) }
            item {
                // --- CORRECCIÓN: Typo 'CrearMarcota' ---
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrearMascota.Colores.entries.forEach { c ->
                        FilterChip(
                            selected = (colores.contains(c)),
                            onClick = {
                                colores = if (colores.contains(c)) colores - c else colores + c
                            },
                            label = { Text(c.value) } // <-- '.value' es correcto
                        )
                    }
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
                .menuAnchor(),
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
            razas.filterNot { it.nombre.equals("Mestizo", ignoreCase = true) }
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