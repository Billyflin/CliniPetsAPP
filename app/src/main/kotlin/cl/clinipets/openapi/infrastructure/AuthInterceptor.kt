package cl.clinipets.openapi.infrastructure

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Skip adding token if not targeting our backend
        if (!request.url.host.isBackendHost()) {
            return chain.proceed(request)
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        val token = if (currentUser != null) {
            try {
                Log.d("ClinipetsInterceptor", "Obteniendo IdToken de Firebase para: ${currentUser.email}")
                val task = currentUser.getIdToken(false)
                val result = Tasks.await(task, 10, TimeUnit.SECONDS)
                Log.d("ClinipetsInterceptor", "Token obtenido correctamente")
                result?.token
            } catch (e: Exception) {
                Log.e("ClinipetsInterceptor", "Error al obtener IdToken", e)
                null
            }
        } else {
            Log.w("ClinipetsInterceptor", "No hay usuario en Firebase, enviando petición sin token")
            null
        }

        return if (!token.isNullOrBlank()) {
            Log.d("ClinipetsInterceptor", "Adjuntando cabecera Authorization Bearer")
            val newRequest = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
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
        this.startsWith("192.168.") ||
        this == "10.0.2.2" // Emulator support
}
