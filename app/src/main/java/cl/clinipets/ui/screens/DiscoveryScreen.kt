package cl.clinipets.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import cl.clinipets.R
import cl.clinipets.util.Result

@Composable
fun DiscoveryScreen(navController: NavController, discoveryViewModel: DiscoveryViewModel = hiltViewModel()) {
    val offersState by discoveryViewModel.offersState.collectAsState()
    val proceduresState by discoveryViewModel.proceduresState.collectAsState()
    val veterinariansState by discoveryViewModel.veterinariansState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.discovery_screen_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = stringResource(R.string.discovery_offers_title), style = MaterialTheme.typography.titleMedium)
        when (offersState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(offersState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                val offers = (offersState as Result.Success).data
                if (offers.isEmpty()) {
                    Text(text = stringResource(R.string.discovery_no_offers_available))
                } else {
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(offers) {
                            Text(text = "- ${it.titulo} (${it.precio} ${it.moneda})",
                                modifier = Modifier.fillMaxWidth().clickable { /* TODO: Navigate to Offer Detail */ }
                                    .padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text(text = stringResource(R.string.discovery_procedures_title), style = MaterialTheme.typography.titleMedium)
        when (proceduresState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(proceduresState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                val procedures = (proceduresState as Result.Success).data
                if (procedures.isEmpty()) {
                    Text(text = stringResource(R.string.discovery_no_procedures_available))
                } else {
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(procedures) {
                            Text(text = "- ${it.nombre} (${it.sku})",
                                modifier = Modifier.fillMaxWidth().clickable { /* TODO: Navigate to Procedure Detail */ }
                                    .padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text(text = stringResource(R.string.discovery_veterinarians_title), style = MaterialTheme.typography.titleMedium)
        when (veterinariansState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(veterinariansState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                val veterinarians = (veterinariansState as Result.Success).data
                if (veterinarians.isEmpty()) {
                    Text(text = stringResource(R.string.discovery_no_veterinarians_available))
                } else {
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(veterinarians) {
                            Text(text = "- ${it.nombre} ${it.apellido} (${it.especialidad})",
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("vet_detail/${it.id}") }
                                    .padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
