package cl.clinipets.domain.auth

import cl.clinipets.data.dto.MeResponse

interface AuthRepository {
    fun isLoggedIn(): Boolean
    suspend fun loginWithGoogle(idToken: String): String
    suspend fun me(): MeResponse
    suspend fun logout()

    // Nuevos: roles y rol activo persistidos
    fun getRoles(): List<String>
    fun getActiveRole(): String?
    fun setActiveRole(role: String?)
}
