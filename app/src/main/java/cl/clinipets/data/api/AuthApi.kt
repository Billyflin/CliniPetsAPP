package cl.clinipets.data.api

import cl.clinipets.data.dto.GoogleLoginRequest
import cl.clinipets.data.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/google")
    suspend fun login(@Body body: GoogleLoginRequest): TokenResponse

    @GET("/api/auth/me")
    suspend fun me(): String
}

