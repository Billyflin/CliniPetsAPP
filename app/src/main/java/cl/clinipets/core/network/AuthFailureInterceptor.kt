package cl.clinipets.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthFailureInterceptor(
    private val onUnauthorized: () -> Unit,
    private val onForbidden: (() -> Unit)? = null,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val path = request.url.encodedPath
        val isPublic = path.startsWith("/api/publico") || path.startsWith("/api/auth")
        if (!isPublic) {
            when (response.code) {
                401 -> onUnauthorized()
                403 -> onForbidden?.invoke()
            }
        }
        return response
    }
}
