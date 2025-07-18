// ui/components/FloatingBottomNavBar.kt
package cl.clinipets.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.clinipets.ui.theme.LocalExtendedColors

@Composable
fun FloatingBottomNavBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val tintMap = listOf(ext.pink.color, ext.mint.color, ext.lavander.color, ext.peach.color)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(36.dp),
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        ext.pink.colorContainer.copy(alpha = 0.3f),
                        ext.lavander.colorContainer.copy(alpha = 0.3f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    FloatingNavItem(
                        item = item,
                        selected = selectedIndex == index,
                        onClick = { onItemSelected(index) },
                        tint = tintMap.getOrElse(index) { MaterialTheme.colorScheme.primary }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val size by animateDpAsState(
        targetValue = if (selected) 64.dp else 56.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 8.dp else 0.dp,
        animationSpec = spring()
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .let {
                if (selected) {
                    it
                        .shadow(elevation, CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    tint.copy(alpha = 0.2f),
                                    tint.copy(alpha = 0.05f)
                                )
                            )
                        )
                } else it
            }
            .semantics { contentDescription = item.label },
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            BadgedBox(
                badge = {
                    if (item.badgeCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) { Text(item.badgeCount.toString()) }
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = null,
                    tint = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(if (selected) 28.dp else 24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = selected,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(tint)
            )
        }
    }
}

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)
