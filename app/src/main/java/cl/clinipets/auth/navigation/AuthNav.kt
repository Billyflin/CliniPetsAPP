package cl.clinipets.auth.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import cl.clinipets.auth.presentation.AuthUiState
import cl.clinipets.auth.presentation.LoginViewModel
import cl.clinipets.auth.ui.AccountScreen
import cl.clinipets.auth.ui.LoginScreen
import cl.clinipets.home.navigation.HomeDest
import kotlinx.serialization.Serializable

// Destinos Auth
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

        composable<AuthDest.Login> {
            val vm: LoginViewModel = hiltViewModel()
            val uiState by vm.uiState.collectAsStateWithLifecycle()

            // Navegación impulsada por estado (segura ante recomposiciones)
            LaunchedEffect(uiState) {
                if (uiState == AuthUiState.LoggedIn) {
                    nav.navigate(HomeDest.Graph) {
                        popUpTo(AuthDest.Graph) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            when (uiState) {
                AuthUiState.Loading -> LoginScreen(uiState, vm::signIn, vm.errors)
                AuthUiState.LoggedOut -> LoginScreen(uiState, vm::signIn, vm.errors)
                AuthUiState.LoggedIn -> Unit // LaunchedEffect ya navegó
            }
        }

        composable<AuthDest.Account> {
            val vm: LoginViewModel = hiltViewModel()
            AccountScreen(
                onLogout = {
                    vm.logout()
                    nav.navigate(AuthDest.Graph) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
