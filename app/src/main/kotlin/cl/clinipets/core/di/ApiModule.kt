package cl.clinipets.core.di

import android.os.Looper
import android.util.Log
import cl.clinipets.BuildConfig
import cl.clinipets.openapi.apis.AgendaApi
import cl.clinipets.openapi.apis.AutenticacinApi
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.infrastructure.ApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Volatile
    private var CACHED_BASE_URL: String? = null

    @Provides
    @Singleton
    fun provideApiClient(): ApiClient {
        // Resolución segura de baseUrl: intenta varios campos y env vars, y valida reachability.
        val baseUrl = resolveBaseUrl()

        val apiClient = ApiClient(
            baseUrl = baseUrl,
            authNames = arrayOf("bearerAuth")
        )

        if (BuildConfig.DEBUG) {
            apiClient.setLogger { message ->
                Log.d("OkHttp", message)
            }
        }

        return apiClient
    }

    @Provides
    @Singleton
    fun provideAuthApi(apiClient: ApiClient): AutenticacinApi =
        apiClient.createService(AutenticacinApi::class.java)

    @Provides
    @Singleton
    fun provideMascotasApi(apiClient: ApiClient): MascotasApi =
        apiClient.createService(MascotasApi::class.java)

    @Provides
    @Singleton
    fun provideVeterinariosApi(apiClient: ApiClient): VeterinariosApi =
        apiClient.createService(VeterinariosApi::class.java)

    @Provides
    @Singleton
    fun provideDescubrimientoApi(apiClient: ApiClient): cl.clinipets.openapi.apis.DescubrimientoApi =
        apiClient.createService(cl.clinipets.openapi.apis.DescubrimientoApi::class.java)

    @Provides
    @Singleton
    fun provideAgendaApi(apiClient: ApiClient): AgendaApi =
        apiClient.createService(AgendaApi::class.java)


    @Provides
    @Named("GoogleClientId")
    fun provideGoogleClientId(): String = BuildConfig.GOOGLE_SERVER_CLIENT_ID

    fun resolveBaseUrl(): String {
        // Usa cache si ya se resolvió
        CACHED_BASE_URL?.let { return it }

        fun read(field: String): String? = runCatching {
            BuildConfig::class.java.getField(field).get(null) as? String
        }.getOrNull()?.takeIf { it.isNotBlank() }

        // Candidatos de mayor a menor prioridad
        val rawCandidates = listOfNotNull(
            read("BASE_URL_RELEASE"),
            read("BASE_URL_DEBUG"),
            read("BASE_URL"),
            System.getenv("CLINIPETS_BASE_URL"),
            System.getProperty("clinipets.baseUrl"),
            "http://10.0.2.2:8080" // fallback por defecto (Emulador Android)
        )

        val candidates = rawCandidates
            .map(::normalizeUrl)
            .distinct()

        // Si no estamos en el hilo principal, intentamos validar reachability con HEAD rápido.
        val notOnMainThread = Looper.myLooper() != Looper.getMainLooper()
        val chosen = if (notOnMainThread) {
            val probeClient = OkHttpClient.Builder()
                .connectTimeout(750, TimeUnit.MILLISECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .callTimeout(1500, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .build()

            var selected: String? = null
            for (url in candidates) {
                val ok = canReachBaseUrl(probeClient, url)
                if (BuildConfig.DEBUG) Log.d(
                    "ApiModule",
                    "Probing $url -> ${if (ok) "OK" else "FAIL"}"
                )
                if (ok) {
                    selected = url; break
                }
            }
            selected ?: candidates.first()
        } else {
            // En hilo principal evitamos red: tomamos el primer candidato válido
            if (BuildConfig.DEBUG) Log.d(
                "ApiModule",
                "resolveBaseUrl en hilo principal: se omite probe de red"
            )
            candidates.first()
        }

        CACHED_BASE_URL = chosen
        return chosen
    }

    private fun normalizeUrl(input: String): String {
        var u = input.trim()
        // Añade esquema por defecto si falta
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        // Normaliza slash final
        if (!u.endsWith('/')) u += "/"
        return u
    }

    private fun canReachBaseUrl(client: OkHttpClient, baseUrl: String): Boolean {
        return runCatching {
            val req = Request.Builder().url(baseUrl).head().build()
            client.newCall(req).execute().use { resp ->
                // Considera 2xx como OK, y también 3xx/401/403/405 como señal de servidor vivo
                resp.isSuccessful || resp.code in 300..399 || resp.code == 401 || resp.code == 403 || resp.code == 405
            }
        }.getOrElse { false }
    }
}
