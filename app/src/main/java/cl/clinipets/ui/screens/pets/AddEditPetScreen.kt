// ui/screens/pets/AddEditPetScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.PetSex
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.ui.viewmodels.PetsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPetScreen(
    petId: String? = null,
    onPetSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    val petsState by viewModel.petsState.collectAsState()
    val isEditing = petId != null

    var name by remember { mutableStateOf("") }
    var selectedSpecies by remember { mutableStateOf(PetSpecies.DOG) }
    var breed by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf<Long?>(null) }
    var weight by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf(PetSex.MALE) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Cargar datos si es edición
    LaunchedEffect(petId) {
        if (isEditing && petId != null) {
            viewModel.loadPetDetail(petId)
        }
    }

    LaunchedEffect(petsState.selectedPet) {
        petsState.selectedPet?.let { pet ->
            name = pet.name
            selectedSpecies = pet.species
            breed = pet.breed
            birthDate = pet.birthDate
            weight = pet.weight.toString()
            selectedSex = pet.sex
            notes = pet.notes
        }
    }

    LaunchedEffect(petsState.isPetAdded, petsState.isPetUpdated) {
        if (petsState.isPetAdded || petsState.isPetUpdated) {
            viewModel.clearState()
            onPetSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Mascota" else "Agregar Mascota") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val weightFloat = weight.toFloatOrNull() ?: 0f
                            if (isEditing && petId != null) {
                                viewModel.updatePet(
                                    petId = petId,
                                    name = name,
                                    species = selectedSpecies,
                                    breed = breed,
                                    birthDate = birthDate,
                                    weight = weightFloat,
                                    sex = selectedSex,
                                    notes = notes
                                )
                            } else {
                                viewModel.addPet(
                                    name = name,
                                    species = selectedSpecies,
                                    breed = breed,
                                    birthDate = birthDate,
                                    weight = weightFloat,
                                    sex = selectedSex,
                                    notes = notes
                                )
                            }
                        },
                        enabled = name.isNotBlank() && !petsState.isLoading
                    ) {
                        if (petsState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre *") },
                modifier = Modifier.fillMaxWidth(),
                isError = name.isBlank()
            )

            // Especie
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Especie", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PetSpecies.values().forEach { species ->
                            FilterChip(
                                selected = selectedSpecies == species,
                                onClick = { selectedSpecies = species },
                                label = {
                                    Text(
                                        when (species) {
                                            PetSpecies.DOG -> "Perro"
                                            PetSpecies.CAT -> "Gato"
                                            PetSpecies.OTHER -> "Otro"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                label = { Text("Raza") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = birthDate?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                } ?: "",
                onValueChange = { },
                label = { Text("Fecha de nacimiento") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Seleccionar fecha")
                    }
                }
            )

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Peso (kg)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Sexo
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sexo", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSex == PetSex.MALE,
                            onClick = { selectedSex = PetSex.MALE },
                            label = { Text("Macho") }
                        )
                        FilterChip(
                            selected = selectedSex == PetSex.FEMALE,
                            onClick = { selectedSex = PetSex.FEMALE },
                            label = { Text("Hembra") }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (alergias, condiciones, etc.)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDateSelected = { date ->
                    birthDate = date
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        petsState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearState() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearState() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun DatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Implementación simple de DatePicker
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar fecha") },
        text = {
            Column {
                Text("Selecciona la fecha de nacimiento")
                // En producción usar DatePicker de Material3
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Por ahora, seleccionar hace 1 año
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.YEAR, -1)
                onDateSelected(calendar.timeInMillis)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}