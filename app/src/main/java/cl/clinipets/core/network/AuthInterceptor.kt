package cl.clinipets.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath
        val skip = path.startsWith("/api/auth") || path.startsWith("/api/publico")
        val token = tokenProvider()
        val newReq = if (!skip && !token.isNullOrBlank()) {
            req.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else req
        return chain.proceed(newReq)
    }
}

