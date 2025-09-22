package cl.clinipets.auth.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import cl.clinipets.auth.presentation.LoginViewModel
import cl.clinipets.auth.ui.LoginScreen
import cl.clinipets.attention.navigation.AttDest
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
            LoginScreen(vm = vm) {
                // Navega al "home" de Tutor (o lo que definas)
                nav.navigate(AttDest.Graph) {
                    popUpTo(AuthDest.Graph) { inclusive = true }
                }
            }
        }

        composable<AuthDest.Account> {
            // TODO: AccountScreen()
        }
    }
}
