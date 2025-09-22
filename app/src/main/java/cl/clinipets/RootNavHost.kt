package cl.clinipets

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cl.clinipets.attention.navigation.attentionGraph
import cl.clinipets.auth.navigation.AuthDest
import cl.clinipets.auth.navigation.authGraph

@Composable
fun RootNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = AuthDest.Graph) {
        authGraph(nav)
        attentionGraph(nav)
//         onboardingGraph(nav)
//         vetVerificationGraph(nav)
//         appointmentsGraph(nav)
//         consultationGraph(nav)
//         historyGraph(nav)
//         chatGraph(nav)
//         notificationsGraph(nav)
//         reportsGraph(nav)
    }
}


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