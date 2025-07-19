// navigation/NavigationGraph.kt (CORREGIDO)
package cl.clinipets.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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

@Composable
fun NavigationGraph(
    navController: NavHostController,
    startDestination: Route,
    onAuthStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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

        // Onboarding Screen
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
                    navController.navigate(Route.NewPet)
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

        // New Pet Screen (TODO: Implement)
        composable<Route.NewPet> {
            // TODO: Implement NewPetScreen
        }

        // New Appointment Screen (TODO: Implement)
        composable<Route.NewAppointment> { backStackEntry ->
            val newAppointment: Route.NewAppointment = backStackEntry.toRoute()
            // TODO: Implement NewAppointmentScreen with petId = newAppointment.petId
        }

        // Appointment Detail Screen (TODO: Implement)
        composable<Route.AppointmentDetail> { backStackEntry ->
            val appointmentDetail: Route.AppointmentDetail = backStackEntry.toRoute()
            // TODO: Implement AppointmentDetailScreen with appointmentId = appointmentDetail.appointmentId
        }
    }
}