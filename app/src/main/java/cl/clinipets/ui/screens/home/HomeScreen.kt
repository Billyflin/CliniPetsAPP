// ui/screens/home/HomeScreen.kt
package cl.clinipets.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.navigation.Routes
import cl.clinipets.ui.components.FloatingBottomNavBar
import cl.clinipets.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navigateTo: (Routes) -> Unit,
    selectedNavIndex: Int,
    onNavIndexChanged: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val extColors = LocalExtendedColors.current
    var fabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "¡Hola! 👋",
                            style = MaterialTheme.typography.titleMedium,
                            color = extColors.pink.color
                        )
                        Text(
                            "Bienvenido a Clinipets",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = { },
                        shape = RoundedCornerShape(16.dp),
                        color = extColors.pink.colorContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Pets,
                                contentDescription = null,
                                tint = extColors.pink.onColorContainer
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        onClick = { },
                        shape = RoundedCornerShape(16.dp),
                        color = extColors.lavander.colorContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) { Text("3") }
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = extColors.lavander.onColorContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                scrollBehavior = scrollBehavior
            )
        },


    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Card
                item {
                    HeroCard()
                }

                // Connected Button Group
                item {
                    Text(
                        "¿Qué necesitas hoy?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    ConnectedServiceButtons()
                }

                // Próxima cita
                if (uiState.nextAppointment != null) {
                    item {
                        NextAppointmentCard(uiState.nextAppointment!!)
                    }
                }

                // Mascotas
                item {
                    SectionHeader(
                        title = "Tus Mascotas",
                        actionText = "Ver todas",
                        onActionClick = { navigateTo(Routes.PetsRoute) }
                    )
                }

                item {
                    PetsRow(
                        pets = uiState.pets,
                        onPetClick = { petId ->
                            navigateTo(Routes.PetDetailRoute(petId))
                        }
                    )
                }

                // Servicios
                item {
                    Text(
                        "Servicios Destacados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    ServicesGrid()
                }
            }



            // FAB con menú
            AnimatedVisibility(
                visible = !fabMenuExpanded,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 110.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { fabMenuExpanded = true },
                    containerColor = extColors.mint.color,
                    contentColor = extColors.mint.onColor,
                    modifier = Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp)
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva Cita")
                }
            }

            // FAB Menu expandido
            AnimatedVisibility(
                visible = fabMenuExpanded,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FabMenu(
                    onDismiss = { fabMenuExpanded = false },
                    onItemClick = { action ->
                        fabMenuExpanded = false
                        when (action) {
                            "cita" -> navigateTo(Routes.NewAppointmentRoute())
                            "mascota" -> navigateTo(Routes.PetsRoute)
                            "emergencia" -> { /* Handle emergency */ }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 110.dp)
                )
            }
        }
    }
}

