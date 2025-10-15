package cl.clinipets.data.repositories

import cl.clinipets.core.security.TokenStorage
import cl.clinipets.data.api.AuthApi
import cl.clinipets.data.dto.GoogleLoginRequest
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
    override suspend fun me(): String = api.me()
    override suspend fun logout() {
        tokenStorage.setJwt(null)
    }
}
