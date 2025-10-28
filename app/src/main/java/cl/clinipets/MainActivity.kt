package cl.clinipets

import android.content.Intent
import android.net.Uri
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import cl.clinipets.auth.AuthEvents
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    // Callback para reenviar deep links a Compose cuando la Activity recibe nuevos intents
    private var onNewDeepLink: ((Uri) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClinipetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // Expone callback para manejar nuevos intents (Activity -> Compose)
                    onNewDeepLink = { uri ->
                        val normalized = normalizeUri(uri)
                        val deepIntent = Intent(Intent.ACTION_VIEW, normalized)
                        navController.handleDeepLink(deepIntent)
                    }
                    // Maneja el intent inicial (si vino con deep link)
                    LaunchedEffect(Unit) {
                        intent?.data?.let { data ->
                            val normalized = normalizeUri(data)
                            val deepIntent = Intent(Intent.ACTION_VIEW, normalized)
                            navController.handleDeepLink(deepIntent)
                        }
                    }

                    // Única instancia compartida de AuthViewModel
                    val authVm: AuthViewModel = viewModel(factory = AuthViewModelFactory(applicationContext))

                    // Redirección global a Login si la sesión expira (refresh fallido)
                    LaunchedEffect(Unit) {
                        AuthEvents.sessionExpired.collect {
                            authVm.signOut()
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "splash") {
                        composable(
                            route = "splash",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/splash" }
                            )
                        ) { SplashScreen(navController = navController, viewModel = authVm) }

                        composable(
                            route = "login",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/login" }
                            )
                        ) {
                            LoginScreen(
                                viewModel = authVm,
                                onLoginSuccess = {
                                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                }
                            )
                        }

                        composable(
                            route = "home",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/" },
                                navDeepLink { uriPattern = "https://clinipets.cl/home" }
                            )
                        ) {
                            val profile by authVm.profile.collectAsState()
                            val isVet = profile?.roles?.contains("VETERINARIO") == true
                            HomeScreen(
                                onNavigate = { route -> navController.navigate(route) },
                                onSignOut = {
                                    authVm.signOut(); navController.navigate("login") { popUpTo("home") { inclusive = true } }
                                },
                                isVet = isVet
                            )
                        }

                        composable(
                            route = "perfil",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/perfil" }
                            )
                        ) { ProfileScreen(viewModel = authVm, onBeVet = { navController.navigate("onboard_vet") }, onVetProfile = { navController.navigate("perfil_vet") }) }

                        composable(
                            route = "perfil_vet",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/perfil-vet" }
                            )
                        ) { cl.clinipets.veterinario.PerfilVetScreen(viewModelFactory = VeterinarioViewModelFactory(applicationContext)) }

                        composable(
                            route = "onboard_vet",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/ser-veterinario" }
                            )
                        ) {
                            OnboardingVetScreen(viewModelFactory = VeterinarioViewModelFactory(applicationContext), onSuccess = {
                                authVm.fetchProfile(); navController.navigate("home") { popUpTo("onboard_vet") { inclusive = true } }
                            })
                        }

                        composable(
                            route = "mascotas",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/mascotas" }
                            )
                        ) { MascotasScreen(viewModelFactory = MascotasViewModelFactory(applicationContext)) }

                        composable(
                            route = "descubrimiento",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/descubrimiento" },
                                navDeepLink { uriPattern = "https://clinipets.cl/descubrir" }
                            )
                        ) {
                            DescubrimientoScreen(
                                viewModelFactory = DescubrimientoViewModelFactory(applicationContext),
                                onReservarDesdeOferta = { sku, especie ->
                                    val skuEnc = URLEncoder.encode(sku, StandardCharsets.UTF_8.name())
                                    val espEnc = especie?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) } ?: ""
                                    navController.navigate("nueva_reserva?sku=$skuEnc&especie=$espEnc")
                                }
                            )
                        }

                        composable(
                            route = "reservas",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/reservas" }
                            )
                        ) { ReservasScreen(viewModelFactory = ReservasViewModelFactory(applicationContext)) }

                        composable(
                            route = "nueva_reserva?sku={sku}&especie={especie}",
                            arguments = listOf(
                                navArgument("sku") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("especie") { type = NavType.StringType; nullable = true; defaultValue = null }
                            ),
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/reservar" },
                                navDeepLink { uriPattern = "https://clinipets.cl/reservar?sku={sku}" },
                                navDeepLink { uriPattern = "https://clinipets.cl/reservar?especie={especie}" },
                                navDeepLink { uriPattern = "https://clinipets.cl/reservar?sku={sku}&especie={especie}" }
                            )
                        ) { backStackEntry ->
                            val sku = backStackEntry.arguments?.getString("sku")
                            val especie = backStackEntry.arguments?.getString("especie")
                            ReservaFlowScreen(viewModelFactory = ReservaFlowViewModelFactory(applicationContext), initialSku = sku, initialEspecie = especie)
                        }

                        composable(
                            route = "juntas",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/juntas" }
                            )
                        ) { JuntasScreen(viewModelFactory = JuntasViewModelFactory(applicationContext)) }

                        composable(
                            route = "disponibilidad_vet",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/disponibilidad" }
                            )
                        ) { cl.clinipets.disponibilidad.DisponibilidadScreen(viewModelFactory = cl.clinipets.disponibilidad.DisponibilidadViewModelFactory(applicationContext)) }

                        composable(
                            route = "agenda_vet",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/agenda-vet" }
                            )
                        ) { cl.clinipets.agenda.AgendaVetScreen(viewModelFactory = cl.clinipets.agenda.AgendaVetViewModelFactory(applicationContext)) }

                        composable(
                            route = "clinica",
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://clinipets.cl/clinica" }
                            )
                        ) { cl.clinipets.clinica.ClinicaScreen(viewModelFactory = cl.clinipets.clinica.ClinicaViewModelFactory(applicationContext)) }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri -> onNewDeepLink?.invoke(uri) }
    }

    private fun normalizeUri(uri: Uri): Uri {
        return when {
            uri.scheme == "clinipets" -> {
                val host = uri.host
                val encodedPath = uri.encodedPath ?: ""
                val fullPath = buildString {
                    if (!host.isNullOrEmpty()) {
                        append('/')
                        append(host)
                    }
                    if (encodedPath.isNotEmpty()) {
                        if (!encodedPath.startsWith('/')) append('/')
                        append(encodedPath.trimStart('/'))
                    }
                }
                val path = if (fullPath.isEmpty()) "/" else fullPath
                val query = uri.encodedQuery?.let { "?$it" } ?: ""
                val frag = uri.encodedFragment?.let { "#$it" } ?: ""
                Uri.parse("https://clinipets.cl$path$query$frag")
            }
            uri.scheme == "https" && uri.host == "www.clinipets.cl" -> {
                val path = uri.encodedPath ?: ""
                val query = uri.encodedQuery?.let { "?$it" } ?: ""
                val frag = uri.encodedFragment?.let { "#$it" } ?: ""
                Uri.parse("https://clinipets.cl$path$query$frag")
            }
            else -> uri
        }
    }
}