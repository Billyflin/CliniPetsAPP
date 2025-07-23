// ui/screens/pets/AddEditPetScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.PetSex
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.data.model.User
import cl.clinipets.ui.viewmodels.PetsViewModel
import cl.clinipets.ui.viewmodels.VetViewModel
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
    viewModel: PetsViewModel = hiltViewModel(),
    vetViewModel: VetViewModel = hiltViewModel(),
) {
    val petsState by viewModel.petsState.collectAsState()
    val isEditing = petId != null
    val isVet = vetViewModel.vetState.collectAsState().value.isVeterinarian

    var name by remember { mutableStateOf("") }
    var selectedSpecies by remember { mutableStateOf(PetSpecies.DOG) }
    var breed by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf<Long?>(null) }
    var weight by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf(PetSex.MALE) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Estados para la asignación de dueño
    var ownerSearchQuery by remember { mutableStateOf("") }
    var selectedOwnerId by remember { mutableStateOf<String?>(null) }
    var showOwnerDialog by remember { mutableStateOf(false) }

    // Cargar datos si es edición
    LaunchedEffect(petId) {
        if (isEditing && petId != null) {
            viewModel.loadPetDetail(petId)
        }
    }

    LaunchedEffect(isVet) {
        if (isVet) {
            viewModel.loadOwners()
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
            selectedOwnerId = pet.ownerId
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
                                    notes = notes,
                                    ownerId = if (isVet) selectedOwnerId else null
                                )
                            }
                        },
                        enabled = name.isNotBlank() &&
                                !petsState.isLoading &&
                                (!isVet || selectedOwnerId != null)
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
            // Si es veterinario y no es edición, mostrar selector de dueño
            if (isVet && !isEditing) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dueño de la mascota *", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = ownerSearchQuery,
                            onValueChange = { ownerSearchQuery = it },
                            label = { Text("Buscar por email o teléfono") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = selectedOwnerId != null,
                            trailingIcon = {
                                if (selectedOwnerId != null) {
                                    IconButton(onClick = {
                                        selectedOwnerId = null
                                        ownerSearchQuery = ""
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                    }
                                } else {
                                    IconButton(onClick = { showOwnerDialog = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                                    }
                                }
                            }
                        )

                        // Mostrar el usuario seleccionado
                        selectedOwnerId?.let { ownerId ->
                            val selectedUser = petsState.owners.find { it.id == ownerId }
                            selectedUser?.let { user ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = user.email ?: user.phone ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

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

        // Diálogo de búsqueda de dueños
        if (showOwnerDialog) {
            SearchOwnerDialog(
                searchQuery = ownerSearchQuery,
                owners = petsState.owners.filter { user ->
                    ownerSearchQuery.isBlank() ||
                            user.email.contains(ownerSearchQuery, ignoreCase = true) == true ||
                            user.phone?.contains(ownerSearchQuery) == true ||
                            user.name.contains(ownerSearchQuery, ignoreCase = true)
                },
                onUserSelected = { user ->
                    selectedOwnerId = user.id
                    ownerSearchQuery = user.email ?: user.phone ?: user.name
                    showOwnerDialog = false
                },
                onDismiss = { showOwnerDialog = false }
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
private fun SearchOwnerDialog(
    searchQuery: String,
    owners: List<User>,
    onUserSelected: (User) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredOwners = remember(searchQuery, owners) {
        owners.filter { owner ->
            searchQuery.isBlank() ||
                    owner.email.contains(searchQuery, ignoreCase = true) == true ||
                    owner.phone?.contains(searchQuery) == true ||
                    owner.name.contains(searchQuery, ignoreCase = true)
        }.take(5) // Limitar a 5 resultados para evitar problemas de tamaño
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Dueño") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredOwners.isEmpty()) {
                    Text(
                        text = "No se encontraron usuarios",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    filteredOwners.forEach { owner ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserSelected(owner) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = owner.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                owner.email?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                owner.phone?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (owners.size > 5 && filteredOwners.size == 5) {
                        Text(
                            text = "Mostrando solo los primeros 5 resultados...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
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