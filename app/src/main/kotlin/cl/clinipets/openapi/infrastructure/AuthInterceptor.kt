package cl.clinipets.openapi.infrastructure

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {
    private val token = AtomicReference<String?>(null)

    fun setToken(newToken: String?) {
        token.set(newToken)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val currentToken = token.get()

        return if (!currentToken.isNullOrBlank() && request.url.host.isBackendHost()) {
            val newRequest = request.newBuilder()
                .addHeader("Authorization", "Bearer $currentToken")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }
}

private fun String?.isBackendHost(): Boolean {
    if (this.isNullOrBlank()) return false
    return this == "api.clinipets.cl" ||
        this == "homeserver.local" ||
        this.startsWith("192.168.")
}
