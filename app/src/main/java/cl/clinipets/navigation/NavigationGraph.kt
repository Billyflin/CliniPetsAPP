// navigation/NavigationGraph.kt
package cl.clinipets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cl.clinipets.ui.screens.appointments.AppointmentsScreen
import cl.clinipets.ui.screens.auth.LoginScreen
import cl.clinipets.ui.screens.auth.RegisterScreen
import cl.clinipets.ui.screens.auth.SplashScreen
import cl.clinipets.ui.screens.home.HomeScreen
import cl.clinipets.ui.screens.onboarding.OnboardingScreen
import cl.clinipets.ui.screens.pets.PetDetailScreen
import cl.clinipets.ui.screens.pets.PetsScreen
import cl.clinipets.ui.screens.profile.ProfileScreen
import kotlinx.serialization.Serializable

// Define all routes using type-safe navigation
sealed interface Route {
    // Auth routes
    @Serializable
    data object Splash : Route

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Login : Route

    @Serializable
    data object Register : Route

    // Main app routes
    @Serializable
    data object Home : Route

    @Serializable
    data object Appointments : Route

    @Serializable
    data object Pets : Route

    @Serializable
    data object Profile : Route

    // Detail routes with parameters
    @Serializable
    data class PetDetail(val petId: String) : Route

    @Serializable
    data class AppointmentDetail(val appointmentId: String) : Route

    @Serializable
    data class NewAppointment(val petId: String? = null) : Route
}

// Define navigation graphs
sealed class NavGraph(val route: String) {
    data object Auth : NavGraph("auth_graph")
    data object Main : NavGraph("main_graph")
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    startDestination: Route,
    onAuthStateChanged: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable<Route.Splash> {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Route.Onboarding) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Route.Login) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding
        composable<Route.Onboarding> {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Route.Login) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        // Auth Screens
        composable<Route.Login> {
            LoginScreen(
                onLoginSuccess = {
                    onAuthStateChanged(true)
                    navController.navigate(Route.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Route.Register)
                }
            )
        }

        composable<Route.Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    onAuthStateChanged(true)
                    navController.navigate(Route.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Main App Screens
        composable<Route.Home> {
            HomeScreen(
                onNavigateToPetDetail = { petId ->
                    navController.navigate(Route.PetDetail(petId))
                },
                onNavigateToNewAppointment = { petId ->
                    navController.navigate(Route.NewAppointment(petId))
                }
            )
        }

        composable<Route.Appointments> {
            AppointmentsScreen(
                onNavigateToDetail = { appointmentId ->
                    navController.navigate(Route.AppointmentDetail(appointmentId))
                },
                onNavigateToNewAppointment = {
                    navController.navigate(Route.NewAppointment())
                }
            )
        }

        composable<Route.Pets> {
            PetsScreen(
                onNavigateToPetDetail = { petId ->
                    navController.navigate(Route.PetDetail(petId))
                },
                onNavigateToNewPet = {
                    // Navigate to new pet screen
                }
            )
        }

        composable<Route.Profile> {
            ProfileScreen(
                onSignOut = {
                    onAuthStateChanged(false)
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Detail Screens
        composable<Route.PetDetail> { backStackEntry ->
            val petDetail: Route.PetDetail = backStackEntry.toRoute()
            PetDetailScreen(
                petId = petDetail.petId,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToNewAppointment = {
                    navController.navigate(Route.NewAppointment(petDetail.petId))
                }
            )
        }

        composable<Route.AppointmentDetail> { backStackEntry ->
            val appointmentDetail: Route.AppointmentDetail = backStackEntry.toRoute()
            // AppointmentDetailScreen implementation
        }

        composable<Route.NewAppointment> { backStackEntry ->
            val newAppointment: Route.NewAppointment = backStackEntry.toRoute()
            // NewAppointmentScreen implementation
        }
    }
}