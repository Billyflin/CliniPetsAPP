package cl.clinipets.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.clinipets.navigation.NavigationState
import cl.clinipets.navigation.Route
import cl.clinipets.ui.theme.LocalExtendedColors
import kotlinx.coroutines.delay

data class BottomNavItem(
    val route: Route,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
    val badge: Int? = null
)

@Composable
fun ClinipetsBottomBar(
    navigationState: NavigationState,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val extColors = LocalExtendedColors.current

    val items = remember {
        listOf(
            BottomNavItem(
                route = Route.Home,
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                label = "Inicio"
            ),
            BottomNavItem(
                route = Route.Appointments,
                icon = Icons.Outlined.CalendarMonth,
                selectedIcon = Icons.Filled.CalendarMonth,
                label = "Citas",
                badge = 2
            ),
            BottomNavItem(
                route = Route.Pets,
                icon = Icons.Outlined.Pets,
                selectedIcon = Icons.Filled.Pets,
                label = "Mascotas"
            ),
            BottomNavItem(
                route = Route.Profile,
                icon = Icons.Outlined.Person,
                selectedIcon = Icons.Filled.Person,
                label = "Perfil"
            )
        )
    }

    val colors = listOf(
        extColors.pink.color,
        extColors.mint.color,
        extColors.lavander.color,
        extColors.peach.color
    )

    var showFab by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        showFab = true
    }

    val fabScale by animateFloatAsState(
        targetValue = if (showFab) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 12.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = extColors.pink.color.copy(alpha = 0.1f)
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items.forEachIndexed { index, item ->
                    if (index == 2) {
                        // Space for FAB
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    val isSelected = navigationState.currentRoute == item.route::class.qualifiedName

                    NavBarItem(
                        icon = item.icon,
                        selectedIcon = item.selectedIcon,
                        title = item.label,
                        isSelected = isSelected,
                        color = colors[index],
                        badge = item.badge,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navigationState.navigateToBottomBarRoute(item.route)
                        }
                    )
                }
            }
        }

        // FAB Central
        AnimatedVisibility(
            visible = showFab,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it }
            ),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-30).dp)
        ) {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navigationState.navigateToRoute(Route.NewAppointment())
                },
                shape = CircleShape,
                containerColor = extColors.mint.color,
                contentColor = extColors.mint.onColor,
                modifier = Modifier
                    .size(56.dp)
                    .scale(fabScale)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = extColors.mint.color.copy(alpha = 0.3f)
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Nueva Cita",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    selectedIcon: ImageVector,
    title: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    badge: Int? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "scale"
    )

    val indicatorHeight by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "indicatorHeight"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) color.copy(alpha = 0.1f)
                    else Color.Transparent
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (badge != null && badge > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(badge.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSelected) selectedIcon else icon,
                    contentDescription = title,
                    tint = if (isSelected) color
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isSelected) color
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Selection indicator
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(20.dp)
                .height(indicatorHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
        )
    }
}