package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import cl.clinipets.auth.AuthViewModel

@Composable
fun SplashScreen(navController: NavController, viewModel: AuthViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val profile by viewModel.profile.collectAsState()

    val started = remember { mutableStateOf(false) }
    val sawLoading = remember { mutableStateOf(false) }
    val navigated = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started.value = true
        viewModel.bootstrap()
    }

    LaunchedEffect(isLoading, profile, started.value, sawLoading.value) {
        if (!started.value || navigated.value) return@LaunchedEffect
        if (isLoading) {
            sawLoading.value = true
            return@LaunchedEffect
        }
        // Solo decidir cuando ya vimos loading alguna vez, evitando el falso negativo inicial
        if (sawLoading.value) {
            navigated.value = true
            if (profile?.authenticated == true) {
                navController.navigate("home") { popUpTo("splash") { inclusive = true } }
            } else {
                navController.navigate("login") { popUpTo("splash") { inclusive = true } }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
