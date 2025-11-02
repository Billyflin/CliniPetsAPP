// MainActivity.kt (uso mínimo del botón de login)
package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.auth.ui.LoginViewModel
import cl.clinipets.auth.ui.requestGoogleIdToken
import cl.clinipets.mascotas.ui.MascotasViewModel
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.ui.theme.ClinipetsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClinipetsTheme { ClinipetsApp() }
        }
    }
}

@Composable
private fun ClinipetsApp(vm: LoginViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val state by vm.ui.collectAsState()
    val context = LocalContext.current
    var requestingGoogle by remember { mutableStateOf(false) }
    val busy = requestingGoogle || state.isAuthenticating

    Surface(Modifier.fillMaxSize()) {
        when {
            state.isCheckingSession -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.ok -> {
                MascotasScreen(onLogout = { vm.logout() })
            }
            else -> {
                LoginScreen(
                    busy = busy,
                    error = state.error,
                    onLoginClick = {
                        scope.launch {
                            try {
                                requestingGoogle = true
                                val token = requestGoogleIdToken(
                                    context = context,
                                    serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
                                )
                                if (!token.isNullOrBlank()) {
                                    vm.loginWithGoogleIdToken(token)
                                }
                            } finally {
                                requestingGoogle = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    busy: Boolean,
    error: String?,
    onLoginClick: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                enabled = !busy,
                onClick = onLoginClick
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Iniciar sesión con Google")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MascotasScreen(
    onLogout: () -> Unit,
    vm: MascotasViewModel = hiltViewModel()
) {
    val mascotas by vm.items.collectAsState()

    LaunchedEffect(Unit) {
        vm.cargar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis mascotas") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        if (mascotas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no tienes mascotas registradas.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mascotas, key = { it.id ?: it.nombre }) { mascota ->
                    MascotaCard(mascota)
                }
            }
        }
    }
}

@Composable
private fun MascotaCard(mascota: Mascota) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = mascota.nombre,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Especie: ${mascota.especie.value}",
                style = MaterialTheme.typography.bodyMedium
            )
            mascota.raza?.takeIf { it.isNotBlank() }?.let { raza ->
                Text(
                    text = "Raza: $raza",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
