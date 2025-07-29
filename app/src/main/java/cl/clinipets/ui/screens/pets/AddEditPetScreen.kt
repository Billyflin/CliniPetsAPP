package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.PetSex
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.data.model.User
import cl.clinipets.ui.viewmodels.PetsViewModel
import cl.clinipets.ui.viewmodels.VetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// package: cl.clinipets.ui.screens.pets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPetScreen(
    petId: String? = null,
    onPetSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    vm: PetsViewModel = hiltViewModel(),
    vetVm: VetViewModel = hiltViewModel()
) {
    val petState by vm.petsState.collectAsStateWithLifecycle()
    val vetState by vetVm.vetState.collectAsStateWithLifecycle()
    val editing = petId != null
    val isVet = vetState.isVeterinarian

    /* ---------- form state ---------- */
    var name by rememberSaveable { mutableStateOf("") }
    var species by rememberSaveable { mutableStateOf(PetSpecies.DOG) }
    var breed by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf(PetSex.MALE) }
    var weight by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var birthMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    var ownerId by rememberSaveable { mutableStateOf<String?>(null) }
    var ownerSearch by rememberSaveable { mutableStateOf("") }
    var showOwnerDlg by remember { mutableStateOf(false) }
    var showDateDlg by remember { mutableStateOf(false) }

    /* ---------- load existing pet ---------- */
    LaunchedEffect(petId) { if (editing) vm.loadPetDetail(petId!!) }
    LaunchedEffect(isVet) { if (isVet) vm.loadOwners() }

    LaunchedEffect(petState.selectedPet) {
        petState.selectedPet?.let { p ->
            name = p.name
            species = p.species
            breed = p.breed
            sex = p.sex
            weight = if (p.weight > 0f) p.weight.toString() else ""
            notes = p.notes
            birthMillis = p.birthDate
            ownerId = p.ownerId
        }
    }

    LaunchedEffect(petState.isPetAdded, petState.isPetUpdated) {
        if (petState.isPetAdded || petState.isPetUpdated) {
            vm.clearState(); onPetSaved()
        }
    }

    /* ---------- breed options ---------- */
    val dogBreeds = listOf(
        "Labrador Retriever", "Pastor Alemán", "Poodle", "Bulldog",
        "Beagle", "Chihuahua", "Akita", "Otro"
    )
    val catBreeds = listOf("Doméstico Pelo Corto", "Doméstico Pelo Largo", "Siamés", "Persa")

    val breedOptions = if (species == PetSpecies.CAT) catBreeds else dogBreeds
    var breedExpanded by remember { mutableStateOf(false) }

    /* ---------- UI ---------- */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Editar Mascota" else "Nueva Mascota") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                },
                actions = {
                    // Solo requiere nombre para guardar (dueño es opcional para vets)
                    val saveEnabled = name.isNotBlank() && !petState.isLoading
                    TextButton(
                        enabled = saveEnabled,
                        onClick = {
                            val w = weight.toFloatOrNull() ?: 0f
                            if (editing) {
                                vm.updatePet(
                                    petId!!, name, species, breed, birthMillis,
                                    w, sex, notes
                                )
                            } else {
                                vm.addPet(
                                    if (isVet) ownerId else null,
                                    name, species, breed, birthMillis,
                                    w, sex, notes,
                                )
                            }
                        }
                    ) { Text("Guardar") }
                }
            )
        }
    ) { pv ->
        LazyColumn(
            modifier = Modifier
                .padding(pv)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /* dueño (solo vet) */
            if (isVet && !editing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Dueño",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "Opcional - Se puede asignar después",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                }
                                if (ownerId != null) {
                                    AssistChip(
                                        onClick = { ownerId = null; ownerSearch = "" },
                                        label = { Text("Quitar") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = ownerSearch,
                                onValueChange = { ownerSearch = it },
                                label = { Text("Buscar dueño") },
                                readOnly = ownerId != null,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { showOwnerDlg = true }) {
                                        Icon(Icons.Default.PersonSearch, null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Información Básica",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre de la mascota *") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Especie",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PetSpecies.values().forEach { petSpecies ->
                                FilterChip(
                                    selected = species == petSpecies,
                                    onClick = { species = petSpecies },
                                    label = {
                                        Text(
                                            when (petSpecies) {
                                                PetSpecies.DOG -> "🐕 Perro"
                                                PetSpecies.CAT -> "🐈 Gato"
                                                else -> "🦜 Otro"
                                            }
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = breedExpanded,
                            onExpandedChange = { breedExpanded = !breedExpanded }
                        ) {
                            OutlinedTextField(
                                value = breed,
                                onValueChange = { breed = it },
                                label = { Text("Raza") },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(breedExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = breedExpanded,
                                onDismissRequest = { breedExpanded = false }
                            ) {
                                breedOptions.forEach { breedOption ->
                                    DropdownMenuItem(
                                        text = { Text(breedOption) },
                                        onClick = { breed = breedOption; breedExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Detalles Médicos",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = birthMillis?.let {
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                                        Date(
                                            it
                                        )
                                    )
                                } ?: "",
                                onValueChange = {},
                                label = { Text("Fecha de nacimiento") },
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = { showDateDlg = true }) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = weight,
                                onValueChange = {
                                    weight = it.filter { c -> c.isDigit() || c == '.' }
                                },
                                label = { Text("Peso (kg)") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Sexo",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = sex == PetSex.MALE,
                                onClick = { sex = PetSex.MALE },
                                label = { Text("♂ Macho") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                                )
                            )
                            FilterChip(
                                selected = sex == PetSex.FEMALE,
                                onClick = { sex = PetSex.FEMALE },
                                label = { Text("♀ Hembra") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                                )
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notas adicionales") },
                            placeholder = { Text("Alergias, condiciones especiales, comportamiento...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            maxLines = 5
                        )
                    }
                }
            }
        }

        /* dialogs */
        if (showDateDlg) {
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = birthMillis ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDateDlg = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            birthMillis = dateState.selectedDateMillis
                            showDateDlg = false
                        }
                    ) { Text("Seleccionar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDateDlg = false }) { Text("Cancelar") }
                }
            ) { DatePicker(state = dateState) }
        }

        if (showOwnerDlg) {
            SearchOwnerDialog(
                search = ownerSearch,
                owners = petState.owners,
                onSelect = {
                    ownerId = it.id
                    ownerSearch = "${it.name} - ${it.email ?: it.phone ?: ""}"
                    showOwnerDlg = false
                },
                onDismiss = { showOwnerDlg = false }
            )
        }

        petState.error?.let {
            AlertDialog(
                onDismissRequest = { vm.clearState() },
                confirmButton = {
                    TextButton(onClick = { vm.clearState() }) { Text("Entendido") }
                },
                title = { Text("Error") },
                text = { Text(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchOwnerDialog(
    search: String,
    owners: List<User>,
    onSelect: (User) -> Unit,
    onDismiss: () -> Unit
) {
    val list = remember(search, owners) {
        owners.filter {
            search.isBlank() ||
                    it.email?.contains(search, true) == true ||
                    it.phone?.contains(search) == true ||
                    it.name.contains(search, true)
        }.take(20)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                TopAppBar(
                    title = { Text("Seleccionar Dueño") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                )

                if (list.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Sin resultados",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn {
                        items(list) { owner ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        owner.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        owner.email?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        owner.phone?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            owner.name.first().uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                },
                                modifier = Modifier.clickable { onSelect(owner) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}