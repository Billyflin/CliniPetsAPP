package cl.clinipets.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import cl.clinipets.openapi.models.ProfileResponse
import cl.clinipets.ui.auth.LoginScreen
import cl.clinipets.ui.auth.LoginViewModel
import cl.clinipets.ui.agenda.PaymentResultScreen
import cl.clinipets.ui.agenda.PaymentScreen
import cl.clinipets.ui.staff.StaffAgendaScreen
import kotlinx.serialization.Serializable

@Serializable object LoginRoute
@Serializable object HomeRoute
@Serializable object StaffAgendaRoute
@Serializable data class StaffCitaDetailRoute(val citaId: String)
@Serializable data class StaffAtencionRoute(val citaId: String, val mascotaId: String)
@Serializable data class MascotaFormRoute(val petId: String? = null)
@Serializable data class BookingRoute(val petId: String? = null)
@Serializable data class PaymentRoute(val paymentUrl: String?, val price: Int)
@Serializable data class PaymentResultRoute(val status: String? = null)
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

    // Determine the start destination based on role
    val startDestination: Any = if (isLoggedIn) {
        val role = uiState.me?.role
        if (role == ProfileResponse.Role.STAFF || role == ProfileResponse.Role.ADMIN) {
            StaffAgendaRoute
        } else {
            HomeRoute
        }
    } else {
        LoginRoute
    }
    
    // Handle navigation upon successful login
    LaunchedEffect(uiState.ok, uiState.me) {
        if (uiState.ok && uiState.me != null) {
            val role = uiState.me.role
            val targetRoute = if (role == ProfileResponse.Role.STAFF || role == ProfileResponse.Role.ADMIN) {
                StaffAgendaRoute
            } else {
                HomeRoute
            }
            // Check if we are already on the target route or need to navigate
            // For simplicity in this flow, we navigate and pop Login
            navController.navigate(targetRoute) {
                popUpTo(LoginRoute) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<LoginRoute> {
            LoginScreen(
                busy = busy,
                error = uiState.error,
                onLoginClick = onLoginClick
            )
        }
        
        composable<StaffAgendaRoute> {
            StaffAgendaScreen(
                onLogout = onLogout,
                onCitaClick = { citaId ->
                    navController.navigate(StaffCitaDetailRoute(citaId))
                }
            )
        }

        composable<StaffCitaDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<StaffCitaDetailRoute>()
            cl.clinipets.ui.staff.StaffCitaDetailScreen(
                citaId = route.citaId,
                onBack = { navController.popBackStack() },
                onPetClick = { petId ->
                    navController.navigate(PetDetailRoute(petId))
                },
                onStartAtencion = { citaId, mascotaId ->
                    navController.navigate(StaffAtencionRoute(citaId, mascotaId))
                }
            )
        }

        composable<StaffAtencionRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<StaffAtencionRoute>()
            cl.clinipets.ui.staff.StaffAtencionScreen(
                citaId = route.citaId,
                mascotaId = route.mascotaId,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(StaffAgendaRoute) {
                        popUpTo(StaffAgendaRoute) { inclusive = true }
                    }
                }
            )
        }
        
        composable<HomeRoute> {
            cl.clinipets.ui.home.HomeScreen(
                 onServiceClick = { _ ->
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
                onBack = { navController.popBackStack() },
                onPay = { url ->
                    val context = navController.context
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                },
                onCancel = { }
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
            PaymentScreen(
                cita = cl.clinipets.openapi.models.CitaResponse(
                    id = java.util.UUID.randomUUID(),
                    fechaHoraInicio = java.time.OffsetDateTime.now(),
                    fechaHoraFin = java.time.OffsetDateTime.now(),
                    estado = cl.clinipets.openapi.models.CitaResponse.Estado.PENDIENTE_PAGO,
                    precioFinal = route.price,
                    detalles = emptyList(),
                    tutorId = java.util.UUID.randomUUID(),
                    origen = cl.clinipets.openapi.models.CitaResponse.Origen.APP,
                    paymentUrl = route.paymentUrl,
                    montoAbono = 0,
                    saldoPendiente = route.price,
                    tipoAtencion = cl.clinipets.openapi.models.CitaResponse.TipoAtencion.CLINICA
                ),
                onHomeClick = {
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<PaymentResultRoute>(
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "clinipets://payment-result?status={status}"
                }
            )
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<PaymentResultRoute>()
            PaymentResultScreen(
                status = route.status,
                onHomeClick = {
                    navController.navigate(HomeRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}