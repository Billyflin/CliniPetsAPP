package cl.clinipets

import androidx.compose.runtime.Composable
import cl.clinipets.domain.agenda.AgendaRepository
import cl.clinipets.domain.auth.AuthRepository
import cl.clinipets.domain.catalogo.CatalogoRepository
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.mascotas.MascotasRepository

@Composable
fun AppRoot(
    authRepository: AuthRepository,
    discoveryRepository: DiscoveryRepository,
    webClientId: String,
    unauthorizedSignal: Long = 0L,
    mascotasRepository: MascotasRepository,
    agendaRepository: AgendaRepository,
    catalogoRepository: CatalogoRepository,
) = cl.clinipets.ui.AppRoot(
    authRepository = authRepository,
    discoveryRepository = discoveryRepository,
    webClientId = webClientId,
    unauthorizedSignal = unauthorizedSignal,
    mascotasRepository = mascotasRepository,
    agendaRepository = agendaRepository,
    catalogoRepository = catalogoRepository
)
