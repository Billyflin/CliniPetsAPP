package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cl.clinipets.auth.AuthViewModel
import cl.clinipets.auth.AuthViewModelFactory

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val vm: AuthViewModel = viewModel(factory = AuthViewModelFactory(context))

    val isLoading by vm.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        vm.fetchProfile()
    }

    // cuando isLoading pasa a false, verificamos profile y navegamos
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            val profile = vm.profile.value
            if (profile != null) {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
