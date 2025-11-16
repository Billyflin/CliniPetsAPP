package cl.clinipets.core.di

import android.content.Context
import android.util.Log
import cl.clinipets.BuildConfig
import cl.clinipets.core.ws.StompClient
import cl.clinipets.openapi.apis.AgendaControllerApi
import cl.clinipets.openapi.apis.AutenticacinApi
import cl.clinipets.openapi.apis.DescubrimientoApi
import cl.clinipets.openapi.apis.HorariosControllerApi
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.infrastructure.ApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideApiClient(): ApiClient {
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
    fun provideDescubrimientoApi(apiClient: ApiClient): DescubrimientoApi =
        apiClient.createService(DescubrimientoApi::class.java)

    @Provides
    @Singleton
    fun provideAgendaApi(apiClient: ApiClient): AgendaControllerApi =
        apiClient.createService(AgendaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideHorarios(apiClient: ApiClient): HorariosControllerApi =
        apiClient.createService(HorariosControllerApi::class.java)

    @Provides
    @Singleton
    fun provideStompClient(): StompClient {
        val httpClient = OkHttpClient.Builder().build()
        // baseUrl http(s)://host/ -> ws(s)://host/ws
        val baseUrl = resolveBaseUrl()
        val base = baseUrl.trimEnd('/')
        val wsUrl = base.replaceFirst("http", "ws") + "/ws"
        return StompClient(wsUrl, httpClient)
    }

    @Provides
    @Named("GoogleClientId")
    fun provideGoogleClientId(): String = BuildConfig.GOOGLE_SERVER_CLIENT_ID

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun resolveBaseUrl(): String {
        fun read(field: String): String? = runCatching {
            BuildConfig::class.java.getField(field).get(null) as? String
        }.getOrNull()?.takeIf { it.isNotBlank() }
        //
        val candidates = listOf(
            read("BASE_URL_RELEASE"),
            read("BASE_URL_VPS"),
            read("BASE_URL_DEBUG"),
        )
        val chosen = candidates.firstOrNull { !it.isNullOrBlank() } ?: "BASE_URL_DEBUG"
        return if (chosen.endsWith('/')) chosen else "$chosen/"
    }
}
