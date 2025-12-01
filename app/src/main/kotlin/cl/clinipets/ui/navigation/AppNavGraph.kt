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
@Serializable object MascotaFormRoute
@Serializable data class BookingRoute(val serviceId: String)
@Serializable data class PaymentRoute(val paymentUrl: String?, val price: Int)
@Serializable object ProfileRoute
@Serializable object MyReservationsRoute


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
                 onServiceClick = { serviceId ->
                     navController.navigate(BookingRoute(serviceId))
                 },
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

        composable<MascotaFormRoute> {
            cl.clinipets.ui.mascotas.MascotaFormScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable<BookingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<BookingRoute>()
            cl.clinipets.ui.agenda.BookingScreen(
                serviceId = route.serviceId,
                onBack = { navController.popBackStack() },
                onAddPet = { navController.navigate(MascotaFormRoute) },
                onSuccess = { cita -> 
                    navController.navigate(PaymentRoute(cita.paymentUrl, cita.precioFinal))
                }
            )
        }

        composable<PaymentRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PaymentRoute>()
            // Construct a partial CitaResponse for display
            // In a real app, you might pass ID and refetch, or use a shared ViewModel
            val citaStub = cl.clinipets.openapi.models.CitaResponse(
                id = java.util.UUID.randomUUID(), // Dummy, not needed for display
                fechaHoraInicio = java.time.OffsetDateTime.now(), // Dummy
                fechaHoraFin = java.time.OffsetDateTime.now(), // Dummy
                estado = cl.clinipets.openapi.models.CitaResponse.Estado.PENDIENTE_PAGO,
                precioFinal = route.price,
                servicioId = java.util.UUID.randomUUID(), // Dummy
                mascotaId = java.util.UUID.randomUUID(), // Dummy
                tutorId = java.util.UUID.randomUUID(), // Dummy
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
        }
    }
}
