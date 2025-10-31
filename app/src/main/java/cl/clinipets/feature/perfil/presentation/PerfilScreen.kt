package cl.clinipets.feature.perfil.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.feature.auth.presentation.AuthUiState
import cl.clinipets.openapi.models.VeterinarioPerfil
import java.util.Locale

@Composable
fun PerfilRoute(
    estado: AuthUiState,
    onBack: () -> Unit,
    onCerrarSesion: () -> Unit,
    onIrOnboardingVet: () -> Unit,
    onIrPerfilVet: () -> Unit,
    shouldRefreshVeterinario: Boolean = false,
    onVeterinarioRefreshConsumed: () -> Unit = {},
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val estadoVeterinario by viewModel.estadoVeterinario.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refrescar()
    }

    LaunchedEffect(shouldRefreshVeterinario) {
        if (shouldRefreshVeterinario) {
            viewModel.refrescar()
            onVeterinarioRefreshConsumed()
        }
    }

    PerfilScreen(
        estadoAuth = estado,
        estadoVeterinario = estadoVeterinario,
        onBack = onBack,
        onCerrarSesion = onCerrarSesion,
        onIniciarSolicitud = onIrOnboardingVet,
        onEditarSolicitud = onIrOnboardingVet,
        onAbrirPanelVeterinario = onIrPerfilVet,
        onRefrescarSolicitud = viewModel::refrescar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerfilScreen(
    estadoAuth: AuthUiState,
    estadoVeterinario: PerfilVeterinarioUiState,
    onBack: () -> Unit,
    onCerrarSesion: () -> Unit,
    onIniciarSolicitud: () -> Unit,
    onEditarSolicitud: () -> Unit,
    onAbrirPanelVeterinario: () -> Unit,
    onRefrescarSolicitud: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val perfil = estadoAuth.perfil

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Mi perfil") },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Datos personales",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(text = perfil?.email ?: "Correo no disponible")
                    perfil?.nombre?.takeIf { it.isNotBlank() }?.let {
                        Text(text = it)
                    }
                    val roles = perfil?.roles.orEmpty()
                    if (roles.isNotEmpty()) {
                        Text(text = "Roles: ${roles.joinToString()}")
                    }
                }
            }

            VeterinarioSection(
                estado = estadoVeterinario,
                onIniciarSolicitud = onIniciarSolicitud,
                onEditarSolicitud = onEditarSolicitud,
                onAbrirPanelVeterinario = onAbrirPanelVeterinario,
                onRefrescarSolicitud = onRefrescarSolicitud,
            )

            estadoAuth.error?.let { error ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Error al actualizar tus datos",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(text = error.mensaje ?: "No pudimos actualizar tus datos.")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth(),
                enabled = !estadoAuth.cargando,
            ) {
                Text(text = "Cerrar sesión")
            }
        }
    }
}

@Composable
private fun VeterinarioSection(
    estado: PerfilVeterinarioUiState,
    onIniciarSolicitud: () -> Unit,
    onEditarSolicitud: () -> Unit,
    onAbrirPanelVeterinario: () -> Unit,
    onRefrescarSolicitud: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Servicios profesionales",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = "Activa tu perfil veterinario para recibir reservas y aparecer en la búsqueda de tutores.",
            style = MaterialTheme.typography.bodyMedium,
        )

        when {
            estado.cargando -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            estado.error != null -> {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "No pudimos obtener tu información.",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Text(text = estado.error.mensaje ?: "Intenta nuevamente en unos minutos.")
                        TextButton(onClick = onRefrescarSolicitud) {
                            Text(text = "Reintentar")
                        }
                    }
                }
            }

            estado.sinPerfil -> {
                PerfilNoRegistradoCard(onIniciarSolicitud = onIniciarSolicitud)
            }

            estado.perfil != null -> {
                SolicitudVeterinariaCard(
                    perfil = estado.perfil,
                    onEditarSolicitud = onEditarSolicitud,
                    onAbrirPanelVeterinario = onAbrirPanelVeterinario,
                    onRefrescarSolicitud = onRefrescarSolicitud,
                )
            }
        }
    }
}

@Composable
private fun PerfilNoRegistradoCard(
    onIniciarSolicitud: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Activa tu modo veterinario",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Completa la solicitud para empezar a recibir reservas y mostrar tu disponibilidad.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Button(
                onClick = onIniciarSolicitud,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Iniciar solicitud")
            }
        }
    }
}

@Composable
private fun SolicitudVeterinariaCard(
    perfil: VeterinarioPerfil,
    onEditarSolicitud: () -> Unit,
    onAbrirPanelVeterinario: () -> Unit,
    onRefrescarSolicitud: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (perfil.verificado) {
                EstadoChip(
                    texto = "Perfil verificado",
                    icon = Icons.Default.CheckCircle,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Tus servicios ya aparecen en la búsqueda de CliniPets. Mantén tu agenda al día para recibir reservas.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                EstadoChip(
                    texto = "Solicitud en revisión",
                    icon = Icons.Default.Schedule,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "Nuestro equipo revisará tu información en las próximas horas. Te avisaremos por correo cuando finalice la verificación.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoFila(titulo = "Nombre profesional", valor = perfil.nombreCompleto)
                perfil.numeroLicencia?.let {
                    InfoFila(titulo = "Nº de licencia", valor = it)
                }
                InfoFila(
                    titulo = "Modos de atención",
                    valor = perfil.modosAtencion.joinToString { it.value },
                )
                val coordenadas = perfil.latitud?.let { lat ->
                    perfil.longitud?.let { lng ->
                        String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng)
                    }
                }
                coordenadas?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(text = it)
                    }
                }
                perfil.radioCobertura?.let { km ->
                    InfoFila(
                        titulo = "Radio de cobertura",
                        valor = "${"%.1f".format(km)} km",
                    )
                }
            }

            if (perfil.verificado) {
                Button(
                    onClick = onAbrirPanelVeterinario,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Abrir herramientas de veterinario")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onEditarSolicitud,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Editar mi solicitud")
                    }
                    TextButton(
                        onClick = onRefrescarSolicitud,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Actualizar estado")
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoChip(
    texto: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    AssistChip(
        onClick = {},
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
            )
        },
        label = { Text(text = texto) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            leadingIconContentColor = contentColor,
        ),
    )
}

@Composable
private fun InfoFila(
    titulo: String,
    valor: String,
) {
    Column {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Text(text = valor, style = MaterialTheme.typography.bodyMedium)
    }
}
