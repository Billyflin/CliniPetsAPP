package cl.clinipets.auth

import cl.clinipets.network.ApiService
import cl.clinipets.network.MeResponse
import cl.clinipets.network.GoogleAuthRequest
import cl.clinipets.network.RefreshRequest

class AuthRepository(private val api: ApiService, private val tokenStore: TokenStore) {

    suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return try {
            val resp = api.loginWithGoogle(GoogleAuthRequest(idToken))
            val access = resp.accessToken ?: resp.token ?: ""
            val refresh = resp.refreshToken
            if (access.isNotEmpty()) {
                if (!refresh.isNullOrBlank()) tokenStore.saveTokens(access, refresh) else tokenStore.saveToken(access)
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
            val refresh = tokenStore.getRefreshToken() ?: return Result.failure(Exception("No refresh token available"))
            val rr = api.refresh(RefreshRequest(refresh))
            val newAccess = rr.accessToken ?: rr.token ?: ""
            val newRefresh = rr.refreshToken
            if (newAccess.isNotEmpty()) {
                if (!newRefresh.isNullOrBlank()) tokenStore.saveTokens(newAccess, newRefresh) else tokenStore.updateAccessToken(newAccess)
                Result.success(newAccess)
            } else {
                Result.failure(Exception("Refresh returned empty token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bootstrapSession(): Result<MeResponse> {
        // Un solo intento de resolver sesión: me() que activará el Authenticator para refrescar si hay cookie o refresh token
        return fetchProfile()
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) { }
        tokenStore.clearAll()
    }

    fun signOut() {
        tokenStore.clearAll()
    }
}
