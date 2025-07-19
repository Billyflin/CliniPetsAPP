package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    // Observar cambios de autenticación
    LaunchedEffect(appState.isAuthenticated) {
        if (!appState.isAuthenticated && navigationState.navController.currentDestination?.route != Route.Login::class.qualifiedName) {
            navigationState.navController.navigate(Route.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

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
    ) && isAuthenticated

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
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
                .padding(bottom = paddingValues.calculateBottomPadding())
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