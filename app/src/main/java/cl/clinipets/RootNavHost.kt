package cl.clinipets

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cl.clinipets.attention.navigation.attentionGraph
import cl.clinipets.auth.navigation.AuthDest
import cl.clinipets.auth.navigation.authGraph
import cl.clinipets.chat.navigation.chatGraph
import cl.clinipets.history.navigation.historyGraph
import cl.clinipets.home.navigation.homeGraph

@Composable
fun RootNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = AuthDest.Graph) {
        authGraph(nav)
        homeGraph(nav)
        attentionGraph(nav)
        historyGraph(nav)
//         onboardingGraph(nav)
//         vetVerificationGraph(nav)
//         appointmentsGraph(nav)
//         consultationGraph(nav)
         chatGraph(nav)
//         notificationsGraph(nav)
//         reportsGraph(nav)
    }
}

