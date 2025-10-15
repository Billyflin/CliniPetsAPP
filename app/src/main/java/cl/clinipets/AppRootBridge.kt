package cl.clinipets

import androidx.compose.runtime.Composable
import cl.clinipets.domain.auth.AuthRepository
import cl.clinipets.domain.discovery.DiscoveryRepository

@Composable
fun AppRoot(
    authRepository: AuthRepository,
    discoveryRepository: DiscoveryRepository,
    webClientId: String
) = cl.clinipets.ui.AppRoot(
    authRepository = authRepository,
    discoveryRepository = discoveryRepository,
    webClientId = webClientId
)

