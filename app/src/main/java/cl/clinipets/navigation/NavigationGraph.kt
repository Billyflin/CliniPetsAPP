// navigation/AppNavigation.kt
package cl.clinipets.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.clinipets.ui.screens.LoginScreen
import cl.clinipets.ui.screens.ProfileScreen
import cl.clinipets.ui.screens.appointments.AddAppointmentScreen
import cl.clinipets.ui.screens.appointments.AppointmentsScreen
import cl.clinipets.ui.screens.home.HomeScreen
import cl.clinipets.ui.screens.pets.AddPetScreen
import cl.clinipets.ui.screens.pets.PetDetailScreen
import cl.clinipets.ui.screens.pets.PetsScreen
import cl.clinipets.ui.screens.profile.SettingsScreen
import cl.clinipets.ui.screens.vet.VetScheduleScreen
import cl.clinipets.ui.viewmodels.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // Determinar destino inicial basado en estado de autenticación
    val startDestination = if (authState.isAuthenticated) "home" else "login"

    // Observar cambios en autenticación
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToPets = { navController.navigate("pets") },
                onNavigateToAppointments = { navController.navigate("appointments") },
                onNavigateToProfile = { navController.navigate("profile") },
                onSignOut = {
                    authViewModel.signOut()
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

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVetSchedule = { navController.navigate("vet_schedule") }
            )
        }


        composable("profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    // Navegación manejada por LaunchedEffect arriba
                }
            )
        }


        composable("vet_schedule") {
            VetScheduleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

    }
}