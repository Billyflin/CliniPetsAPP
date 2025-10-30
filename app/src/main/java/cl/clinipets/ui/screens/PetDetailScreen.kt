package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import cl.clinipets.util.Result
import java.util.UUID

@Composable
fun PetDetailScreen(navController: NavController, petId: String, petDetailViewModel: PetDetailViewModel = hiltViewModel()) {
    val petState by petDetailViewModel.petState.collectAsState()
    val updatePetState by petDetailViewModel.updatePetState.collectAsState()
    val deletePetState by petDetailViewModel.deletePetState.collectAsState()

    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }

    LaunchedEffect(petId) {
        petDetailViewModel.fetchPetDetails(UUID.fromString(petId))
    }

    LaunchedEffect(petState) {
        if (petState is Result.Success) {
            val pet = (petState as Result.Success).data
            name = pet.nombre
            species = pet.especie.name // Convert enum to string
            breed = pet.raza ?: ""
            weight = pet.pesoKg?.toString() ?: ""
            birthDate = pet.fechaNacimiento?.toString() ?: ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (petState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(petState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                Text(text = "Pet Details", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it },
                    label = { Text("Species") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Breed") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Birth Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    petDetailViewModel.updatePet(
                        UUID.fromString(petId),
                        name, breed, weight.toDoubleOrNull(), birthDate
                    ) { navController.popBackStack() }
                }, modifier = Modifier.fillMaxWidth(), enabled = updatePetState !is Result.Loading) {
                    if (updatePetState is Result.Loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Updating Pet...")
                    } else {
                        Text(text = "Update Pet")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    petDetailViewModel.deletePet(UUID.fromString(petId)) { navController.popBackStack() }
                }, modifier = Modifier.fillMaxWidth(), enabled = deletePetState !is Result.Loading) {
                    if (deletePetState is Result.Loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Deleting Pet...")
                    } else {
                        Text(text = "Delete Pet")
                    }
                }

                updatePetState.let { state ->
                    if (state is Result.Error) {
                        Text(text = "Error: ${state.exception.message}", color = androidx.compose.ui.graphics.Color.Red)
                    }
                }
                deletePetState.let { state ->
                    if (state is Result.Error) {
                        Text(text = "Error: ${state.exception.message}", color = androidx.compose.ui.graphics.Color.Red)
                    }
                }
            }
        }
    }
}
