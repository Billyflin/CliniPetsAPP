package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, onSignOut: () -> Unit, isVet: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Inicio", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { onNavigate("perfil") }, modifier = Modifier.fillMaxWidth()) {
            Text("Perfil")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { onNavigate("mascotas") }, modifier = Modifier.fillMaxWidth()) {
            Text("Mascotas")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { onNavigate("descubrimiento") }, modifier = Modifier.fillMaxWidth()) {
            Text("Descubrimiento")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { onNavigate("reservas") }, modifier = Modifier.fillMaxWidth()) {
            Text("Reservas")
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { onNavigate("nueva_reserva") }, modifier = Modifier.fillMaxWidth()) {
            Text("Nueva reserva")
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (isVet) {
            Button(onClick = { onNavigate("disponibilidad_vet") }, modifier = Modifier.fillMaxWidth()) {
                Text("Disponibilidad (Vet)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onNavigate("agenda_vet") }, modifier = Modifier.fillMaxWidth()) {
                Text("Agenda (Vet)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onNavigate("juntas") }, modifier = Modifier.fillMaxWidth()) {
                Text("Juntas (Vet)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onNavigate("clinica") }, modifier = Modifier.fillMaxWidth()) {
                Text("Herramientas clínicas")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { onSignOut() }, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}
