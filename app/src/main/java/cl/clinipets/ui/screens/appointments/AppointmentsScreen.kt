// ui/screens/appointments/AppointmentsScreen.kt
package cl.clinipets.ui.screens.appointments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.navigation.Routes
import cl.clinipets.ui.components.BaseScreen

@Composable
fun AppointmentsScreen(
    navigateTo: (Routes) -> Unit,
    selectedNavIndex: Int,
    onNavIndexChanged: (Int) -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BaseScreen(
        title = "Mis Citas",
        navigateTo = navigateTo,
        selectedNavIndex = selectedNavIndex,
        onNavIndexChanged = onNavIndexChanged
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Contenido de la pantalla de citas
            item {
                Text(
                    "Próximas Citas",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Aquí irían las citas...
        }
    }
}