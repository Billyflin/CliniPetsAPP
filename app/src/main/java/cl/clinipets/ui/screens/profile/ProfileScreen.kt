// ui/screens/profile/ProfileScreen.kt
package cl.clinipets.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.ui.theme.Contrast
import cl.clinipets.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extColors = LocalExtendedColors.current
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mi Perfil",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Configuración")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header
            ProfileHeader(
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                userPhotoUrl = uiState.userPhotoUrl
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = uiState.petsCount.toString(),
                    label = "Mascotas",
                    color = extColors.mint.colorContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = uiState.appointmentsCount.toString(),
                    label = "Citas",
                    color = extColors.lavander.colorContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${uiState.memberSince}",
                    label = "Miembro desde",
                    color = extColors.peach.colorContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Section
            SectionTitle("Cuenta")
            ProfileMenuItem(
                icon = Icons.Outlined.Person,
                title = "Información Personal",
                subtitle = "Edita tu perfil",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.Outlined.Notifications,
                title = "Notificaciones",
                subtitle = "Configura las alertas",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.Outlined.Security,
                title = "Seguridad",
                subtitle = "Contraseña y autenticación",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preferences Section
            SectionTitle("Preferencias")

            // Theme Settings
            ThemeSettingsCard(
                isDarkMode = uiState.isDarkMode,
                isDynamicColor = uiState.isDynamicColor,
                contrast = uiState.contrast,
                onDarkModeChange = viewModel::updateDarkMode,
                onDynamicColorChange = viewModel::updateDynamicColor,
                onContrastChange = viewModel::updateContrast
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Help Section
            SectionTitle("Ayuda")
            ProfileMenuItem(
                icon = Icons.Outlined.HelpOutline,
                title = "Centro de Ayuda",
                subtitle = "Preguntas frecuentes",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.Outlined.Email,
                title = "Contacto",
                subtitle = "Envíanos un mensaje",
                onClick = { /* TODO */ }
            )
            ProfileMenuItem(
                icon = Icons.Outlined.Info,
                title = "Acerca de",
                subtitle = "Versión 1.0.0",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Out Button
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = null
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                        onSignOut()
                    }
                ) {
                    Text("Cerrar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    userName: String?,
    userEmail: String?,
    userPhotoUrl: String?
) {
    val extColors = LocalExtendedColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = extColors.pink.colorContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (userPhotoUrl != null) {
                        // TODO: Load actual image
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = extColors.pink.onColorContainer
                        )
                    } else {
                        Text(
                            userName?.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = extColors.pink.onColorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                userName ?: "Usuario",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                userEmail ?: "email@example.com",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeSettingsCard(
    isDarkMode: Boolean,
    isDynamicColor: Boolean,
    contrast: Contrast,
    onDarkModeChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onContrastChange: (Contrast) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dark Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Modo Oscuro")
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange
                )
            }

            HorizontalDivider()

            // Dynamic Color Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Color Dinámico")
                }
                Switch(
                    checked = isDynamicColor,
                    onCheckedChange = onDynamicColorChange
                )
            }

            HorizontalDivider()

            // Contrast Selector
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Contrast,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Contraste")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Contrast.entries.forEach { contrastOption ->
                        FilterChip(
                            selected = contrast == contrastOption,
                            onClick = { onContrastChange(contrastOption) },
                            label = {
                                Text(
                                    when (contrastOption) {
                                        Contrast.Standard -> "Estándar"
                                        Contrast.Medium -> "Medio"
                                        Contrast.High -> "Alto"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}