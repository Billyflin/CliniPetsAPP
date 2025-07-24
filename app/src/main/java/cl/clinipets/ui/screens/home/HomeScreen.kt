// ui/screens/HomeScreen.kt
package cl.clinipets.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.ui.theme.LocalExtendedColors
import cl.clinipets.ui.viewmodels.UserViewModel
import cl.clinipets.ui.viewmodels.VetViewModel

// package cl.clinipets.ui.screens.home

// package cl.clinipets.ui.screens.home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPets: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToVetDashboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    vetViewModel: VetViewModel = hiltViewModel()
) {
    val userState by userViewModel.userState.collectAsStateWithLifecycle()
    val vetState by vetViewModel.vetState.collectAsStateWithLifecycle()
    val extColors = LocalExtendedColors.current

    LaunchedEffect(Unit) {
        userViewModel.refreshUser()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hola, ${userState.userName} 👋",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "¿Cómo están tus mascotas hoy?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = extColors.lavander.colorContainer,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onNavigateToProfile() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = extColors.lavander.onColorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cards de resumen con animación
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card Mascotas
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clickable { onNavigateToPets() },
                        colors = CardDefaults.cardColors(
                            containerColor = extColors.pink.colorContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = null,
                                tint = extColors.pink.onColorContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${userState.petsCount}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = extColors.pink.onColorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Mascotas",
                                style = MaterialTheme.typography.bodySmall,
                                color = extColors.pink.onColorContainer
                            )
                        }
                    }

                    // Card Citas
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clickable { onNavigateToAppointments() },
                        colors = CardDefaults.cardColors(
                            containerColor = extColors.mint.colorContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = extColors.mint.onColorContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${userState.appointmentsCount}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = extColors.mint.onColorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Citas",
                                style = MaterialTheme.typography.bodySmall,
                                color = extColors.mint.onColorContainer
                            )
                        }
                    }
                }
            }

            // Sección de acciones rápidas
            item {
                Text(
                    "Acciones rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Card Agendar Cita
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAppointments() },
                    colors = CardDefaults.cardColors(
                        containerColor = extColors.peach.colorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = extColors.peach.color,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = extColors.peach.onColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Agendar Cita",
                                    fontWeight = FontWeight.Bold,
                                    color = extColors.peach.onColorContainer
                                )
                                Text(
                                    "Programa una visita para tu mascota",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.peach.onColorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = extColors.peach.onColorContainer
                        )
                    }
                }
            }

            // Panel Veterinario (si es veterinario)
            if (userState.isVet) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToVetDashboard() },
                        colors = CardDefaults.cardColors(
                            containerColor = extColors.lavander.colorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 2.dp,
                            color = extColors.lavander.color.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = extColors.lavander.color,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.MedicalServices,
                                            contentDescription = null,
                                            tint = extColors.lavander.onColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Panel Veterinario",
                                            fontWeight = FontWeight.Bold,
                                            color = extColors.lavander.onColorContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = extColors.lavander.color,
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Text(
                                                "PRO",
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = extColors.lavander.onColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        "Gestión de consultas y pacientes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extColors.lavander.onColorContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = extColors.lavander.onColorContainer
                            )
                        }
                    }
                }
            }

            // Tips de cuidado
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tip del día",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Recuerda mantener al día las vacunas de tus mascotas. ¡Su salud es lo más importante! 🐾",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}