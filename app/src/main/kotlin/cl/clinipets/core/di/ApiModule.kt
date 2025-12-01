package cl.clinipets.core.di

import android.content.Context
import android.util.Log
import cl.clinipets.BuildConfig
import cl.clinipets.openapi.apis.AuthControllerApi
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.ServicioMedicoControllerApi
import cl.clinipets.openapi.infrastructure.ApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

import cl.clinipets.openapi.infrastructure.AuthInterceptor

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideApiClient(authInterceptor: AuthInterceptor): ApiClient {
        val baseUrl = resolveBaseUrl()

        val apiClient = ApiClient(
            baseUrl = baseUrl
        )
        
        // Add Bearer Token Interceptor
        apiClient.addAuthorization("Bearer", authInterceptor)

        if (BuildConfig.DEBUG) {
            apiClient.setLogger { message ->
                Log.d("OkHttp", message)
            }
        }

        return apiClient
    }

    @Provides
    @Singleton
    fun provideAuthApi(apiClient: ApiClient): AuthControllerApi =
        apiClient.createService(AuthControllerApi::class.java)

    @Provides
    @Singleton
    fun provideMascotasApi(apiClient: ApiClient): MascotaControllerApi =
        apiClient.createService(MascotaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideServiciosApi(apiClient: ApiClient): ServicioMedicoControllerApi =
        apiClient.createService(ServicioMedicoControllerApi::class.java)



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
