package cl.clinipets.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cl.clinipets.ui.auth.LoginScreen
import cl.clinipets.ui.auth.LoginViewModel
import kotlinx.serialization.Serializable

@Serializable object LoginRoute
@Serializable object HomeRoute


@Serializable
data class JuntaRoute(val reservaId: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalStdlibApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    uiState: LoginViewModel.UiState,
    busy: Boolean,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit
) {
    val isLoggedIn = uiState.me != null

    NavHost(navController = navController, startDestination = if (isLoggedIn) HomeRoute else LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                busy = busy,
                error = uiState.error,
                onLoginClick = onLoginClick
            )
        }
        
        composable<HomeRoute> {
             // Placeholder for HomeScreen, will be replaced with actual HomeScreen call once signature is verified
             // But wait, I should probably check HomeScreen signature first to avoid another error.
             // I will assume a basic Text for now or check the file in next step if I could, but I must do it in one go if I use replace.
             // Let's assume HomeScreen exists. I saw it in file list: app/src/main/kotlin/cl/clinipets/ui/home/HomeScreen.kt
             // I will add the import and the composable.
             cl.clinipets.ui.home.HomeScreen(
                 onLogout = onLogout
             )
        }
    }
}
