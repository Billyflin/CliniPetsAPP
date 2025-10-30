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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import cl.clinipets.util.Result
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import cl.clinipets.R
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AddPetScreen(navController: NavController, addPetViewModel: AddPetViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }

    val addPetState by addPetViewModel.addPetState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(text = stringResource(R.string.add_pet_screen_title), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.add_pet_name_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = species,
            onValueChange = { species = it },
            label = { Text(stringResource(R.string.add_pet_species_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = breed,
            onValueChange = { breed = it },
            label = { Text(stringResource(R.string.add_pet_breed_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text(stringResource(R.string.add_pet_weight_label)) },
            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = birthDate,
            onValueChange = { birthDate = it },
            label = { Text(stringResource(R.string.add_pet_birth_date_label)) },
            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            addPetViewModel.addPet(name, species, breed, weight.toDoubleOrNull(), birthDate) {
                name = ""
                species = ""
                breed = ""
                weight = ""
                birthDate = ""
                navController.popBackStack()
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = addPetState !is Result.Loading) {
            if (addPetState is Result.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.loading_adding_pet))
            } else {
                Text(text = stringResource(R.string.button_add_pet))
            }
        }

        addPetState.let { state ->
            if (state is Result.Error) {
                Text(text = "Error: ${state.exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            }
        }
    }
}
