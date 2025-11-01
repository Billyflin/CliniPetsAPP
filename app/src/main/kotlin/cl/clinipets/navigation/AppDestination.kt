package cl.clinipets.navigation

import java.util.UUID

sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object MisMascotas : AppDestination("mis_mascotas")
    data object Descubrir : AppDestination("descubrir")
    data object Perfil : AppDestination("perfil")
    data object MascotaCrear : AppDestination("mis_mascotas/crear")
    data object MascotaDetalle : AppDestination("mis_mascotas/detalle/{mascotaId}") {
        const val ARG_ID = "mascotaId"
        fun createRoute(id: UUID): String = "mis_mascotas/detalle/$id"
    }
    data object MascotaEditar : AppDestination("mis_mascotas/editar/{mascotaId}") {
        const val ARG_ID = "mascotaId"
        fun createRoute(id: UUID): String = "mis_mascotas/editar/$id"
    }
    data object VeterinarioOnboarding : AppDestination("veterinario/onboarding")
    data object VeterinarioPerfil : AppDestination("veterinario/perfil")
    data object VeterinarioAgenda : AppDestination("veterinario/agenda")
}
