package cl.clinipets.appointments.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import cl.clinipets.appointments.ui.CalendarScreen
import cl.clinipets.appointments.ui.DetailsScreen
import kotlinx.serialization.Serializable

// Citas
sealed interface ApptDest {
    @Serializable
    data object Graph : ApptDest

    @Serializable
    data object Calendar : ApptDest

    @Serializable
    data class Details(val appointmentId: String) : ApptDest
}

fun NavGraphBuilder.appointmentsGraph(nav: NavController) {
    navigation<ApptDest.Graph>(startDestination = ApptDest.Calendar) {

        // Pantalla 1: Calendario de citas

        composable<ApptDest.Calendar> {
            // Aquí va tu pantalla de calendario
            CalendarScreen()
        }
        // Pantalla 2: Detalles de la cita
        composable<ApptDest.Details> { entry ->
            val args = entry.toRoute<ApptDest.Details>()
            Text(text = "Detalles de la Cita: ${args.appointmentId}")
            DetailsScreen(
                appointmentId = args.appointmentId,
            )
        }
    }
}