package cl.clinipets.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cl.clinipets.auth.ui.LoginScreen
import cl.clinipets.mascotas.ui.MascotasScreen

sealed class AppRoute(val route: String) {
    data object Login : AppRoute("login")
    data object Home : AppRoute("home")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    busy: Boolean,
    error: String?,
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
                error = error,
                onLoginClick = onLoginClick
            )
        }
        composable(AppRoute.Home.route) {
            MascotasScreen(onLogout = onLogout)
        }
    }
}
