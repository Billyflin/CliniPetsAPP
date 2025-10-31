package cl.clinipets.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cl.clinipets.feature.auth.presentation.AuthUiState
import cl.clinipets.feature.descubrimiento.presentation.DescubrimientoRoute
import cl.clinipets.feature.home.presentation.HomeAction
import cl.clinipets.feature.home.presentation.HomeRoute
import cl.clinipets.feature.mascotas.presentation.MisMascotasRoute
import cl.clinipets.feature.perfil.presentation.PerfilRoute

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
                    }
                },
                onLogout = performLogout,
                onRefreshProfile = onRefreshProfile,
            )
        }
        composable(AppDestination.MisMascotas.route) {
            MisMascotasRoute(
                onCerrarSesion = performLogout,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(AppDestination.Descubrir.route) {
            DescubrimientoRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppDestination.Perfil.route) {
            PerfilRoute(
                estado = authState,
                onBack = { navController.popBackStack() },
                onCerrarSesion = performLogout,
                onIrOnboardingVet = { /* TODO: Navegar a onboarding veterinario */ },
            )
        }
    }
}
