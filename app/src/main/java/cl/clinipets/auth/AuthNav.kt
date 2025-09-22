package cl.clinipets.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import cl.clinipets.AttDest
import cl.clinipets.SimpleScreen
import kotlinx.serialization.Serializable

// Auth
sealed interface AuthDest {
    @Serializable
    data object Graph : AuthDest

    @Serializable
    data object Login : AuthDest

    @Serializable
    data object Account : AuthDest
}

fun NavGraphBuilder.authGraph(nav: NavController) {
    navigation<AuthDest.Graph>(startDestination = AuthDest.Login) {

        // PANTALLA 1: Login
        composable<AuthDest.Login> {
            // si ya tienes LoginViewModel/LoginScreen, reemplaza el SimpleScreen por tu UI:
            // val vm: LoginViewModel = hiltViewModel()
            SimpleScreen(
                title = "Login", primary = "Entrar", onPrimary = {
                    // ejemplo: vm.onSubmit() y navegar al mapa
                    nav.navigate(AttDest.Graph) {
                        popUpTo(AuthDest.Graph) { inclusive = true }
                    }
                })
        }

        composable<AuthDest.Account> {
            SimpleScreen("Cuenta", "Cerrar", onPrimary = { nav.popBackStack() }, showBack = true)
        }
    }
}