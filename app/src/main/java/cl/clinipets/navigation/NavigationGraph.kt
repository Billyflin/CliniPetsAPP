// navigation/AppNavigation.kt
package cl.clinipets.navigation

import MedicalConsultationScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.clinipets.ui.screens.LoginScreen
import cl.clinipets.ui.screens.appointments.AppointmentsScreen
import cl.clinipets.ui.screens.appointments.CreateAppointmentScreen
import cl.clinipets.ui.screens.home.HomeScreen
import cl.clinipets.ui.screens.pets.AddEditPetScreen
import cl.clinipets.ui.screens.pets.PetDetailScreen
import cl.clinipets.ui.screens.pets.PetsScreen
import cl.clinipets.ui.screens.profile.ProfileScreen
import cl.clinipets.ui.screens.profile.SettingsScreen
import cl.clinipets.ui.screens.vet.InventoryScreen
import cl.clinipets.ui.screens.vet.ServicesScreen
import cl.clinipets.ui.screens.vet.VetDashboardScreen
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
        navController = navController, startDestination = startDestination
    ) {
        // ====================== AUTENTICACIÓN ======================
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                })
        }

        // ====================== HOME ======================
        composable("home") {
            HomeScreen(
                onNavigateToPets = { navController.navigate("pets") },
                onNavigateToAppointments = { navController.navigate("appointments") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToVetDashboard = { navController.navigate("vet_dashboard") })
        }

        // ====================== MASCOTAS ======================
        composable("pets") {
            PetsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddPet = { navController.navigate("add_pet") },
                onNavigateToPetDetail = { petId ->
                    navController.navigate("pet_detail/$petId")
                })
        }

        composable("add_pet") {
            AddEditPetScreen(
                petId = null,
                onPetSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() })

        }

        composable("edit_pet/{petId}") { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AddEditPetScreen(
                petId = petId,
                onPetSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() })
        }

        composable("pet_detail/{petId}") { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            PetDetailScreen(
                petId = petId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate("edit_pet/$id")
                },
                onNavigateToNewAppointment = { id ->
                    navController.navigate("create_appointment?petId=$id")
                })
        }

        // ====================== CITAS ======================
        composable("appointments") {
            AppointmentsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddAppointment = { navController.navigate("create_appointment") }

//               , onNavigateToAppointmentDetail = { appointmentId ->
//                    navController.navigate("appointment_detail/$appointmentId")
//                }
            )
        }

        composable("create_appointment?petId={petId}") { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId")
            CreateAppointmentScreen(
                petId = petId,
                onAppointmentCreated = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() })
        }

        composable("appointment_detail/{appointmentId}") { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
        }

        // ====================== PERFIL Y CONFIGURACIÓN ======================
        composable("profile") {
            ProfileScreen(onNavigateBack = { navController.popBackStack() }, onSignOut = {
            })
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ====================== VETERINARIO ======================
        composable("vet_dashboard") {
            VetDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConsultation = { appointmentId ->
                    navController.navigate("medical_consultation/$appointmentId")
                },
                onNavigateToInventory = { navController.navigate("inventory") },
                onNavigateToSchedule = { navController.navigate("vet_schedule") },
                onNavigateToCreatePet = { navController.navigate("add_pet") }
            )
        }

        composable("medical_consultation/{appointmentId}") { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
            MedicalConsultationScreen(appointmentId = appointmentId, onConsultationFinished = {
                navController.popBackStack()
                navController.popBackStack() // Volver al dashboard
            }, onNavigateBack = { navController.popBackStack() })
        }

        composable("inventory") {
            InventoryScreen(
                onNavigateBack = { navController.popBackStack() })
        }

        composable("vet_schedule") {
            VetScheduleScreen(
                onNavigateBack = { navController.popBackStack() },

                )
        }

        // ====================== SERVICIOS VETERINARIOS ======================
        composable("vet_services") {
            ServicesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

    }
}


