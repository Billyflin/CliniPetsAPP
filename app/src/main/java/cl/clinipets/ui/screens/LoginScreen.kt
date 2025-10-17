package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.clinipets.auth.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var token by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Clinipets", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("idToken (para pruebas)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (token.isNotBlank()) {
                    viewModel.loginWithGoogle(token) {
                        onLoginSuccess()
                    }
                } else {
                    // In a real app, trigger Google Sign-In flow; here we require a token for testing
                    viewModel.loginWithGoogle("test-id-token") {
                        onLoginSuccess()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Continuar con Google")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!error.isNullOrEmpty()) {
            Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

