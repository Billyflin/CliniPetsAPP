package cl.clinipets.chat.navigation

import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import cl.clinipets.chat.ui.ThreadListScreen
import cl.clinipets.chat.ui.ThreadScreen
import kotlinx.serialization.Serializable

sealed interface ChatDest {
    @Serializable
    data object Graph : ChatDest

    @Serializable
    data object ThreadList : ChatDest

    @Serializable
    data class Thread(val conversationId: String) : ChatDest
}


fun NavGraphBuilder.chatGraph(nav: NavController) {
    navigation<ChatDest.Graph>(startDestination = ChatDest.ThreadList) {

        composable<ChatDest.ThreadList> {
            ThreadListScreen(
                onThreadSelected = { conversationId ->
                    Log.d("ThreadListScreen", "Selected conversationId: $conversationId")
                    nav.navigate(ChatDest.Thread(conversationId))
                }

            )
        }
        composable<ChatDest.Thread> { backStackEntry ->
            val conversationId =
                backStackEntry.arguments?.getString("conversationId") ?: return@composable

            Log.d("ThreadScreen", "conversationId: $conversationId")
            ThreadScreen(
                conversationId = conversationId,
                onBack = {
                    nav.popBackStack()
                    true
                }
            )
        }

    }
}
