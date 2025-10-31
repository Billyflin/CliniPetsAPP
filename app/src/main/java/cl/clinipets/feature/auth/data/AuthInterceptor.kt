package cl.clinipets.feature.auth.data

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider.obtenerTokenActual()

        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }

        val request = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return try {
            val response = chain.proceed(request)
            if (response.code in CODIGOS_ERROR_SERVIDOR) {
                tokenProvider.invalidarSesionPorError()
            }
            response
        } catch (io: IOException) {
            tokenProvider.invalidarSesionPorError()
            throw io
        }
    }

    companion object {
        private val CODIGOS_ERROR_SERVIDOR = 500..599
    }
}
