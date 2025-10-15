package cl.clinipets.data.repositories

import cl.clinipets.core.security.TokenStorage
import cl.clinipets.data.api.AuthApi
import cl.clinipets.data.dto.GoogleLoginRequest
import cl.clinipets.data.dto.MeResponse
import cl.clinipets.domain.auth.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage
) : AuthRepository {
    override fun isLoggedIn(): Boolean = !tokenStorage.getJwt().isNullOrBlank()

    override suspend fun loginWithGoogle(idToken: String): String {
        val res = api.login(GoogleLoginRequest(idToken))
        tokenStorage.setJwt(res.token)
        return res.token
    }

    override suspend fun me(): MeResponse {
        val me = api.me()
        // Persistir roles y rol activo por defecto si no existe
        tokenStorage.setRoles(me.roles)
        if (tokenStorage.getActiveRole().isNullOrBlank() && me.roles.isNotEmpty()) {
            tokenStorage.setActiveRole(me.roles.first())
        }
        return me
    }

    override suspend fun logout() {
        tokenStorage.setJwt(null)
        tokenStorage.setRoles(emptyList())
        tokenStorage.setActiveRole(null)
    }

    override fun getRoles(): List<String> = tokenStorage.getRoles()

    override fun getActiveRole(): String? = tokenStorage.getActiveRole()

    override fun setActiveRole(role: String?) {
        tokenStorage.setActiveRole(role)
    }
}
