package cl.clinipets.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import cl.clinipets.util.Result

@Composable
fun HomeScreen(navController: NavController, homeViewModel: HomeViewModel = hiltViewModel()) {
    val meResponseState by homeViewModel.meResponseState.collectAsState()
    val petsState by homeViewModel.petsState.collectAsState()
    val logoutState by homeViewModel.logoutState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (meResponseState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(meResponseState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                val me = (meResponseState as Result.Success).data
                Text(text = "Welcome, ${me.email ?: ""}!")
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        Text(text = "Your Pets:")
        when (petsState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(petsState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                val pets = (petsState as Result.Success).data
                if (pets.isEmpty()) {
                    Text(text = "No pets registered.")
                } else {
                    LazyColumn {
                        items(pets) {
                            Text(text = "- ${it.nombre} (${it.especie})",
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("pet_detail/${it.id}") }
                                    .padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        Button(onClick = { navController.navigate("add_pet") },
            modifier = Modifier.padding(top = 8.dp))
        {
            Text(text = "Add New Pet")
        }

        Button(onClick = { homeViewModel.logout { navController.navigate("login") { popUpTo("home") { inclusive = true } } } },
            modifier = Modifier.padding(top = 16.dp), enabled = logoutState !is Result.Loading)
        {
            if (logoutState is Result.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("Logging out...")
            } else {
                Text(text = "Logout")
            }
        }
    }
}
