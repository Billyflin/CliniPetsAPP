package cl.clinipets.ui.mascotas

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.MascotaCreateRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotaFormScreen(
    petId: String? = null,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: MascotaFormViewModel = hiltViewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var especie by remember { mutableStateOf(MascotaCreateRequest.Especie.PERRO) }
    var raza by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf(MascotaCreateRequest.Sexo.MACHO) }
    var esterilizado by remember { mutableStateOf(false) }
    var temperamento by remember { mutableStateOf(MascotaCreateRequest.Temperamento.DOCIL) }
    var chip by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(petId) {
        if (petId != null) {
            viewModel.cargarMascota(petId)
        }
    }

    LaunchedEffect(uiState.pet) {
        uiState.pet?.let { pet ->
            nombre = pet.nombre
            especie = MascotaCreateRequest.Especie.valueOf(pet.especie.name)
            raza = pet.raza
            sexo = MascotaCreateRequest.Sexo.valueOf(pet.sexo.name)
            esterilizado = pet.esterilizado
            temperamento = MascotaCreateRequest.Temperamento.valueOf(pet.temperamento.name)
            chip = pet.chipIdentificador ?: ""
        }
    }

    LaunchedEffect(uiState) {
        if (uiState.success) {
            val message = if (uiState.isEdit) "Mascota actualizada con éxito" else "Mascota creada con éxito"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onSuccess()
        }
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEdit) "Editar Mascota" else "Agregar Mascota") },
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Especie:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(MascotaCreateRequest.Especie.PERRO, MascotaCreateRequest.Especie.GATO).forEach { option ->
                        FilterChip(
                            selected = especie == option,
                            onClick = { 
                                especie = option
                                viewModel.cargarRazas(option)
                            },
                            label = { Text(option.name) },
                            enabled = !uiState.isEdit
                        )
                    }
                }

                // Raza Dropdown
                var expandedRaza by remember { mutableStateOf(false) }
                LaunchedEffect(especie) {
                    viewModel.cargarRazas(especie)
                }
                
                ExposedDropdownMenuBox(
                    expanded = expandedRaza,
                    onExpandedChange = { expandedRaza = !expandedRaza }
                ) {
                    OutlinedTextField(
                        value = raza,
                        onValueChange = { raza = it },
                        label = { Text("Raza") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRaza) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    if (uiState.razasDisponibles.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expandedRaza,
                            onDismissRequest = { expandedRaza = false }
                        ) {
                            uiState.razasDisponibles.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = {
                                        raza = opcion
                                        expandedRaza = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Sexo:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(MascotaCreateRequest.Sexo.MACHO, MascotaCreateRequest.Sexo.HEMBRA).forEach { option ->
                        FilterChip(
                            selected = sexo == option,
                            onClick = { sexo = option },
                            label = { Text(option.name) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("¿Esterilizado?", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = esterilizado,
                        onCheckedChange = { esterilizado = it }
                    )
                }

                OutlinedTextField(
                    value = chip,
                    onValueChange = { chip = it },
                    label = { Text("Chip Identificador (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Temperamento:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        MascotaCreateRequest.Temperamento.DOCIL,
                        MascotaCreateRequest.Temperamento.NERVIOSO,
                        MascotaCreateRequest.Temperamento.AGRESIVO
                    ).forEach { option ->
                        val color = when (option) {
                            MascotaCreateRequest.Temperamento.DOCIL -> MaterialTheme.colorScheme.primary
                            MascotaCreateRequest.Temperamento.NERVIOSO -> MaterialTheme.colorScheme.tertiary
                            MascotaCreateRequest.Temperamento.AGRESIVO -> MaterialTheme.colorScheme.error
                        }
                        FilterChip(
                            selected = temperamento == option,
                            onClick = { temperamento = option },
                            label = { Text(option.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (nombre.isNotBlank() && raza.isNotBlank()) {
                        viewModel.guardarMascota(
                            petId, nombre, especie,
                            raza, sexo, esterilizado, temperamento, chip
                        )
                    } else {
                        Toast.makeText(context, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (uiState.isEdit) "Actualizar Mascota" else "Guardar Mascota")
                }
            }
        }
    }
}
