package cl.clinipets.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import cl.clinipets.feature.auth.presentation.AuthUiState

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

    }
}
