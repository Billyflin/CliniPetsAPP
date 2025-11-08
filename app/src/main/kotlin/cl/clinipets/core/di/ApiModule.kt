package cl.clinipets.core.di

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
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideApiClient(): ApiClient {
        // Resolución segura de baseUrl: intenta varios campos y env vars.
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
        fun read(field: String): String? = runCatching {
            BuildConfig::class.java.getField(field).get(null) as? String
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val candidates = listOf(
            read("BASE_URL_RELEASE"),
            read("BASE_URL_DEBUG"),
            read("BASE_URL"),
            System.getenv("CLINIPETS_BASE_URL"),
            System.getProperty("clinipets.baseUrl")
        )
        val chosen = candidates.firstOrNull { !it.isNullOrBlank() } ?: "http://10.0.2.2:8080"
        // Asegura barra final para evitar problemas de concatenación en el cliente autogenerado
        return if (chosen.endsWith('/')) chosen else "$chosen/"
    }
}
