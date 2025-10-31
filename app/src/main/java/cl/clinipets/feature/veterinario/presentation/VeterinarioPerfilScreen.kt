package cl.clinipets.feature.veterinario.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.dp
import cl.clinipets.openapi.models.VeterinarioPerfil

@Composable
fun VeterinarioPerfilRoute(
    onBack: () -> Unit,
    onIrAgenda: () -> Unit,
    onIrOnboarding: () -> Unit,
    viewModel: VeterinarioPerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsState()
    VeterinarioPerfilScreen(
        estado = estado,
        onBack = onBack,
        onIrAgenda = onIrAgenda,
        onIrOnboarding = onIrOnboarding,
        onReintentar = viewModel::refrescar,
    )
}

data class VeterinarioPerfilUiView(
    val nombre: String,
    val verificado: Boolean,
    val licencia: String?,
    val modosAtencion: String,
    val ubicacion: String?,
    val radio: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VeterinarioPerfilScreen(
    estado: VeterinarioPerfilUiState,
    onBack: () -> Unit,
    onIrAgenda: () -> Unit,
    onIrOnboarding: () -> Unit,
    onReintentar: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Perfil veterinario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                estado.cargando -> Text(text = "Cargando...")
                estado.sinPerfil -> PerfilNoRegistrado(onIrOnboarding)
                estado.error != null -> {
                    Text(text = estado.error.mensaje ?: "No pudimos obtener tu perfil.")
                    Button(onClick = onReintentar) {
                        Text(text = "Reintentar")
                    }
                }
                estado.perfil != null -> ContenidoPerfil(estado.perfil, onIrAgenda)
            }
        }
    }
}

@Composable
private fun PerfilNoRegistrado(onIrOnboarding: () -> Unit) {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Aun no activas tu perfil de veterinario.")
        Text(text = "Completa el registro para gestionar tu agenda y aparecer en los resultados.")
        Button(
            onClick = onIrOnboarding,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        ) {
            Text(text = "Activar cuenta")
        }
    }
}

@Composable
private fun ContenidoPerfil(perfil: VeterinarioPerfil, onIrAgenda: () -> Unit) {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Nombre: ${perfil.nombreCompleto}")
        Text(text = "Estado: ${if (perfil.verificado) "Verificado" else "Pendiente"}")
        perfil.numeroLicencia?.let { Text(text = "Licencia: $it") }
        Text(text = "Modos de atención: ${perfil.modosAtencion.joinToString { it.value }}")
        perfil.latitud?.let { lat ->
            val lng = perfil.longitud
            Text(text = "Ubicación: ${lat}, ${lng ?: "-"}")
        }
        perfil.radioCobertura?.let { Text(text = "Radio cobertura: ${"%.1f".format(it)} km") }
        Button(onClick = onIrAgenda, modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
            Text(text = "Gestionar agenda")
        }
    }
}
