// ui/screens/pets/PetsScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsScreen(
    onNavigateToPetDetail: (String) -> Unit,
    onNavigateToNewPet: () -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extColors = LocalExtendedColors.current
    var searchQuery by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mis Mascotas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                        Icon(Icons.Outlined.FilterList, contentDescription = "Filtrar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewPet,
                containerColor = extColors.mint.color,
                contentColor = extColors.mint.onColor,
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp)
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Mascota")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar mascota...") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                singleLine = true
            )

            // Filter Chips
            AnimatedVisibility(
                visible = showFilterMenu,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilterChipsRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterChanged = { viewModel.updateFilter(it) }
                )
            }

            // Pets List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.pets.isEmpty()) {
                EmptyPetsState(onAddPet = onNavigateToNewPet)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.pets.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.breed.contains(searchQuery, ignoreCase = true)
                        },
                        key = { it.id }
                    ) { pet ->
                        PetCard(
                            pet = pet,
                            onClick = { onNavigateToPetDetail(pet.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: PetFilter,
    onFilterChanged: (PetFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == PetFilter.ALL,
            onClick = { onFilterChanged(PetFilter.ALL) },
            label = { Text("Todas") },
            leadingIcon = if (selectedFilter == PetFilter.ALL) {
                { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == PetFilter.DOGS,
            onClick = { onFilterChanged(PetFilter.DOGS) },
            label = { Text("Perros") },
            leadingIcon = if (selectedFilter == PetFilter.DOGS) {
                { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == PetFilter.CATS,
            onClick = { onFilterChanged(PetFilter.CATS) },
            label = { Text("Gatos") },
            leadingIcon = if (selectedFilter == PetFilter.CATS) {
                { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == PetFilter.OTHERS,
            onClick = { onFilterChanged(PetFilter.OTHERS) },
            label = { Text("Otros") },
            leadingIcon = if (selectedFilter == PetFilter.OTHERS) {
                { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetCard(
    pet: Pet,
    onClick: () -> Unit
) {
    val extColors = LocalExtendedColors.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (pet.species) {
                PetSpecies.DOG -> extColors.mint.colorContainer.copy(alpha = 0.5f)
                PetSpecies.CAT -> extColors.lavander.colorContainer.copy(alpha = 0.5f)
                else -> extColors.peach.colorContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pet Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        when (pet.species) {
                            PetSpecies.DOG -> extColors.mint.colorContainer
                            PetSpecies.CAT -> extColors.lavander.colorContainer
                            else -> extColors.peach.colorContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (pet.species) {
                        PetSpecies.DOG -> "🐕"
                        PetSpecies.CAT -> "🐈"
                        PetSpecies.RABBIT -> "🐰"
                        PetSpecies.BIRD -> "🦜"
                        else -> "🐾"
                    },
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Pet Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    pet.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    pet.breed,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Cake,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${pet.age} años",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Monitor,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${pet.weight} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Next appointment indicator
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ver detalles",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPetsState(
    onAddPet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "🐾",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No tienes mascotas registradas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Agrega tu primera mascota para comenzar",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddPet,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar Mascota")
        }
    }
}

enum class PetFilter {
    ALL, DOGS, CATS, OTHERS
}