package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.clinipets.auth.AuthViewModel
import cl.clinipets.ui.screens.AddPetScreen
import cl.clinipets.ui.screens.DiscoveryScreen
import cl.clinipets.ui.screens.HomeScreen
import cl.clinipets.ui.screens.LoginScreen
import cl.clinipets.ui.screens.MyReservationsScreen
import cl.clinipets.ui.screens.PetDetailScreen
import cl.clinipets.ui.screens.VetDetailScreen
import cl.clinipets.ui.theme.ClinipetsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClinipetsTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                var startDestination by remember { mutableStateOf("loading") }

                LaunchedEffect(Unit) {
                    authViewModel.checkLoginStatus(
                        onLoggedIn = { startDestination = "home" },
                        onLoggedOut = { startDestination = "login" }
                    )
                }

                if (startDestination != "loading") {
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(viewModel = authViewModel) { navController.navigate("home") { popUpTo("login") { inclusive = true } } }
                        }
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("add_pet") {
                            AddPetScreen(navController = navController)
                        }
                        composable("pet_detail/{petId}") {
                            val petId = it.arguments?.getString("petId")
                            petId?.let { id ->
                                PetDetailScreen(navController = navController, petId = id)
                            }
                        }
                        composable("discovery") {
                            DiscoveryScreen(navController = navController)
                        }
                        composable("vet_detail/{vetId}") {
                            val vetId = it.arguments?.getString("vetId")
                            vetId?.let { id ->
                                VetDetailScreen(navController = navController, vetId = id)
                            }
                        }
                        composable("my_reservations") {
                            MyReservationsScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}