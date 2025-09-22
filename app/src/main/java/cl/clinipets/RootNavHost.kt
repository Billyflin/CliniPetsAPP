package cl.clinipets

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import cl.clinipets.auth.AuthDest
import cl.clinipets.auth.authGraph
import kotlinx.serialization.Serializable



// Onboarding
sealed interface OnbDest {
    @Serializable
    data object Graph : OnbDest

    @Serializable
    data object RolePicker : OnbDest

    @Serializable
    data object BasicInfo : OnbDest
}

// Verificación Vet
sealed interface VetVerDest {
    @Serializable
    data object Graph : VetVerDest

    @Serializable
    data object UploadDocs : VetVerDest

    @Serializable
    data object Status : VetVerDest
}

// Atención (Tutor)
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

// Citas
sealed interface ApptDest {
    @Serializable
    data object Graph : ApptDest

    @Serializable
    data object Calendar : ApptDest

    @Serializable
    data class Details(val appointmentId: String) : ApptDest
}

// Consulta
sealed interface ConsDest {
    @Serializable
    data object Graph : ConsDest

    @Serializable
    data class Form(val appointmentId: String) : ConsDest

    @Serializable
    data class Checkout(val consultationId: String) : ConsDest

    @Serializable
    data class Derivation(val consultationId: String) : ConsDest
}

// Historial
sealed interface HistDest {
    @Serializable
    data object Graph : HistDest

    @Serializable
    data object PetList : HistDest

    @Serializable
    data class PetDetails(val petId: String) : HistDest

    @Serializable
    data class PdfPreview(val petId: String? = null) : HistDest
}

// Chat
sealed interface ChatDest {
    @Serializable
    data object Graph : ChatDest

    @Serializable
    data object ThreadList : ChatDest

    @Serializable
    data class Thread(val conversationId: String) : ChatDest
}

// Notificaciones
sealed interface NotifDest {
    @Serializable
    data object Graph : NotifDest

    @Serializable
    data object Center : NotifDest
}

// Reportes
sealed interface RepDest {
    @Serializable
    data object Graph : RepDest

    @Serializable
    data object Dashboard : RepDest
}

fun NavGraphBuilder.attentionGraph(nav: NavController) {
    navigation<AttDest.Graph>(startDestination = AttDest.Request) {

        // PANTALLA 2: Request (mapa)
        composable<AttDest.Request> {
            // si ya tienes RequestViewModel/RequestMapScreen, colócalos aquí:
            // val vm: RequestViewModel = hiltViewModel()
            SimpleScreen(
                title = "Mapa (Request)",
                primary = "Elegir Vet",
                onPrimary = { nav.navigate(AttDest.Confirm(vetId = "vet-123")) })
        }

        composable<AttDest.Confirm>(
            deepLinks = listOf(
                navDeepLink { uriPattern = "clinipets://confirm/{vetId}" },
                navDeepLink { uriPattern = "https://clinipets.app/confirm/{vetId}" }
            )
        ) { entry ->
            val args = entry.toRoute<AttDest.Confirm>()
            Log.d("ATT_DEST", "Confirm: ${args.vetId}")
            SimpleScreen(
                title = "Confirmar: ${args.vetId}",
                primary = "Crear cita",
                onPrimary = { nav.popBackStack() },
                showBack = true
            )
        }

        composable<AttDest.Inbox> {
            SimpleScreen(
                "Inbox Vet", "Volver", onPrimary = { nav.popBackStack() }, showBack = true
            )
        }
    }
}

/* ───────────── Root host súper compacto ───────────── */

@Composable
fun RootNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = AuthDest.Graph) {
        authGraph(nav)
        attentionGraph(nav)
        // Añade cuando tengas pantallas:
        // onboardingGraph(nav)
        // vetVerificationGraph(nav)
        // appointmentsGraph(nav)
        // consultationGraph(nav)
        // historyGraph(nav)
        // chatGraph(nav)
        // notificationsGraph(nav)
        // reportsGraph(nav)
    }
}

/* ───────────── UI placeholder mínima (para compilar ya) ───────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleScreen(
    title: String, primary: String, onPrimary: () -> Unit, showBack: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title) }, navigationIcon = {
                if (showBack) {
                    @Composable {
                        IconButton(onClick = onPrimary) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack, contentDescription = null
                            )
                        }
                    }
                } else null
            })
        }) { pads ->
        Button(
            onClick = onPrimary, modifier = Modifier
                .padding(pads)
                .padding(24.dp)
        ) { Text(primary) }
    }
}