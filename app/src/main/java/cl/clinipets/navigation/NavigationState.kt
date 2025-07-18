
// navigation/NavigationState.kt
package cl.clinipets.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Stable
class NavigationState(
    val navController: NavHostController
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState()
            .value?.destination

    val currentRoute: String?
        @Composable get() = currentDestination?.route

    // Bottom navigation items
    val bottomBarRoutes = listOf(
        Route.Home,
        Route.Appointments,
        Route.Pets,
        Route.Profile
    )

    fun navigateToBottomBarRoute(route: Route) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToRoute(route: Route) {
        navController.navigate(route)
    }

    fun navigateUp() {
        navController.navigateUp()
    }
}

@Composable
fun rememberNavigationState(
    navController: NavHostController = rememberNavController()
): NavigationState = remember(navController) {
    NavigationState(navController)
}