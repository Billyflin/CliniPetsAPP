package cl.clinipets.auth

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)

