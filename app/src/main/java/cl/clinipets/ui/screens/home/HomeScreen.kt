// ui/screens/HomeScreen.kt
package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.UserViewModel

@Composable
fun HomeScreen(
    onNavigateToPets: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    Column {
        Text("Bienvenido")
        Button(onClick = onNavigateToPets) { Text("Mascotas (${userState.petsCount})") }
        Button(onClick = onNavigateToAppointments) { Text("Citas (${userState.appointmentsCount})") }
        Button(onClick = onNavigateToProfile) { Text("Perfil") }
        OutlinedButton(onClick = onSignOut) { Text("Cerrar Sesión") }
    }
}