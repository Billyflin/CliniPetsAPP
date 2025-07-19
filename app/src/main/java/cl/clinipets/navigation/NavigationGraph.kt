// navigation/AppNavigation.kt
package cl.clinipets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.clinipets.ui.screens.HomeScreen
import cl.clinipets.ui.screens.LoginScreen
import cl.clinipets.ui.screens.ProfileScreen
import cl.clinipets.ui.screens.RegisterScreen
import cl.clinipets.ui.screens.appointments.AddAppointmentScreen
import cl.clinipets.ui.screens.appointments.AppointmentsScreen
import cl.clinipets.ui.screens.pets.AddPetScreen
import cl.clinipets.ui.screens.pets.PetDetailScreen
import cl.clinipets.ui.screens.pets.PetsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("home") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("home") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToPets = { navController.navigate("pets") },
                onNavigateToAppointments = { navController.navigate("appointments") },
                onNavigateToProfile = { navController.navigate("profile") },
                onSignOut = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("pets") {
            PetsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddPet = { navController.navigate("add_pet") },
                onNavigateToPetDetail = { petId ->
                    navController.navigate("pet_detail/$petId")
                }
            )
        }

        composable("add_pet") {
            AddPetScreen(
                onPetAdded = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("pet_detail/{petId}") { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            PetDetailScreen(
                petId = petId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("appointments") {
            AppointmentsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddAppointment = { navController.navigate("add_appointment") }
            )
        }

        composable("add_appointment") {
            AddAppointmentScreen(
                onAppointmentAdded = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}