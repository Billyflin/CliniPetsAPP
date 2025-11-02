package cl.clinipets.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cl.clinipets.ui.auth.LoginScreen
import cl.clinipets.ui.auth.LoginViewModel
import cl.clinipets.ui.discover.DiscoverScreen
import cl.clinipets.ui.home.HomeScreen
import cl.clinipets.ui.mascotas.MascotasScreen
import cl.clinipets.ui.onboarding.VeterinarianOnboardingScreen
import cl.clinipets.ui.profile.ProfileScreen

sealed class AppRoute(val route: String) {
    data object Login : AppRoute("login")
    data object Home : AppRoute("home")
    data object Mascotas : AppRoute("mascotas")
    data object Profile : AppRoute("profile")
    data object Discover : AppRoute("discover")
    data object VeterinarianOnboarding : AppRoute("vet_onboarding")
}

@OptIn(ExperimentalMaterial3Api::class)
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
        startDestination = AppRoute.Login.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(AppRoute.Login.route) {
            LoginScreen(
                busy = busy, error = uiState.error, onLoginClick = onLoginClick
            )
        }
        composable(AppRoute.Home.route) {
            HomeScreen(
                displayName = uiState.displayName,
                roles = uiState.roles,
                onNavigateToMascotas = {
                    navController.navigate(AppRoute.Mascotas.route) {
                        launchSingleTop = true
                    }

                },
                onNavigateToDiscover = {
                    navController.navigate(AppRoute.Discover.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(AppRoute.Profile.route) {
                        launchSingleTop = true
                    }
                },
                onLogout = onLogout
            )
        }
        composable(AppRoute.Mascotas.route) {
            MascotasScreen(
                displayName = uiState.displayName,
                roles = uiState.roles,
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }
        composable(AppRoute.Profile.route) {
            ProfileScreen(
                state = uiState,
                onBack = { navController.popBackStack() },
                onBecomeVeterinarian = {
                    navController.navigate(AppRoute.VeterinarianOnboarding.route) {
                        launchSingleTop = true
                    }
                },
                onLogout = onLogout
            )
        }
        composable(AppRoute.VeterinarianOnboarding.route) {
            VeterinarianOnboardingScreen(
                suggestedName = uiState.displayName ?: uiState.me?.nombre,
                onBack = { navController.popBackStack() },
                onCompleted = {
                    navController.popBackStack(AppRoute.Profile.route, false)
                    onRefreshProfile()
                })
        }
        composable(AppRoute.Discover.route) {
            DiscoverScreen(
                onBack = { navController.popBackStack() })
        }
    }
}
