package cl.clinipets.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cl.clinipets.openapi.models.DiscoveryRequest
import cl.clinipets.ui.agenda.AgendaClienteScreen
import cl.clinipets.ui.agenda.AgendaVeterinarioScreen
import cl.clinipets.ui.agenda.ReservaConfirmScreen
import cl.clinipets.ui.agenda.ReservaFormScreen
import cl.clinipets.ui.auth.LoginScreen
import cl.clinipets.ui.auth.LoginViewModel
import cl.clinipets.ui.discovery.DiscoveryScreen
import cl.clinipets.ui.home.HomeScreen
import cl.clinipets.ui.junta.JuntaComposable
import cl.clinipets.ui.mascotas.MascotaDetailScreen
import cl.clinipets.ui.mascotas.MascotaFormScreen
import cl.clinipets.ui.mascotas.MascotasScreen
import cl.clinipets.ui.profile.ProfileScreen
import cl.clinipets.ui.profile.VeterinarianScreen
import cl.clinipets.ui.veterinarios.MiCatalogoScreen
import cl.clinipets.ui.veterinarios.MiDisponibilidadScreen
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable object LoginRoute
@Serializable object HomeRoute
@Serializable object MascotasRoute
@Serializable object ProfileRoute
@Serializable object VeterinarianRoute
@Serializable object MiCatalogoRoute
@Serializable object MiDisponibilidadRoute
@Serializable object DiscoveryRoute

@Serializable object AgendaClienteRoute
@Serializable object AgendaVeterinarioRoute

@Serializable
data class MascotaDetailRoute(val id: String)

@Serializable
data class MascotaFormRoute(val id: String? = null)

@Serializable
data class ReservaFormRoute(
    val mascotaId: String,
    val procedimientoSku: String,
    val modo: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val veterinarioId: String? = null,
    val precioSugerido: Int? = null,
    val veterinarioNombre: String? = null
)

