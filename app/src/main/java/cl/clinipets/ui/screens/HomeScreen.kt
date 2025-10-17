package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, onSignOut: () -> Unit) {
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { onSignOut() }, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}
