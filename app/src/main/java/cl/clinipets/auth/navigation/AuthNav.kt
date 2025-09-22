package cl.clinipets.auth.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import cl.clinipets.attention.navigation.AttDest
import cl.clinipets.auth.presentation.LoginViewModel
import cl.clinipets.auth.ui.AccountScreen
import cl.clinipets.auth.ui.LoginScreen
import cl.clinipets.home.navigation.HomeDest
import kotlinx.serialization.Serializable

// AuthNav.kt
sealed interface AuthDest {
    @Serializable data object Graph : AuthDest
    @Serializable data object Login : AuthDest
    @Serializable data object Account : AuthDest
}

fun NavGraphBuilder.authGraph(nav: NavController) {
    navigation<AuthDest.Graph>(startDestination = AuthDest.Login) {

        composable<AuthDest.Login> {
            val vm: LoginViewModel = hiltViewModel()

            if (vm.isLoggedIn()) {
                LaunchedEffect(Unit) {
                    nav.navigate(HomeDest.Graph) {
                        popUpTo(AuthDest.Graph) { inclusive = true }
                    }
                }
            } else {
                LoginScreen(vm = vm) {
                    // Navega al "home" de Tutor (o lo que definas)
                    nav.navigate(AttDest.Graph) {
                        popUpTo(AuthDest.Graph) { inclusive = true }
                    }
                }
            }
        }

        composable<AuthDest.Account> {
            AccountScreen(
                onLogout = {
                    // Vuelve al flujo de Auth (Login)
                    nav.navigate(AuthDest.Graph) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

    }
}
