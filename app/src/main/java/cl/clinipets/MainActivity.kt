package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.clinipets.auth.AuthViewModel
import cl.clinipets.auth.AuthViewModelFactory
import cl.clinipets.descubrimiento.DescubrimientoScreen
import cl.clinipets.descubrimiento.DescubrimientoViewModelFactory
import cl.clinipets.juntas.JuntasScreen
import cl.clinipets.juntas.JuntasViewModelFactory
import cl.clinipets.mascotas.MascotasScreen
import cl.clinipets.mascotas.MascotasViewModelFactory
import cl.clinipets.perfil.ProfileScreen
import cl.clinipets.reservas.ReservaFlowScreen
import cl.clinipets.reservas.ReservaFlowViewModelFactory
import cl.clinipets.reservas.ReservasScreen
import cl.clinipets.reservas.ReservasViewModelFactory
import cl.clinipets.ui.screens.HomeScreen
import cl.clinipets.ui.screens.LoginScreen
import cl.clinipets.ui.screens.SplashScreen
import cl.clinipets.ui.theme.ClinipetsTheme
import cl.clinipets.veterinario.OnboardingVetScreen
import cl.clinipets.veterinario.VeterinarioViewModelFactory
import cl.clinipets.auth.AuthEvents

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
                    // Única instancia compartida de AuthViewModel
                    val authVm: AuthViewModel = viewModel(factory = AuthViewModelFactory(applicationContext))

                    // Redirección global a Login si la sesión expira (refresh fallido)
                    LaunchedEffect(Unit) {
                        AuthEvents.sessionExpired.collect {
                            // Limpia estado y navega a login
                            authVm.signOut()
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(navController = navController, viewModel = authVm)
                        }

                        composable("login") {
                            LoginScreen(
                                viewModel = authVm,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            val profile by authVm.profile.collectAsState()
                            val isVet = profile?.roles?.contains("VETERINARIO") == true
                            HomeScreen(
                                onNavigate = { route -> navController.navigate(route) },
                                onSignOut = {
                                    authVm.signOut()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                isVet = isVet
                            )
                        }

                        composable("perfil") {
                            ProfileScreen(
                                viewModel = authVm,
                                onBeVet = { navController.navigate("onboard_vet") },
                                onVetProfile = { navController.navigate("perfil_vet") }
                            )
                        }

                        composable("perfil_vet") {
                            cl.clinipets.veterinario.PerfilVetScreen(viewModelFactory = VeterinarioViewModelFactory(applicationContext))
                        }

                        composable("onboard_vet") {
                            OnboardingVetScreen(viewModelFactory = VeterinarioViewModelFactory(applicationContext), onSuccess = {
                                authVm.fetchProfile()
                                navController.navigate("home") { popUpTo("onboard_vet") { inclusive = true } }
                            })
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

                        composable("nueva_reserva") {
                            ReservaFlowScreen(viewModelFactory = ReservaFlowViewModelFactory(applicationContext))
                        }

                        composable("juntas") {
                            JuntasScreen(viewModelFactory = JuntasViewModelFactory(applicationContext))
                        }

                        composable("disponibilidad_vet") {
                            cl.clinipets.disponibilidad.DisponibilidadScreen(viewModelFactory = cl.clinipets.disponibilidad.DisponibilidadViewModelFactory(applicationContext))
                        }

                        composable("agenda_vet") {
                            cl.clinipets.agenda.AgendaVetScreen(viewModelFactory = cl.clinipets.agenda.AgendaVetViewModelFactory(applicationContext))
                        }

                        composable("clinica") {
                            cl.clinipets.clinica.ClinicaScreen(viewModelFactory = cl.clinipets.clinica.ClinicaViewModelFactory(applicationContext))
                        }
                    }
                }
            }
        }
    }
}