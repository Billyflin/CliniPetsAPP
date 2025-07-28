// ui/screens/HomeScreen.kt
package cl.clinipets.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.ui.components.textLogo
import cl.clinipets.ui.theme.ColorFamily
import cl.clinipets.ui.theme.ExtendedColorScheme
import cl.clinipets.ui.theme.LocalExtendedColors
import cl.clinipets.ui.viewmodels.AppointmentsViewModel
import cl.clinipets.ui.viewmodels.PetsViewModel
import cl.clinipets.ui.viewmodels.UserViewModel
import cl.clinipets.ui.viewmodels.VetViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// package cl.clinipets.ui.screens.home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPets: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToVetDashboard: () -> Unit,
    onNavigateToProfile: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    vetViewModel: VetViewModel = hiltViewModel(),
    petsViewModel: PetsViewModel = hiltViewModel(),
    appointmentsViewModel: AppointmentsViewModel = hiltViewModel()
) {
    val userState by userViewModel.userState.collectAsStateWithLifecycle()
    val petsState by petsViewModel.petsState.collectAsStateWithLifecycle()
    val appointmentsState by appointmentsViewModel.appointmentsState.collectAsStateWithLifecycle()
    val extColors = LocalExtendedColors.current

    LaunchedEffect(Unit) {
        userViewModel.refreshUser()
        petsViewModel.loadPets()
        appointmentsViewModel.loadAppointments()
    }

    val scrollState = rememberScrollState()
    val headerHeight = 280.dp
    val toolbarHeight = 64.dp
    val density = LocalDensity.current
    val scrollProgress = remember {
        derivedStateOf {
            val scroll = scrollState.value.toFloat()
            val headerHeightPx = with(density) { headerHeight.toPx() }
            (scroll / headerHeightPx).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface, topBar = {
            Icon(
                imageVector = textLogo,
                contentDescription = "Clinipets Logo",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(width = 160.dp, height = 64.dp),
                tint = extColors.mint.color
            )
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = scrollProgress.value
                ), shadowElevation = if (scrollProgress.value > 0.5f) 4.dp else 0.dp
            ) {
                TopAppBar(
                    title = {
                        AnimatedVisibility(
                            visible = scrollProgress.value > 0.7f,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            Text(
                                "Hola, ${userState.userName.split(" ").first()} 👋",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }, actions = {
                        // Notificaciones
                        IconButton(onClick = {

                            // Acción de notificaciones


                        }) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = extColors.pink.color,
                                        contentColor = extColors.pink.onColor
                                    ) {
                                        Text("2")
                                    }
                                }) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notificaciones"
                                )
                            }
                        }
                        // Perfil
                        Surface(
                            shape = CircleShape,
                            color = extColors.lavander.colorContainer,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onNavigateToProfile() }) {
                            Box(contentAlignment = Alignment.Center) {
                                if (userState.photoUrl.isNotEmpty()) {
                                    // AsyncImage aquí
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Perfil",
                                        tint = extColors.lavander.onColorContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        userState.userName.firstOrNull()?.toString() ?: "?",
                                        fontWeight = FontWeight.Bold,
                                        color = extColors.lavander.onColorContainer
                                    )
                                }
                            }
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ), modifier = Modifier.height(toolbarHeight)
                )
            }
        }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .graphicsLayer {
                        translationY = scrollState.value * 0.5f
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                extColors.lavander.colorContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header animado
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp, bottom = 8.dp)
                            .graphicsLayer {
                                alpha = 1f - scrollProgress.value
                                translationY = scrollState.value * 0.3f
                            }) {
                        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                            in 0..11 -> "Buenos días"
                            in 12..18 -> "Buenas tardes"
                            else -> "Buenas noches"
                        }

                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${userState.userName.split(" ").first()} 👋",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = "¿Cómo están tus mascotas hoy?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cards de resumen mejoradas
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickStatCard(
                            value = userState.petsCount.toString(),
                            label = "Mascotas",
                            icon = "🐾",
                            color = extColors.pink,
                            onClick = onNavigateToPets,
                            modifier = Modifier.weight(1f)
                        )

                        QuickStatCard(
                            value = appointmentsState.upcomingAppointments.size.toString(),
                            label = "Citas próximas",
                            icon = "📅",
                            color = extColors.mint,
                            onClick = onNavigateToAppointments,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Próxima cita destacada
                appointmentsState.upcomingAppointments.firstOrNull()?.let { nextAppointment ->
                    item {
                        NextAppointmentCard(
                            appointment = nextAppointment,
                            pet = petsState.pets.find { it.id == nextAppointment.petId },
                            onClick = onNavigateToAppointments,
                            extColors = extColors,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Mascotas con scroll horizontal
                if (petsState.pets.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Tus mascotas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onNavigateToPets) {
                                    Text("Ver todas")
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            val shownPets = petsState.pets.take(5)
                            val remainingCount = petsState.pets.size - shownPets.size

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(shownPets) { _, pet ->
                                    PetQuickCard(
                                        pet = pet,
                                        onClick = { onNavigateToPets() },
                                        extColors = extColors
                                    )
                                }

                                if (remainingCount > 0) {
                                    item {
                                        MorePetsCard(
                                            count = remainingCount,
                                            onClick = onNavigateToPets,
                                            color = extColors.lavander
                                        )
                                    }
                                }
                            }


                        }
                    }
                }

                // Acciones rápidas mejoradas
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Acceso rápido",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        QuickActionCard(
                            icon = Icons.Default.CalendarMonth,
                            title = "Agendar Cita",
                            subtitle = "Programa una visita al veterinario",
                            color = extColors.peach,
                            onClick = onNavigateToAppointments
                        )

                        if (userState.isVet) {
                            QuickActionCard(
                                icon = Icons.Default.MedicalServices,
                                title = "Panel Veterinario",
                                subtitle = "Gestiona tus consultas del día",
                                color = extColors.lavander,
                                onClick = onNavigateToVetDashboard,
                                badge = "PRO"
                            )
                        }
                    }
                }

                // Tips aleatorios
                item {
                    TipCard(
                        tips = listOf(
                            "💉 Recuerda mantener al día las vacunas de tus mascotas",
                            "🦷 La salud dental es importante, programa limpiezas regulares",
                            "🏃 El ejercicio diario mantiene a tu mascota feliz y saludable",
                            "💊 Nunca olvides la desparasitación mensual",
                            "🥗 Una dieta balanceada es clave para su bienestar"
                        ), extColors = extColors, modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // Espacio al final
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickStatCard(
    value: String,
    label: String,
    icon: String,
    color: ColorFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.colorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = color.color.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = icon, style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color.onColorContainer
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.onColorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextAppointmentCard(
    appointment: Appointment,
    pet: Pet?,
    onClick: () -> Unit,
    extColors: ExtendedColorScheme,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = extColors.mint.colorContainer
        ),
        border = BorderStroke(
            width = 2.dp, color = extColors.mint.color.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp), color = extColors.mint.color
                        ) {
                            Text(
                                "Próxima cita",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = extColors.mint.onColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        pet?.name ?: "Mascota",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = extColors.mint.onColorContainer
                    )
                }

                Text(
                    when (pet?.species) {
                        PetSpecies.DOG -> "🐕"
                        PetSpecies.CAT -> "🐈"
                        else -> "🐾"
                    }, style = MaterialTheme.typography.headlineLarge
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = extColors.mint.onColorContainer.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = extColors.mint.onColorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Fecha",
                                style = MaterialTheme.typography.labelSmall,
                                color = extColors.mint.onColorContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                formatDateShort(appointment.date),
                                fontWeight = FontWeight.Medium,
                                color = extColors.mint.onColorContainer
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = extColors.mint.onColorContainer.copy(alpha = 0.2f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = extColors.mint.onColorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Hora",
                                style = MaterialTheme.typography.labelSmall,
                                color = extColors.mint.onColorContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                appointment.time,
                                fontWeight = FontWeight.Medium,
                                color = extColors.mint.onColorContainer
                            )
                        }
                    }
                }
            }

            if (appointment.reason.isNotBlank()) {
                Text(
                    appointment.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extColors.mint.onColorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetQuickCard(
    pet: Pet, onClick: () -> Unit, extColors: ExtendedColorScheme
) {
    val petColor = when (pet.species) {
        PetSpecies.DOG -> extColors.peach
        PetSpecies.CAT -> extColors.lavander
        else -> extColors.mint
    }

    Card(
        onClick = onClick,
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = petColor.colorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (pet.species) {
                    PetSpecies.DOG -> "🐕"
                    PetSpecies.CAT -> "🐈"
                    else -> "🐾"
                }, style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                pet.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = petColor.onColorContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorePetsCard(
    count: Int, onClick: () -> Unit, color: ColorFamily
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 2.dp, color = color.color.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = color.colorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "+$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color.onColorContainer
            )
            Text(
                "más",
                style = MaterialTheme.typography.bodySmall,
                color = color.onColorContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: ColorFamily,
    onClick: () -> Unit,
    badge: String? = null
) {
    Card(
        onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(
            containerColor = color.colorContainer
        ), modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape, color = color.color, modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = color.onColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            title, fontWeight = FontWeight.Bold, color = color.onColorContainer
                        )
                        badge?.let {
                            Surface(
                                shape = RoundedCornerShape(8.dp), color = color.color
                            ) {
                                Text(
                                    it,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color.onColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = color.onColorContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = color.onColorContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TipCard(
    tips: List<String>, extColors: ExtendedColorScheme, modifier: Modifier = Modifier
) {
    var currentTipIndex by remember { mutableIntStateOf((0..tips.lastIndex).random()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Tip del día",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        currentTipIndex = (currentTipIndex + 1) % tips.size
                    }, modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Siguiente tip",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = currentTipIndex, transitionSpec = {
                    fadeIn() + slideInHorizontally() with fadeOut() + slideOutHorizontally()
                }, label = "tipAnimation"
            ) { index ->
                Text(
                    tips[index],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Función auxiliar
private fun formatDateShort(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        val today = Calendar.getInstance()
        val targetDate = Calendar.getInstance().apply { time = date!! }

        when {
            targetDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) && targetDate.get(Calendar.DAY_OF_YEAR) == today.get(
                Calendar.DAY_OF_YEAR
            ) -> "Hoy"

            targetDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) && targetDate.get(Calendar.DAY_OF_YEAR) == today.get(
                Calendar.DAY_OF_YEAR
            ) + 1 -> "Mañana"

            else -> SimpleDateFormat("d MMM", Locale("es")).format(date!!)
        }
    } catch (e: Exception) {
        dateString
    }
}

@Composable
@Preview
private fun HomeScreenPreview() {
    HomeScreen(
        onNavigateToPets = {},
        onNavigateToAppointments = {},
        onNavigateToVetDashboard = {},
        onNavigateToProfile = {})
}