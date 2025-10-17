package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.clinipets.auth.AuthViewModel
import cl.clinipets.auth.AuthViewModelFactory
import cl.clinipets.descubrimiento.DescubrimientoScreen
import cl.clinipets.descubrimiento.DescubrimientoViewModelFactory
import cl.clinipets.mascotas.MascotasScreen
import cl.clinipets.mascotas.MascotasViewModelFactory
import cl.clinipets.perfil.ProfileScreen
import cl.clinipets.reservas.ReservasScreen
import cl.clinipets.reservas.ReservasViewModelFactory
import cl.clinipets.ui.screens.HomeScreen
import cl.clinipets.ui.screens.LoginScreen
import cl.clinipets.ui.theme.ClinipetsTheme
import cl.clinipets.veterinario.OnboardingVetScreen
import cl.clinipets.veterinario.VeterinarioViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClinipetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            val vm: AuthViewModel = viewModel(factory = AuthViewModelFactory(applicationContext))
                            LoginScreen(
                                viewModel = vm,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            val vm: AuthViewModel = viewModel(factory = AuthViewModelFactory(applicationContext))
                            HomeScreen(
                                onNavigate = { route -> navController.navigate(route) },
                                onSignOut = {
                                    vm.signOut()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("perfil") {
                            val vm: AuthViewModel = viewModel(factory = AuthViewModelFactory(applicationContext))
                            ProfileScreen(viewModel = vm, onBeVet = { navController.navigate("onboard_vet") })
                        }

                        composable("onboard_vet") {
                            OnboardingVetScreen(viewModelFactory = VeterinarioViewModelFactory(applicationContext), onSuccess = { navController.navigate("home") { popUpTo("onboard_vet") { inclusive = true } } })
                        }

                        composable("mascotas") {
                            MascotasScreen(viewModelFactory = MascotasViewModelFactory(applicationContext))
                        }

                        composable("descubrimiento") {
                            DescubrimientoScreen(viewModelFactory = DescubrimientoViewModelFactory(applicationContext))
                        }

                        composable("reservas") {
                            ReservasScreen(viewModelFactory = ReservasViewModelFactory(applicationContext))
                        }
                    }
                }
            }
        }
    }
}