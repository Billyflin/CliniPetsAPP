// ui/navigation/NavGraph.kt — NavHost usando rutas tipadas (Compose Navigation 2.8+)
package cl.clinipets.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cl.clinipets.ui.agenda.OfertasScreen
import cl.clinipets.ui.agenda.ReservasScreen
import cl.clinipets.ui.agenda.SolicitudesScreen
import cl.clinipets.ui.auth.LoginScreen
import cl.clinipets.ui.auth.LoginViewModel
import cl.clinipets.ui.discover.DiscoverScreen
import cl.clinipets.ui.home.HomeScreen
import cl.clinipets.ui.mascotas.MascotaDetailScreen
import cl.clinipets.ui.mascotas.MascotaFormScreen
import cl.clinipets.ui.mascotas.MascotasScreen
import cl.clinipets.ui.profile.ProfileScreen
import cl.clinipets.ui.profile.VeterinarianScreen
import cl.clinipets.ui.veterinarios.MiCatalogoScreen
import cl.clinipets.ui.veterinarios.MiDisponibilidadScreen
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable object LoginRoute
@Serializable object HomeRoute
@Serializable object MascotasRoute
@Serializable object ProfileRoute
@Serializable object DiscoverRoute
@Serializable object VeterinarianRoute
@Serializable object MiCatalogoRoute
@Serializable object MiDisponibilidadRoute
@Serializable object SolicitudesRoute
@Serializable object OfertasRoute
@Serializable object ReservasRoute

@Serializable
data class MascotaDetailRoute(val id: String)

@Serializable
data class MascotaFormRoute(val id: String? = null)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalStdlibApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    uiState: LoginViewModel.UiState,
    busy: Boolean,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit
) {

    NavHost(
        navController = navController,
        startDestination = LoginRoute
    ) {
        composable<LoginRoute> {
            LoginScreen(busy = busy, error = uiState.error, onLoginClick = onLoginClick)
        }
        composable<HomeRoute> {
            HomeScreen(
                uiState.displayName,
                uiState.roles,
                { navController.navigate(MascotasRoute) },
                { navController.navigate(DiscoverRoute) },
                { navController.navigate(ProfileRoute) },
                { navController.navigate(MiCatalogoRoute) },
                { navController.navigate(MiDisponibilidadRoute) },
                { navController.navigate(SolicitudesRoute) },
                { navController.navigate(OfertasRoute) },
                { navController.navigate(ReservasRoute) }
            )
        }
        composable<MascotasRoute> {
            MascotasScreen(
                displayName = uiState.displayName,
                onNavigateToMascotaDetail = { id: UUID ->
                    navController.navigate(MascotaDetailRoute(id.toString()))
                },
                onBack = { navController.popBackStack() },
                onNavigateToMascotaForm = {
                    navController.navigate(MascotaFormRoute())
                }
            )
        }
        composable<MascotaFormRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<MascotaFormRoute>()
            val mascotaId = args.id?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            MascotaFormScreen(
                mascotaId = mascotaId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<MascotaDetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<MascotaDetailRoute>()
            MascotaDetailScreen(
                mascotaId = UUID.fromString(args.id),
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(MascotaFormRoute(id.toString())) }
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                state = uiState,
                onBack = { navController.popBackStack() },
                onBecomeVeterinarian = { navController.navigate(VeterinarianRoute) },
                onRefreshProfile=     onRefreshProfile,
                onEditProfessional = { navController.navigate(VeterinarianRoute) },
                onLogout = onLogout
            )
        }
        composable<VeterinarianRoute> {
            VeterinarianScreen(
                suggestedName = uiState.displayName ?: uiState.me?.nombre,
                onBack = { navController.popBackStack() },
                onCompleted = {
                    navController.popBackStack()
                    onRefreshProfile()
                }
            )
        }
        composable<DiscoverRoute> {
            DiscoverScreen(onBack = { navController.popBackStack() })
        }
        composable<MiCatalogoRoute> {
            MiCatalogoScreen(onBack = { navController.popBackStack() })
        }
        composable<MiDisponibilidadRoute> {
            MiDisponibilidadScreen(onBack = { navController.popBackStack() })
        }
        composable<SolicitudesRoute> {
            SolicitudesScreen(onBack = { navController.popBackStack() })
        }
        composable<OfertasRoute> {
            OfertasScreen(onBack = { navController.popBackStack() })
        }
        composable<ReservasRoute> {
            ReservasScreen(onBack = { navController.popBackStack() })
        }
    }
}
