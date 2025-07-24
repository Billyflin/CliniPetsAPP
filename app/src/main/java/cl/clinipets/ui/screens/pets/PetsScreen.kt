// ui/screens/pets/PetsScreen.kt
package cl.clinipets.ui.screens.pets

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.ui.theme.ExtendedColorScheme
import cl.clinipets.ui.theme.LocalExtendedColors
import cl.clinipets.ui.viewmodels.PetsViewModel
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddPet: () -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    val petsState by viewModel.petsState.collectAsStateWithLifecycle()
    val extColors = LocalExtendedColors.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Mascotas")
                        Text(
                            "Tus compañeros peludos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddPet,
                containerColor = extColors.pink.color,
                contentColor = extColors.pink.onColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar mascota")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Mascota")
            }
        }
    ) { paddingValues ->
        if (petsState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = extColors.pink.color
                    )
                    Text(
                        "Cargando mascotas...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (petsState.pets.isEmpty()) {
            EmptyPetsState(
                modifier = Modifier.padding(paddingValues),
                onAddPet = onNavigateToAddPet,
                extColors = extColors
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(petsState.pets) { pet ->
                    PetCard(
                        pet = pet,
                        onClick = { onNavigateToPetDetail(pet.id) },
                        extColors = extColors
                    )
                }

                // Espacio adicional para el FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        petsState.error?.let { error ->
            LaunchedEffect(error) {
                // Mostrar snackbar de error
            }
        }
    }
}

@Composable
private fun PetCard(
    pet: Pet,
    onClick: () -> Unit,
    extColors: ExtendedColorScheme
) {
    val backgroundColor = when (pet.species) {
        PetSpecies.DOG -> extColors.peach.colorContainer
        PetSpecies.CAT -> extColors.lavander.colorContainer
        else -> extColors.mint.colorContainer
    }

    val iconColor = when (pet.species) {
        PetSpecies.DOG -> extColors.peach.onColorContainer
        PetSpecies.CAT -> extColors.lavander.onColorContainer
        else -> extColors.mint.onColorContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar de la mascota
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.2f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (pet.species) {
                        PetSpecies.DOG -> "🐕"
                        PetSpecies.CAT -> "🐈"
                        else -> "🐾"
                    }
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = iconColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = when (pet.species) {
                                PetSpecies.DOG -> "Perro"
                                PetSpecies.CAT -> "Gato"
                                PetSpecies.OTHER -> "Otra"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor
                        )
                    }
                    if (pet.breed.isNotBlank()) {
                        Text(
                            text = pet.breed,
                            style = MaterialTheme.typography.bodySmall,
                            color = iconColor.copy(alpha = 0.7f)
                        )
                    }
                }
                pet.birthDate?.let { birthDate ->
                    val age = calculateAge(birthDate)
                    Text(
                        text = if (age == 0) "Menos de 1 año" else "$age ${if (age == 1) "año" else "años"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = iconColor.copy(alpha = 0.7f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = iconColor
            )
        }
    }
}

@Composable
private fun EmptyPetsState(
    modifier: Modifier = Modifier,
    onAddPet: () -> Unit,
    extColors: ExtendedColorScheme
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ilustración con emojis
        Surface(
            shape = CircleShape,
            color = extColors.pink.colorContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "🐾",
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Aún no tienes mascotas!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Agrega a tu primer compañero peludo",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddPet,
            colors = ButtonDefaults.buttonColors(
                containerColor = extColors.pink.color,
                contentColor = extColors.pink.onColor
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar mi primera mascota")
        }
    }
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