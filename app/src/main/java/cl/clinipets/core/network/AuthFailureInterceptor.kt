package cl.clinipets.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthFailureInterceptor(private val onUnauthorized: () -> Unit) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val path = request.url.encodedPath
        val isPublic = path.startsWith("/api/publico") || path.startsWith("/api/auth")
        if (!isPublic && response.code == 401) {
            onUnauthorized()
        }
        return response
    }
}

