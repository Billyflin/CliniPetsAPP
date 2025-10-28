package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit, onSignOut: () -> Unit, isVet: Boolean) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CliniPets") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Inicio", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            ElevatedButton(onClick = { onNavigate("perfil") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Perfil")
            }

            Spacer(modifier = Modifier.height(8.dp))

            ElevatedButton(onClick = { onNavigate("mascotas") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Pets, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mascotas")
            }

            Spacer(modifier = Modifier.height(8.dp))

            ElevatedButton(onClick = { onNavigate("descubrimiento") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Place, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Descubrimiento")
            }

            Spacer(modifier = Modifier.height(8.dp))

            ElevatedButton(onClick = { onNavigate("reservas") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reservas")
            }

            Spacer(modifier = Modifier.height(8.dp))

            ElevatedButton(onClick = { onNavigate("nueva_reserva") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AddCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nueva reserva")
            }

            if (isVet) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Veterinario", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ElevatedButton(onClick = { onNavigate("disponibilidad_vet") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Disponibilidad")
                }
                Spacer(modifier = Modifier.height(8.dp))

                ElevatedButton(onClick = { onNavigate("agenda_vet") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agenda")
                }
                Spacer(modifier = Modifier.height(8.dp))

                ElevatedButton(onClick = { onNavigate("juntas") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.MedicalServices, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Juntas")
                }
                Spacer(modifier = Modifier.height(8.dp))

                ElevatedButton(onClick = { onNavigate("clinica") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.MedicalServices, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Herramientas clínicas")
                }
            }
        }
    }
}
