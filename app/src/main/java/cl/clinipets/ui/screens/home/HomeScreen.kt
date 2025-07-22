// ui/screens/HomeScreen.kt
package cl.clinipets.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.UserViewModel
import cl.clinipets.ui.viewmodels.VetViewModel

// package cl.clinipets.ui.screens.home

@Composable
fun HomeScreen(
    onNavigateToPets: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToVetDashboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    vetViewModel: VetViewModel = hiltViewModel()
) {
    val userState by userViewModel.userState.collectAsState()
    val vetState by vetViewModel.vetState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Hola, ${userState.userName}",
            style = MaterialTheme.typography.headlineMedium
        )
        //Boton de perfil
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Perfil",
                modifier = Modifier
                    .clickable { onNavigateToProfile() }
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Cards de resumen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToPets() }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("${userState.petsCount}", style = MaterialTheme.typography.headlineLarge)
                    Text("Mascotas")
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToAppointments() }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${userState.appointmentsCount}",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text("Citas")
                }
            }
        }

        // Acciones rápidas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToAppointments() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Agendar Cita", fontWeight = FontWeight.Bold)
                    Text("Programa una visita", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        if (userState.isVet) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToVetDashboard() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Panel Veterinario", fontWeight = FontWeight.Bold)
                        Text("Gestión de consultas", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.MedicalServices, contentDescription = null)
                }
            }
        }
    }
}