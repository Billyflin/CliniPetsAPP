package cl.clinipets.history.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import cl.clinipets.history.ui.PetDetailsScreen
import cl.clinipets.history.ui.PetListScreen
import kotlinx.serialization.Serializable

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

fun NavGraphBuilder.historyGraph(nav: NavController) {
    navigation<HistDest.Graph>(startDestination = HistDest.PetList) {

        // Pantalla 1: Lista de mascotas
        composable<HistDest.PetList> {
            PetListScreen(
                onPetSelected = { petId ->
                    nav.navigate(HistDest.PetDetails(petId))
                })
        }
        // Pantalla 2: Detalles de la mascota
        composable<HistDest.PetDetails> { entry ->
            val args = entry.toRoute<HistDest.PetDetails>()
            PetDetailsScreen(
                petId = args.petId,
            )

        }
        // Pantalla 3: Vista previa del PDF
        composable<HistDest.PdfPreview> { entry ->

            val args = entry.toRoute<HistDest.PdfPreview>()
            Text(text = "PDF Preview for Pet: ${args.petId}")
        }
    }
}