// Actualiza PetsRow para incluir onPetClick
@Composable
fun PetsRow(
    pets: List<cl.clinipets.data.model.Pet>,
    onPetClick: (String) -> Unit
) {
    val extColors = LocalExtendedColors.current
    val colors = listOf(
        extColors.pink.colorContainer,
        extColors.lavander.colorContainer,
        extColors.peach.colorContainer,
        extColors.mint.colorContainer
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        pets.take(3).forEachIndexed { index, pet ->
            Card(
                onClick = { onPetClick(pet.id) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors[index % colors.size]
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (pet.species) {
                                cl.clinipets.data.model.PetSpecies.DOG -> "🐕"
                                cl.clinipets.data.model.PetSpecies.CAT -> "🐈"
                                else -> "🐾"
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        pet.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// El resto de los componentes (HeroCard, ConnectedServiceButtons, etc.) permanecen igual...
@Composable
fun HeroCard() {
    val extColors = LocalExtendedColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            extColors.pink.colorContainer,
                            extColors.lavander.colorContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "La salud de tus",
                            style = MaterialTheme.typography.titleLarge,
                            color = extColors.pink.onColorContainer
                        )
                        Text(
                            "mascotas primero 💝",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = extColors.pink.onColorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Agenda tu próxima visita",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extColors.pink.onColorContainer.copy(alpha = 0.8f)
                        )
                    }

                    // Decoración animada
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        extColors.lavander.color.copy(alpha = 0.3f),
                                        extColors.lavander.color.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐾", style = MaterialTheme.typography.displaySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectedServiceButtons() {
    val extColors = LocalExtendedColors.current
    val options = listOf("Consulta", "Vacunas", "Urgencia")
    val unCheckedIcons = listOf(
        Icons.Outlined.MedicalServices,
        Icons.Outlined.Vaccines,
        Icons.Outlined.Emergency
    )
    val checkedIcons = listOf(
        Icons.Filled.MedicalServices,
        Icons.Filled.Vaccines,
        Icons.Filled.Emergency
    )
    val colors = listOf(
        extColors.mint.colorContainer,
        extColors.peach.colorContainer,
        extColors.pink.colorContainer
    )
    var selectedIndex by remember { mutableIntStateOf(0) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, label ->
            Surface(
                onClick = { selectedIndex = index },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shape = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes().shape
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes().shape
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes().shape
                },
                color = if (selectedIndex == index) colors[index] else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedIndex == index)
                        colors[index]
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selectedIndex == index) checkedIcons[index] else unCheckedIcons[index],
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                        tint = if (selectedIndex == index)
                            when (index) {
                                0 -> extColors.mint.onColorContainer
                                1 -> extColors.peach.onColorContainer
                                2 -> extColors.pink.onColorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun NextAppointmentCard(appointment: cl.clinipets.data.model.Appointment) {
    val extColors = LocalExtendedColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = extColors.mint.colorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Próxima cita",
                    style = MaterialTheme.typography.labelMedium,
                    color = extColors.mint.onColorContainer.copy(alpha = 0.7f)
                )
                Text(
                    appointment.petName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = extColors.mint.onColorContainer
                )
                Text(
                    appointment.serviceType.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extColors.mint.onColorContainer.copy(alpha = 0.8f)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = extColors.mint.color,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = extColors.mint.onColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PetsRow(pets: List<cl.clinipets.data.model.Pet>) {
    val extColors = LocalExtendedColors.current
    val colors = listOf(
        extColors.pink.colorContainer,
        extColors.lavander.colorContainer,
        extColors.peach.colorContainer,
        extColors.mint.colorContainer
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        pets.take(3).forEachIndexed { index, pet ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors[index % colors.size]
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (pet.species) {
                                cl.clinipets.data.model.PetSpecies.DOG -> "🐕"
                                cl.clinipets.data.model.PetSpecies.CAT -> "🐈"
                                else -> "🐾"
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        pet.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServicesGrid() {
    val extColors = LocalExtendedColors.current
    val services = listOf(
        Triple(Icons.Filled.Vaccines, "Vacunación", extColors.pink.colorContainer),
        Triple(Icons.Filled.MedicalServices, "Consulta", extColors.mint.colorContainer),
        Triple(Icons.Filled.Healing, "Cirugías", extColors.lavander.colorContainer),
        Triple(Icons.Filled.Spa, "Peluquería", extColors.peach.colorContainer),
        Triple(Icons.Filled.Home, "A Domicilio", extColors.pink.colorContainer),
        Triple(Icons.Filled.Science, "Laboratorio", extColors.mint.colorContainer)
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
                    containerColor = color,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionText)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun FabMenu(
    onDismiss: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extColors = LocalExtendedColors.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Opciones del menú
        listOf(
            Triple("emergencia", Icons.Filled.Emergency, extColors.pink),
            Triple("mascota", Icons.Filled.Pets, extColors.lavander),
            Triple("cita", Icons.Filled.CalendarMonth, extColors.mint)
        ).forEach { (action, icon, colorFamily) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = { onItemClick(action) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        when (action) {
                            "emergencia" -> "Emergencia"
                            "mascota" -> "Nueva Mascota"
                            "cita" -> "Agendar Cita"
                            else -> ""
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                SmallFloatingActionButton(
                    onClick = { onItemClick(action) },
                    containerColor = colorFamily.colorContainer,
                    contentColor = colorFamily.onColorContainer
                ) {
                    Icon(icon, contentDescription = null)
                }
            }
        }

        // Botón de cerrar
        Spacer(modifier = Modifier.height(4.dp))
        FloatingActionButton(
            onClick = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
        }
    }
}