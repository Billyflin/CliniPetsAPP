package cl.clinipets.navigation

sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object MisMascotas : AppDestination("mis_mascotas")
    data object Descubrir : AppDestination("descubrir")
    data object Perfil : AppDestination("perfil")
}