@Serializable
data class ReservaConfirmRoute(
    val procedimientoSku: String,
    val modo: String,
    val fecha: String,
    val horaInicio: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val veterinarioId: String? = null,
    val direccion: String? = null,
    val referencias: String? = null,
    val veterinarioNombre: String? = null
)

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
    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                busy = busy,
                error = uiState.error,
                onLoginClick = onLoginClick
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                displayName = uiState.displayName,
                fotoUrl = uiState.me?.fotoUrl,
                roles = uiState.roles,
                onNavigateToMascotas = { navController.navigate(MascotasRoute) },
                onNavigateToProfile = { navController.navigate(ProfileRoute) },
                onNavigateToMiCatalogo = { navController.navigate(MiCatalogoRoute) },
                onNavigateToMiDisponibilidad = { navController.navigate(MiDisponibilidadRoute) },
                onNavigateToAgenda = { navController.navigate(DiscoveryRoute) },
                onNavigateToAgendaCliente = { navController.navigate(AgendaClienteRoute) },
                onNavigateToAgendaVeterinario = { navController.navigate(AgendaVeterinarioRoute) }
            )
        }
        composable<MascotasRoute> {
            MascotasScreen(
                displayName = uiState.displayName,
                onNavigateToMascotaDetail = { id: UUID ->
                    navController.navigate(
                        MascotaDetailRoute(
                            id.toString()
                        )
                    )
                },
                onBack = { navController.popBackStack() },
                onNavigateToMascotaForm = { navController.navigate(MascotaFormRoute()) }
            )
        }
        composable<MascotaFormRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<MascotaFormRoute>()
            val mascotaId = args.id?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            MascotaFormScreen(mascotaId = mascotaId, onBack = { navController.popBackStack() })
        }
        composable<MascotaDetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<MascotaDetailRoute>()
            MascotaDetailScreen(
                mascotaId = UUID.fromString(args.id),
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(MascotaFormRoute(id.toString())) }
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                state = uiState,
                onBack = { navController.popBackStack() },
                onBecomeVeterinarian = { navController.navigate(VeterinarianRoute) },
                onRefreshProfile = onRefreshProfile,
                onEditProfessional = { navController.navigate(VeterinarianRoute) },
                onLogout = onLogout
            )
        }
        composable<VeterinarianRoute> {
            VeterinarianScreen(
                suggestedName = uiState.displayName ?: uiState.me?.nombre,
                onBack = { navController.popBackStack() },
                onCompleted = {
                    navController.popBackStack()
                    onRefreshProfile()
                }
            )
        }
        composable<MiCatalogoRoute> { MiCatalogoScreen(onBack = { navController.popBackStack() }) }
        composable<MiDisponibilidadRoute> { MiDisponibilidadScreen(onBack = { navController.popBackStack() }) }
        composable<DiscoveryRoute> {
            DiscoveryScreen(
                onBack = { navController.popBackStack() },
                onContinuarReserva = { mascotaId: UUID, sku: String, modo: DiscoveryRequest.ModoAtencion, lat: Double?, lng: Double?, veterinarioId: UUID?, precioSugerido: Int?, veterinarioNombre: String? ->
                    navController.navigate(
                        ReservaFormRoute(
                            mascotaId = mascotaId.toString(),
                            procedimientoSku = sku,
                            modo = modo.name,
                            lat = lat,
                            lng = lng,
                            veterinarioId = veterinarioId?.toString(),
                            precioSugerido = precioSugerido,
                            veterinarioNombre = veterinarioNombre
                        )
                    )
                }
            )
        }
        composable<ReservaFormRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<ReservaFormRoute>()
            ReservaFormScreen(
                mascotaId = UUID.fromString(args.mascotaId),
                procedimientoSku = args.procedimientoSku,
                modo = DiscoveryRequest.ModoAtencion.valueOf(args.modo),
                lat = args.lat,
                lng = args.lng,
                veterinarioId = args.veterinarioId?.let(UUID::fromString),
                precioSugerido = args.precioSugerido,
                veterinarioNombre = args.veterinarioNombre,
                onBack = { navController.popBackStack() },
                onReservada = { navController.popBackStack(DiscoveryRoute, inclusive = false) },
                onContinuarConfirmacion = { fecha: String, horaInicio: String, direccion: String?, referencias: String? ->
                    navController.navigate(
                        ReservaConfirmRoute(
                            procedimientoSku = args.procedimientoSku,
                            modo = args.modo,
                            fecha = fecha,
                            horaInicio = horaInicio,
                            lat = args.lat,
                            lng = args.lng,
                            veterinarioId = args.veterinarioId,
                            direccion = direccion,
                            referencias = referencias,
                            veterinarioNombre = args.veterinarioNombre
                        )
                    )
                }
            )
        }
        composable<ReservaConfirmRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<ReservaConfirmRoute>()
            ReservaConfirmScreen(
                procedimientoSku = args.procedimientoSku,
                modo = DiscoveryRequest.ModoAtencion.valueOf(args.modo),
                fecha = args.fecha,
                horaInicio = args.horaInicio,
                lat = args.lat,
                lng = args.lng,
                veterinarioId = args.veterinarioId?.let(UUID::fromString),
                veterinarioNombre = args.veterinarioNombre,
                direccion = args.direccion,
                referencias = args.referencias,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack(DiscoveryRoute, inclusive = false) }
            )
        }

        composable<AgendaClienteRoute> {
            AgendaClienteScreen(
                onBack = { navController.popBackStack() },
                onVerMapa = { reservaId -> navController.navigate(JuntaRoute(reservaId.toString())) },
                userId = uiState.me?.id
            )
        }
        composable<AgendaVeterinarioRoute> {
            AgendaVeterinarioScreen(
                onBack = { navController.popBackStack() },
                onVerMapa = { reservaId -> navController.navigate(JuntaRoute(reservaId.toString())) },
                userId = uiState.me?.id
            )
        }

        composable<JuntaRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<JuntaRoute>()
            JuntaComposable(
                reservaId = UUID.fromString(args.reservaId),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
