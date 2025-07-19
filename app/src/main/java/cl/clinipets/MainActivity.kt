// MainActivity.kt (CORREGIDO)
package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import cl.clinipets.navigation.NavigationGraph
import cl.clinipets.navigation.NavigationState
import cl.clinipets.navigation.Route
import cl.clinipets.navigation.rememberNavigationState
import cl.clinipets.ui.AppViewModel
import cl.clinipets.ui.components.ClinipetsBottomBar
import cl.clinipets.ui.theme.ClinipetsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClinipetsTheme {
                ClinipetsApp()
            }
        }
    }
}

@Composable
fun ClinipetsApp(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val appState by appViewModel.appState.collectAsStateWithLifecycle()
    val navigationState = rememberNavigationState()

    ClinipetsTheme(
        darkTheme = appState.isDarkMode,
        dynamicColor = appState.isDynamicColor,
        contrast = appState.contrast
    ) {
        ClinipetsNavHost(
            navigationState = navigationState,
            isAuthenticated = appState.isAuthenticated,
            hasCompletedOnboarding = appState.hasCompletedOnboarding,
            onAuthStateChanged = { isAuthenticated ->
                appViewModel.updateAuthState(isAuthenticated)
            }
        )
    }
}

@Composable
fun ClinipetsNavHost(
    navigationState: NavigationState,
    isAuthenticated: Boolean,
    hasCompletedOnboarding: Boolean,
    onAuthStateChanged: (Boolean) -> Unit
) {
    val navBackStackEntry by navigationState.navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine start destination
    val startDestination = when {
        !hasCompletedOnboarding -> Route.Splash
        !isAuthenticated -> Route.Login
        else -> Route.Home
    }

    // Check if current route should show bottom bar
    val shouldShowBottomBar = currentRoute in listOf(
        Route.Home::class.qualifiedName,
        Route.Appointments::class.qualifiedName,
        Route.Pets::class.qualifiedName,
        Route.Profile::class.qualifiedName
    )

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar && isAuthenticated) {
                ClinipetsBottomBar(
                    navigationState = navigationState
                )
            }
        }
    ) { paddingValues ->
        NavigationGraph(
            navController = navigationState.navController,
            startDestination = startDestination,
            onAuthStateChanged = onAuthStateChanged,
            modifier = Modifier
                .fillMaxSize()
                // Aplica solo el padding inferior si hay bottom bar
                // o cualquier otro padding que necesites
                .padding(bottom = paddingValues.calculateBottomPadding())
            // Si quieres que el contenido vaya bajo la barra de estado,
            // no apliques paddingValues.calculateTopPadding() aquí.
            // Si tus pantallas individuales necesitan un padding superior,
            // lo aplicas dentro de cada pantalla.
        )
    }
}

@Preview
@Composable
fun ClinipetsAppPreview() {
    ClinipetsTheme {
        ClinipetsApp()
    }
}