// navigation/AppNavigation.kt
package cl.clinipets.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import cl.clinipets.ui.screens.appointments.AppointmentsScreen
import cl.clinipets.ui.screens.home.HomeScreen

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Routes.HomeRoute)
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    // Función para navegar
    val navigateTo: (Routes) -> Unit = { route ->
        when (route) {
            Routes.HomeRoute -> {
                selectedNavIndex = 0
                backStack.add(route)
            }
            Routes.AppointmentsRoute -> {
                selectedNavIndex = 1
                backStack.add(route)
            }
            Routes.PetsRoute -> {
                selectedNavIndex = 2
                backStack.add(route)
            }
            Routes.ProfileRoute -> {
                selectedNavIndex = 3
                backStack.add(route)
            }
            else -> {
                backStack.add(route)
            }
        }
    }

    NavDisplay(
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            // ---------- HOME ----------
            entry<Routes.HomeRoute> {
                HomeScreen(
                    navigateTo = navigateTo,
                    selectedNavIndex = selectedNavIndex,
                    onNavIndexChanged = { selectedNavIndex = it }
                )
            }

            // ---------- APPOINTMENTS ----------
            entry<Routes.AppointmentsRoute> {
                AppointmentsScreen(
                    navigateTo = navigateTo,
                    selectedNavIndex = selectedNavIndex,
                    onNavIndexChanged = { selectedNavIndex = it }
                )
            }

            // ---------- PETS ----------
            entry<Routes.PetsRoute> {

            }

            // ---------- PROFILE ----------
            entry<Routes.ProfileRoute> {

            }

            // ---------- PET DETAIL ----------
            entry<Routes.PetDetailRoute> { (petId: String) ->
                // PetDetailScreen(petId = petId, navigateTo = navigateTo)
            }

            // ---------- NEW APPOINTMENT ----------
            entry<Routes.NewAppointmentRoute> { (petId: String?) ->
                // NewAppointmentScreen(petId = petId, navigateTo = navigateTo)
            }
        },
    )
}