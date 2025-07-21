// ui/screens/pets/PetsScreen.kt
package cl.clinipets.ui.screens.pets

import android.util.Log
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.ui.viewmodels.PetsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddPet: () -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    val petsState by viewModel.petsState.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Mascotas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddPet) {
                Icon(Icons.Default.Add, contentDescription = "Agregar mascota")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showSearchBar) {
                OutlinedTextField(
                    value = petsState.searchQuery,
                    onValueChange = { viewModel.searchPets(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Buscar por nombre, raza o microchip...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (petsState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchPets("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
                )
            }

            if (petsState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (petsState.pets.isEmpty()) {
                EmptyPetsState(onAddPet = onNavigateToAddPet)
            } else {
                val displayPets = if (petsState.filteredPets.isNotEmpty() ||
                    petsState.searchQuery.isNotEmpty() ||
                    petsState.selectedSpeciesFilter != null) {
                    petsState.filteredPets
                } else {
                    petsState.pets
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayPets) { pet ->
                        PetCard(
                            pet = pet,
                            onClick = { onNavigateToPetDetail(pet.id) }
                        )
                    }
                }
            }
        }

        if (showFilterDialog) {
            FilterDialog(
                selectedSpecies = petsState.selectedSpeciesFilter,
                onSpeciesSelected = { species ->
                    viewModel.filterBySpecies(species)
                    showFilterDialog = false
                },
                onDismiss = { showFilterDialog = false }
            )
        }

        petsState.error?.let { error ->
            Log.e("PetsScreen", "Error: $error")
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearState() }) {
                        Text("Cerrar")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun PetCard(
    pet: Pet,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono según especie
            Icon(
                imageVector = when (pet.species) {
                    PetSpecies.DOG -> Icons.Default.Pets
                    PetSpecies.CAT -> Icons.Default.Pets
                    else -> Icons.Default.Pets
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${pet.species.name} - ${pet.breed}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                pet.birthDate?.let { birthDate ->
                    val age = calculateAge(birthDate)
                    Text(
                        text = "$age años",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun EmptyPetsState(onAddPet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tienes mascotas registradas",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Agrega tu primera mascota para comenzar",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddPet) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar mascota")
        }
    }
}

@Composable
private fun FilterDialog(
    selectedSpecies: PetSpecies?,
    onSpeciesSelected: (PetSpecies?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar por especie") },
        text = {
            Column {
                FilterChip(
                    selected = selectedSpecies == null,
                    onClick = { onSpeciesSelected(null) },
                    label = { Text("Todas") },
                    modifier = Modifier.fillMaxWidth()
                )
                PetSpecies.values().forEach { species ->
                    FilterChip(
                        selected = selectedSpecies == species,
                        onClick = { onSpeciesSelected(species) },
                        label = { Text(species.name) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private fun calculateAge(birthDate: Long): Int {
    val birth = Calendar.getInstance().apply { timeInMillis = birthDate }
    val now = Calendar.getInstance()
    var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return age
}

// ====================== AGREGAR/EDITAR MASCOTA ======================

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
    var selectedSex by remember { mutableStateOf(cl.clinipets.data.model.PetSex.MALE) }
    var neutered by remember { mutableStateOf(false) }
    var microchipId by remember { mutableStateOf("") }
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
            neutered = pet.neutered
            microchipId = pet.microchipId ?: ""
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
                                    neutered = neutered,
                                    microchipId = microchipId.ifBlank { null },
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
                                    neutered = neutered,
                                    microchipId = microchipId.ifBlank { null },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank()
                )
            }

            item {
                Text("Especie", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PetSpecies.values().forEach { species ->
                        FilterChip(
                            selected = selectedSpecies == species,
                            onClick = { selectedSpecies = species },
                            label = { Text(species.name) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Raza") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
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
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Sexo", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSex == cl.clinipets.data.model.PetSex.MALE,
                        onClick = { selectedSex = cl.clinipets.data.model.PetSex.MALE },
                        label = { Text("Macho") }
                    )
                    FilterChip(
                        selected = selectedSex == cl.clinipets.data.model.PetSex.FEMALE,
                        onClick = { selectedSex = cl.clinipets.data.model.PetSex.FEMALE },
                        label = { Text("Hembra") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Esterilizado/Castrado")
                    Switch(
                        checked = neutered,
                        onCheckedChange = { neutered = it }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = microchipId,
                    onValueChange = { microchipId = it },
                    label = { Text("Número de microchip") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (alergias, condiciones especiales, etc.)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
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
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearState() }) {
                        Text("Cerrar")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun DatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Implementación simple de DatePicker
    // En producción usar DatePicker de Material3
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar fecha") },
        text = {
            Text("Implementar DatePicker aquí")
        },
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(System.currentTimeMillis())
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