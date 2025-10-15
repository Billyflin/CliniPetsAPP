package cl.clinipets.domain.auth

interface AuthRepository {
    fun isLoggedIn(): Boolean
    suspend fun loginWithGoogle(idToken: String): String
    suspend fun me(): String
}

