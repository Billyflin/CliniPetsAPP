package cl.clinipets.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.clinipets.domain.agenda.AgendaRepository
import cl.clinipets.domain.catalogo.CatalogoRepository
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.mascotas.MascotasRepository

private enum class HomeTab { Descubrir, Mascotas, Reservas }

@Composable
fun HomeScreen(
    discoveryRepository: DiscoveryRepository,
    mascotasRepository: MascotasRepository,
    agendaRepository: AgendaRepository,
    catalogoRepository: CatalogoRepository,
    onLogout: () -> Unit,
) {
    val tab = remember { mutableStateOf(HomeTab.Descubrir) }
    val navController = rememberNavController()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab.value.ordinal) {
            HomeTab.values().forEachIndexed { index, t ->
                Tab(
                    selected = tab.value == t,
                    onClick = {
                        tab.value = t
                        when (t) {
                            HomeTab.Descubrir -> navController.navigate("descubrir") { launchSingleTop = true }
                            HomeTab.Mascotas -> navController.navigate("mascotas") { launchSingleTop = true }
                            HomeTab.Reservas -> navController.navigate("reservas") { launchSingleTop = true }
                        }
                    },
                    text = { Text(t.name) }
                )
            }
        }

        NavHost(navController = navController, startDestination = "descubrir") {
            composable("descubrir") {
                DiscoveryScreen(
                    repo = discoveryRepository,
                    onOpenVet = { vetId -> navController.navigate("vet/$vetId") }
                )
            }
            composable("mascotas") {
                MascotasScreen(repo = mascotasRepository)
            }
            composable("reservas") {
                ReservasScreen(repo = agendaRepository)
            }
            composable(
                route = "vet/{vetId}",
                arguments = listOf(navArgument("vetId") { type = NavType.StringType })
            ) { backStackEntry ->
                val vetId = backStackEntry.arguments?.getString("vetId") ?: ""
                cl.clinipets.ui.VetDetailScreen(
                    vetId = vetId,
                    catalogoRepository = catalogoRepository,
                    agendaRepository = agendaRepository,
                    mascotasRepository = mascotasRepository
                )
            }
        }
    }
}
