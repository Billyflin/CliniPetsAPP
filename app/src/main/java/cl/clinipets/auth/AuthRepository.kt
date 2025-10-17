package cl.clinipets.auth

import cl.clinipets.network.ApiService

class AuthRepository(private val api: ApiService, private val tokenStore: TokenStore) {

    suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return try {
            val resp = api.loginWithGoogle(cl.clinipets.network.GoogleAuthRequest(idToken))
            if (!resp.token.isNullOrEmpty()) {
                tokenStore.saveToken(resp.token)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Empty token from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProfile(): Result<cl.clinipets.network.UserProfile> {
        return try {
            val profile = api.me()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        tokenStore.clearToken()
    }
}

