package cl.clinipets.attention.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

sealed interface AttDest {
    @Serializable
    data object Graph : AttDest

    @Serializable
    data object Request : AttDest

    @Serializable
    data class Confirm(val vetId: String) : AttDest

    @Serializable
    data object Inbox : AttDest
}

fun NavGraphBuilder.attentionGraph(nav: NavController) {
    navigation<AttDest.Graph>(startDestination = AttDest.Request) {

        // PANTALLA 2: Request (mapa)
        composable<AttDest.Request> {
            Log.d("ATT_DEST", "Request")
            Text( text = "Request Screen" )

        }

        composable<AttDest.Confirm>(
            deepLinks = listOf(
                navDeepLink { uriPattern = "clinipets://confirm/{vetId}" },
                navDeepLink { uriPattern = "https://clinipets.app/confirm/{vetId}" }
            )
        ) { entry ->
            val args = entry.toRoute<AttDest.Confirm>()
            Log.d("ATT_DEST", "Confirm: ${args.vetId}")

        }

        composable<AttDest.Inbox> {


        }
    }
}