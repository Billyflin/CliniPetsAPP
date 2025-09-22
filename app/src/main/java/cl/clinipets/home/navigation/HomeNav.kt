package cl.clinipets.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import cl.clinipets.attention.navigation.AttDest
import cl.clinipets.auth.navigation.AuthDest
import cl.clinipets.chat.navigation.ChatDest
import cl.clinipets.history.navigation.HistDest
import cl.clinipets.home.ui.HomeScreen
import kotlinx.serialization.Serializable

sealed interface HomeDest {
    @Serializable data object Graph : HomeDest
    @Serializable data object Dashboard : HomeDest
}

fun NavGraphBuilder.homeGraph(nav: NavController) {
    navigation<HomeDest.Graph>(startDestination = HomeDest.Dashboard) {
        composable<HomeDest.Dashboard> {
            HomeScreen(
                onRequestAttention = { nav.navigate(AttDest.Graph) },
                onOpenHistory = { nav.navigate(HistDest.Graph) },
                onOpenChat = { nav.navigate(ChatDest.Graph) },
                onOpenAccount = { nav.navigate(AuthDest.Account) }
            )
        }
    }
}
