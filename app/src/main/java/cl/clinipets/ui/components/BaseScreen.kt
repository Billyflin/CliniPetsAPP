// ui/components/BaseScreen.kt
package cl.clinipets.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.clinipets.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    title: String,
    navigateTo: (Routes) -> Unit,
    selectedNavIndex: Int,
    onNavIndexChanged: (Int) -> Unit,
    topBar: @Composable () -> Unit = {
        TopAppBar(
            title = { Text(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = topBar
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content(
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 100.dp
                )
            )

            // Bottom Navigation Flotante
            FloatingBottomNavBar(
                items = listOf(
                    BottomNavItem("Inicio", Icons.Filled.Home, Icons.Outlined.Home),
                    BottomNavItem("Citas", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, 2),
                    BottomNavItem("Mascotas", Icons.Filled.Pets, Icons.Outlined.Pets),
                    BottomNavItem("Perfil", Icons.Filled.Person, Icons.Outlined.Person)
                ),
                selectedIndex = selectedNavIndex,
                onItemSelected = { index ->
                    onNavIndexChanged(index)
                    when (index) {
                        0 -> navigateTo(Routes.HomeRoute)
                        1 -> navigateTo(Routes.AppointmentsRoute)
                        2 -> navigateTo(Routes.PetsRoute)
                        3 -> navigateTo(Routes.ProfileRoute)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}