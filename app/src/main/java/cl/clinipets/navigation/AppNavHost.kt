package cl.clinipets.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cl.clinipets.feature.auth.presentation.AuthUiState
import cl.clinipets.feature.descubrimiento.presentation.DescubrimientoRoute
import cl.clinipets.feature.home.presentation.HomeAction
import cl.clinipets.feature.home.presentation.HomeRoute
import cl.clinipets.feature.mascotas.presentation.MascotaDetalleRoute
import cl.clinipets.feature.mascotas.presentation.MascotaFormRoute
import cl.clinipets.feature.mascotas.presentation.MisMascotasRoute
import cl.clinipets.feature.perfil.presentation.PerfilRoute
import cl.clinipets.feature.veterinario.presentation.VeterinarioAgendaRoute
import cl.clinipets.feature.veterinario.presentation.VeterinarioOnboardingRoute
import cl.clinipets.feature.veterinario.presentation.VeterinarioPerfilRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    authState: AuthUiState,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit,
) {
    val performLogout = remember(navController, onLogout) {
        {
            navController.popBackStack(AppDestination.Home.route, inclusive = false)
            onLogout()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
    ) {
        composable(AppDestination.Home.route) {
            HomeRoute(
                estadoAuth = authState,
                onAction = { action ->
                    when (action) {
                        HomeAction.MIS_MASCOTAS -> navController.navigate(AppDestination.MisMascotas.route)
                        HomeAction.DESCUBRIR -> navController.navigate(AppDestination.Descubrir.route)
                        HomeAction.PERFIL -> navController.navigate(AppDestination.Perfil.route)
                        HomeAction.VETERINARIO -> navController.navigate(AppDestination.VeterinarioPerfil.route)
                    }
                },
                onRefreshProfile = onRefreshProfile,
            )
        }
        composable(AppDestination.MisMascotas.route) {
            MisMascotasRoute(
                onNavigateBack = { navController.popBackStack() },
                onAgregarMascota = { navController.navigate(AppDestination.MascotaCrear.route) },
                onMascotaSeleccionada = { id ->
                    navController.navigate(AppDestination.MascotaDetalle.createRoute(id))
                },
            )
        }
        composable(AppDestination.MascotaCrear.route) {
            MascotaFormRoute(
                onBack = { navController.popBackStack() },
                onMascotaGuardada = { id, _ ->
                    navController.popBackStack()
                    navController.navigate(AppDestination.MascotaDetalle.createRoute(id))
                },
            )
        }
        composable(
            route = AppDestination.MascotaDetalle.route,
            arguments = listOf(navArgument(AppDestination.MascotaDetalle.ARG_ID) { type = NavType.StringType }),
        ) {
            MascotaDetalleRoute(
                onBack = { navController.popBackStack() },
                onEditar = { id -> navController.navigate(AppDestination.MascotaEditar.createRoute(id)) },
                onMascotaEliminada = {
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = AppDestination.MascotaEditar.route,
            arguments = listOf(navArgument(AppDestination.MascotaEditar.ARG_ID) { type = NavType.StringType }),
        ) {
            MascotaFormRoute(
                onBack = { navController.popBackStack() },
                onMascotaGuardada = { id, fueEdicion ->
                    navController.popBackStack()
                    if (fueEdicion) {
                        navController.popBackStack()
                    }
                    navController.navigate(AppDestination.MascotaDetalle.createRoute(id))
                },
            )
        }
        composable(AppDestination.Descubrir.route) {
            DescubrimientoRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppDestination.Perfil.route) { backStackEntry ->
            val refrescarVeterinario by backStackEntry
                .savedStateHandle
                .getStateFlow("vetRefresh", false)
                .collectAsState()
            PerfilRoute(
                estado = authState,
                onBack = { navController.popBackStack() },
                onCerrarSesion = performLogout,
                onIrOnboardingVet = { navController.navigate(AppDestination.VeterinarioOnboarding.route) },
                onIrPerfilVet = { navController.navigate(AppDestination.VeterinarioPerfil.route) },
                shouldRefreshVeterinario = refrescarVeterinario,
                onVeterinarioRefreshConsumed = {
                    backStackEntry.savedStateHandle["vetRefresh"] = false
                },
            )
        }
        composable(AppDestination.VeterinarioOnboarding.route) {
            VeterinarioOnboardingRoute(
                onBack = { navController.popBackStack() },
                onCompletado = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("vetRefresh", true)
                    onRefreshProfile()
                    val regresoAPerfil = navController.popBackStack(
                        AppDestination.Perfil.route,
                        inclusive = false,
                    )
                    if (!regresoAPerfil) {
                        navController.popBackStack(AppDestination.Home.route, inclusive = false)
                        navController.navigate(AppDestination.Perfil.route)
                    }
                },
            )
        }
        composable(AppDestination.VeterinarioPerfil.route) {
            VeterinarioPerfilRoute(
                onBack = { navController.popBackStack() },
                onIrAgenda = { navController.navigate(AppDestination.VeterinarioAgenda.route) },
                onIrOnboarding = { navController.navigate(AppDestination.VeterinarioOnboarding.route) },
            )
        }
        composable(AppDestination.VeterinarioAgenda.route) {
            VeterinarioAgendaRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
