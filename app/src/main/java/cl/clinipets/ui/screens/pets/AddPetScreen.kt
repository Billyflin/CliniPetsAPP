// ui/screens/AddPetScreen.kt
package cl.clinipets.ui.screens.pets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.PetsViewModel

@Composable
fun AddPetScreen(
    onPetAdded: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PetsViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    val petsState by viewModel.petsState.collectAsState()

    LaunchedEffect(petsState.isPetAdded) {
        if (petsState.isPetAdded) {
            viewModel.clearState()
            onPetAdded()
        }
    }
    Column {
        Text("Agregar Mascota")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
        OutlinedTextField(
            value = species,
            onValueChange = { species = it },
            label = { Text("Especie") })
        OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Raza") })
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Edad") })
        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso") })
        Button(onClick = {
            viewModel.addPet(
                name,
                species,
                breed,
                age,
                weight
            )
        }) { Text("Guardar") }
        OutlinedButton(onClick = onNavigateBack) { Text("Cancelar") }
        petsState.error?.let { Text(it) }
    }
}