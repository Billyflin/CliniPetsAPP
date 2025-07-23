package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.data.model.PetSex
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.data.model.User
import cl.clinipets.ui.viewmodels.PetsViewModel
import cl.clinipets.ui.viewmodels.VetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPetScreen(
    petId: String? = null,
    onPetSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    vm: PetsViewModel = hiltViewModel(),
    vetVm: VetViewModel = hiltViewModel()
) {
    val petState by vm.petsState.collectAsState()
    val vetState by vetVm.vetState.collectAsState()
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
                    val saveEnabled = name.isNotBlank()
                            && (!isVet || ownerId != null)
                            && !petState.isLoading
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /* dueño (solo vet) */
            if (isVet && !editing) {
                item {
                    OwnerSelector(
                        ownerSearch,
                        { ownerSearch = it },
                        ownerId,
                        petState.owners,
                        onClear = { ownerId = null; ownerSearch = "" },
                        onOpen = { showOwnerDlg = true }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SpeciesChips(species) { species = it } }

            item {
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
                        breedOptions.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = { breed = it; breedExpanded = false }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = birthMillis?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "",
                    onValueChange = {},
                    label = { Text("Nacimiento") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDateDlg = true }) {
                            Icon(Icons.Default.CalendarMonth, null)
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SexChips(sex) { sex = it } }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                )
            }
        }

        /* dialogs */

        if (showDateDlg) {
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = birthMillis
                    ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDateDlg = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            birthMillis = dateState.selectedDateMillis
                            showDateDlg = false
                        }
                    ) { Text("OK") }
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
                    ownerSearch = it.email ?: it.phone ?: it.name
                    showOwnerDlg = false
                },
                onDismiss = { showOwnerDlg = false }
            )
        }

        petState.error?.let {
            AlertDialog(
                onDismissRequest = { vm.clearState() },
                confirmButton = {
                    TextButton(onClick = { vm.clearState() }) { Text("OK") }
                },
                title = { Text("Error") },
                text = { Text(it) }
            )
        }
    }
}

/* ---------------------------------------------------------------------- */

@Composable
private fun SpeciesChips(sel: PetSpecies, onSel: (PetSpecies) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Especie", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PetSpecies.values().forEach {
                    FilterChip(
                        selected = sel == it,
                        onClick = { onSel(it) },
                        label = {
                            Text(
                                when (it) {
                                    PetSpecies.DOG -> "Perro"
                                    PetSpecies.CAT -> "Gato"
                                    else -> "Otro"
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SexChips(sel: PetSex, onSel: (PetSex) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Sexo", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sel == PetSex.MALE,
                    onClick = { onSel(PetSex.MALE) },
                    label = { Text("Macho") }
                )
                FilterChip(
                    selected = sel == PetSex.FEMALE,
                    onClick = { onSel(PetSex.FEMALE) },
                    label = { Text("Hembra") }
                )
            }
        }
    }
}

/* ---------- dueño ---------- */

@Composable
private fun OwnerSelector(
    query: String,
    onQuery: (String) -> Unit,
    ownerId: String?,
    owners: List<User>,
    onClear: () -> Unit,
    onOpen: () -> Unit
) {
    Column {
        Text("Dueño *", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Buscar dueño") },
            readOnly = ownerId != null,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (ownerId != null) {
                    IconButton(onClick = onClear) { Icon(Icons.Default.Close, null) }
                } else {
                    IconButton(onClick = onOpen) { Icon(Icons.Default.PersonSearch, null) }
                }
            }
        )
    }
}

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
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Seleccionar Dueño") },
        text = {
            if (list.isEmpty()) {
                Text("Sin resultados")
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    list.forEach {
                        ListItem(
                            headlineContent = { Text(it.name) },
                            supportingContent = {
                                Text(
                                    it.email ?: it.phone ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.clickable { onSelect(it) }
                        )
                    }
                }
            }
        }
    )
}
