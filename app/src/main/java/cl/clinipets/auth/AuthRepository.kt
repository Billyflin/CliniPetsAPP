package cl.clinipets.auth

import cl.clinipets.network.ApiService
import cl.clinipets.network.MeResponse

class AuthRepository(private val api: ApiService, private val tokenStore: TokenStore) {

    suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return try {
            val resp = api.loginWithGoogle(cl.clinipets.network.GoogleAuthRequest(idToken))
            val token = resp.token
            if (token.isNotEmpty()) {
                tokenStore.saveToken(token)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Empty token from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProfile(): Result<MeResponse> {
        return try {
            val profile = api.me()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refresh(): Result<String> {
        return try {
            val rr = api.refresh()
            if (rr.token.isNotEmpty()) {
                tokenStore.saveToken(rr.token)
                Result.success(rr.token)
            } else {
                Result.failure(Exception("Refresh returned empty token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bootstrapSession(): Result<MeResponse> {
        // Un solo intento de resolver sesión: me() que activará el Authenticator para refrescar si hay cookie
        return fetchProfile()
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) { }
        tokenStore.clearToken()
    }

    fun signOut() {
        tokenStore.clearToken()
    }
}
