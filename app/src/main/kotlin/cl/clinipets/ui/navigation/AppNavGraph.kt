package cl.clinipets.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cl.clinipets.ui.auth.LoginScreen
import cl.clinipets.ui.auth.LoginViewModel
import cl.clinipets.ui.home.HomeScreen
import cl.clinipets.ui.mascotas.MascotasScreen

sealed class AppRoute(val route: String) {
    data object Login : AppRoute("login")
    data object Home : AppRoute("home")
    data object Mascotas : AppRoute("mascotas")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    uiState: LoginViewModel.UiState,
    busy: Boolean,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Login.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(AppRoute.Login.route) {
            LoginScreen(
                busy = busy,
                error = uiState.error,
                onLoginClick = onLoginClick
            )
        }
        composable(AppRoute.Home.route) {
            HomeScreen(
                displayName = uiState.displayName,
                roles = uiState.roles,
                onNavigateToMascotas = {
                    if (uiState.roles.any { it.equals("CLIENTE", ignoreCase = true) }) {
                        navController.navigate(AppRoute.Mascotas.route) {
                            launchSingleTop = true
                        }
                    }
                },
                onLogout = onLogout
            )
        }
        composable(AppRoute.Mascotas.route) {
            MascotasScreen(
                displayName = uiState.displayName,
                roles = uiState.roles,
                onLogout = onLogout
            )
        }
    }
}
