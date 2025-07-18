// ui/screens/home/HomeScreen.kt
package cl.clinipets.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.clinipets.navigation.ClinipetsDestination
import cl.clinipets.ui.theme.*
import kotlinx.coroutines.launch




@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    var fabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "¡Hola! 👋",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Bienvenido a Clinipets",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Pets, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                val navigationItems = listOf(
                    NavigationItem("Inicio", Icons.Filled.Home, Icons.Outlined.Home, ClinipetsDestination.Home),
                    NavigationItem("Citas", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, ClinipetsDestination.Appointments),
                    NavigationItem("Mascotas", Icons.Filled.Pets, Icons.Outlined.Pets, ClinipetsDestination.Pets),
                    NavigationItem("Perfil", Icons.Filled.Person, Icons.Outlined.Person, ClinipetsDestination.Profile)
                )

                navigationItems.forEach { item ->
                    NavigationBarItem(
                        selected = false, // Actualizar con la ruta actual
                        onClick = {

                        },
                        icon = {
                            Icon(
                                if (false) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Actions
                item {
                    QuickActionsSection()
                }

                // Next Appointment
                if (uiState.nextAppointment != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                        }
                    }
                }

                // Pets Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tus Mascotas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {  }
                        ) {
                            Text("Ver todas")
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }



                // Services Section
                item {
                    Text(
                        "Servicios Destacados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    ServicesGrid()
                }
            }

            // Floating Action Button Menu
            FloatingActionButtonMenu(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                expanded = fabMenuExpanded,
                button = {
                    LargeFloatingActionButton(
                        onClick = { fabMenuExpanded = !fabMenuExpanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        AnimatedContent(
                            targetState = fabMenuExpanded,
                            transitionSpec = {
                                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                            }
                        ) { expanded ->
                            Icon(
                                if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            ) {
                val menuItems = listOf(
                    Triple(Icons.Filled.CalendarMonth, "Nueva Cita", MaterialTheme.colorScheme.primaryContainer),
                    Triple(Icons.Filled.Pets, "Agregar Mascota", MaterialTheme.colorScheme.secondaryContainer),
                    Triple(Icons.Filled.Emergency, "Emergencia", MaterialTheme.colorScheme.errorContainer)
                )

                menuItems.forEach { (icon, label, color) ->
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabMenuExpanded = false
                            when (label) {
                                "Nueva Cita" -> {}
                                "Agregar Mascota" -> {}
                                "Emergencia" -> { /* Handle emergency */ }
                            }
                        },
                        containerColor = color,
                        icon = { Icon(icon, contentDescription = null) },
                        text = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionChip(
            icon = Icons.Outlined.Vaccines,
            text = "Vacunas",
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            icon = Icons.Outlined.MedicalServices,
            text = "Consulta",
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            icon = Icons.Outlined.Phone,
            text = "Contacto",
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionChip(
    icon: ImageVector,
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color,
        onClick = { }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServicesGrid() {
    val services = listOf(
        Triple(Icons.Filled.Vaccines, "Vacunación", Pink100),
        Triple(Icons.Filled.MedicalServices, "Consulta General", Mint100),
        Triple(Icons.Filled.Healing, "Cirugías", Lavender100),
        Triple(Icons.Filled.Spa, "Baño y Peluquería", Peach100),
        Triple(Icons.Filled.Home, "Servicio a Domicilio", Pink100),
        Triple(Icons.Filled.Science, "Laboratorio", Mint100)
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        services.forEach { (icon, name, color) ->
            AssistChip(
                onClick = { },
                label = { Text(name) },
                leadingIcon = {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = color
                )
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val destination: ClinipetsDestination
)