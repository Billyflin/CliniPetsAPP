package cl.clinipets.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.ui.auth.LoginViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: LoginViewModel.UiState,
    onBack: () -> Unit,
    onBecomeVeterinarian: () -> Unit,
    onEditProfessional: () -> Unit,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit,
    vm  : ProfileViewModel = hiltViewModel()
) {
    val me = state.me
    val roles = (state.roles.ifEmpty { me?.roles.orEmpty() }).ifEmpty { listOf("CLIENTE") }
    val displayName = state.displayName ?: me?.nombre
    val fotoUrl = me?.fotoUrl
    val canBecomeVet = roles.any { it.equals("CLIENTE", ignoreCase = true) } &&
            roles.none { it.equals("VETERINARIO", ignoreCase = true) }

    val ui by vm.ui.collectAsState()
    // Esta pantalla es la responsable de cargar el perfil
    LaunchedEffect(Unit) { vm.loadMyProfile() }

    val perfil = ui.perfil
    val isLoading = ui.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick =    onRefreshProfile) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!fotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = fotoUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    Text(
                        text = displayName ?: "Sin nombre",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = me?.email ?: "Sin correo",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Roles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    roles.forEach { role ->
                        AssistChip(
                            onClick = {},
                            label = { Text(role) },
                            colors = AssistChipDefaults.assistChipColors()
                        )
                    }
                }
            }

            // Estado del perfil profesional (prellenado con mensajes en español)
            when {
                isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Cargando perfil profesional…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                perfil != null && !perfil.verificado -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Solicitud enviada. Pendiente de verificación.") },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                }
                perfil?.verificado == true -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Veterinario verificado") },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                }
            }

            // Editar datos profesionales: solo si el perfil está verificado
            Button(
                onClick = onEditProfessional,
                enabled = perfil?.verificado == true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Editar datos profesionales")
            }

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
            }

            if (canBecomeVet) {
                Button(
                    onClick = onBecomeVeterinarian,
                    // No permitir registrar mientras estamos cargando el perfil desde el backend
                    enabled = !isLoading && perfil == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.MedicalServices, contentDescription = null)
                    Text("Ser veterinario", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}