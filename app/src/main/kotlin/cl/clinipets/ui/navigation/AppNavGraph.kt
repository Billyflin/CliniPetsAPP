package cl.clinipets.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cl.clinipets.ui.auth.LoginScreen
import cl.clinipets.ui.auth.LoginViewModel
import kotlinx.serialization.Serializable

@Serializable object LoginRoute
@Serializable object HomeRoute
@Serializable data class MascotaFormRoute(val petId: String? = null)
@Serializable data class BookingRoute(val petId: String? = null)
@Serializable data class PaymentRoute(val paymentUrl: String?, val price: Int)
@Serializable object ProfileRoute
@Serializable object MyReservationsRoute
@Serializable object MyPetsRoute
@Serializable data class PetDetailRoute(val petId: String)


@Serializable
data class JuntaRoute(val reservaId: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalStdlibApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    uiState: LoginViewModel.UiState,
    busy: Boolean,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit,
    onRefreshProfile: () -> Unit
) {
    val isLoggedIn = uiState.me != null

    NavHost(navController = navController, startDestination = if (isLoggedIn) HomeRoute else LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                busy = busy,
                error = uiState.error,
                onLoginClick = onLoginClick
            )
        }
        
        composable<HomeRoute> {
            cl.clinipets.ui.home.HomeScreen(
                 onLogout = onLogout, // Kept for safety, but HomeScreen might hide it if Profile is used
                 onServiceClick = { _ ->
                     // Service click now just opens booking, service selection is done inside
                     navController.navigate(BookingRoute())
                 },
                onMyPetsClick = { navController.navigate(MyPetsRoute) },
                onProfileClick = {
                    navController.navigate(ProfileRoute)
                },
                onMyReservationsClick = {
                    navController.navigate(MyReservationsRoute)
                 }
             )
        }

        composable<ProfileRoute> {
            cl.clinipets.ui.profile.ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    onLogout()
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable<MyReservationsRoute> {
            cl.clinipets.ui.agenda.MyReservationsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<MyPetsRoute> {
            cl.clinipets.ui.mascotas.MyPetsScreen(
                onBack = { navController.popBackStack() },
                onAddPet = { navController.navigate(MascotaFormRoute()) },
                onPetClick = { pet ->
                    navController.navigate(PetDetailRoute(pet.id.toString()))
                },
                onEditPet = { pet ->
                    navController.navigate(MascotaFormRoute(pet.id.toString()))
                }
            )
        }

        composable<PetDetailRoute> {
            cl.clinipets.ui.mascotas.PetDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(MascotaFormRoute(it)) },
                onBookAppointment = { petId ->
                    navController.navigate(BookingRoute(petId = petId))
                }
            )
        }

        composable<MascotaFormRoute> {
            val route = it.toRoute<MascotaFormRoute>()
            cl.clinipets.ui.mascotas.MascotaFormScreen(
                petId = route.petId,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable<BookingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<BookingRoute>()
            cl.clinipets.ui.agenda.BookingScreen(
                preselectedPetId = route.petId,
                onBack = { navController.popBackStack() },
                onAddPet = { navController.navigate(MascotaFormRoute()) },
                onSuccess = { cita -> 
                    navController.navigate(PaymentRoute(cita.paymentUrl, cita.precioFinal))
                }
            )
        }

        composable<PaymentRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PaymentRoute>()
            // TEMPORARY PLACEHOLDER due to CitaResponse constructor issues
            androidx.compose.material3.Text("Payment Screen - Price: ${route.price}")
            /*
            val citaStub = cl.clinipets.openapi.models.CitaResponse(
                id = java.util.UUID.randomUUID(),
                fechaHoraInicio = java.time.OffsetDateTime.now(),
                fechaHoraFin = java.time.OffsetDateTime.now(),
                estado = cl.clinipets.openapi.models.CitaResponse.Estado.PENDIENTE_PAGO,
                precioFinal = route.price,
                detalles = emptyList(),
                tutorId = java.util.UUID.randomUUID(),
                origen = cl.clinipets.openapi.models.CitaResponse.Origen.APP,
                paymentUrl = route.paymentUrl
            )
            
            cl.clinipets.ui.agenda.PaymentScreen(
                cita = citaStub,
                onHomeClick = { 
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
            */
        }
    }
}
