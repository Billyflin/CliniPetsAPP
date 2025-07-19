// ui/screens/ProfileScreen.kt
package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.viewmodels.AuthViewModel
import cl.clinipets.ui.viewmodels.UserViewModel

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val userState by userViewModel.userState.collectAsState()
    Column {

        Text("Mi Perfil")
        Text("Nombre: ${userState.userName}")
        Text("Email: ${userState.userEmail}")
        Text("Miembro desde: ${userState.memberSince}")
        Text("Mascotas: ${userState.petsCount}")
        Text("Citas: ${userState.appointmentsCount}")

        Button(onClick = onNavigateBack) { Text("Volver") }
        OutlinedButton(onClick = {
            authViewModel.signOut()
            onSignOut()
        }) { Text("Cerrar Sesión") }
    }
}
