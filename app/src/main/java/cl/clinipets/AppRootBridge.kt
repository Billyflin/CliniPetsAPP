package cl.clinipets

import androidx.compose.runtime.Composable
import cl.clinipets.domain.agenda.AgendaRepository
import cl.clinipets.domain.auth.AuthRepository
import cl.clinipets.domain.catalogo.CatalogoRepository
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.inventario.InventarioRepository
import cl.clinipets.domain.mascotas.MascotasRepository
import cl.clinipets.domain.veterinarios.VeterinariosRepository

@Composable
fun AppRootEntry(
    authRepository: AuthRepository,
    discoveryRepository: DiscoveryRepository,
    webClientId: String,
    unauthorizedSignal: Long = 0L,
    mascotasRepository: MascotasRepository,
    agendaRepository: AgendaRepository,
    catalogoRepository: CatalogoRepository,
    forbiddenSignal: Long = 0L,
    veterinariosRepository: VeterinariosRepository? = null,
    inventarioRepository: InventarioRepository? = null,
) = cl.clinipets.ui.AppRoot(
    authRepository = authRepository,
    discoveryRepository = discoveryRepository,
    webClientId = webClientId,
    unauthorizedSignal = unauthorizedSignal,
    mascotasRepository = mascotasRepository,
    agendaRepository = agendaRepository,
    catalogoRepository = catalogoRepository,
    forbiddenTick = forbiddenSignal,
    veterinariosRepository = veterinariosRepository,
    inventarioRepository = inventarioRepository,
)
