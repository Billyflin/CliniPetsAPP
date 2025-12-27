package cl.clinipets.core.di

import android.content.Context
import android.util.Log
import cl.clinipets.BuildConfig
import cl.clinipets.openapi.apis.AuthControllerApi
import cl.clinipets.openapi.apis.BloqueoControllerApi
import cl.clinipets.openapi.apis.DeviceTokenControllerApi
import cl.clinipets.openapi.apis.DisponibilidadControllerApi
import cl.clinipets.openapi.apis.FichaClinicaControllerApi
import cl.clinipets.openapi.apis.GaleriaControllerApi
import cl.clinipets.openapi.apis.GestinDeAgendaApi
import cl.clinipets.openapi.apis.HistorialClnicoApi
import cl.clinipets.openapi.apis.InventarioControllerApi
import cl.clinipets.openapi.apis.MaestrosControllerApi
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.PingControllerApi
import cl.clinipets.openapi.apis.ReporteControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.apis.ServicioMedicoControllerApi
import cl.clinipets.openapi.infrastructure.ApiClient
import cl.clinipets.openapi.infrastructure.AuthInterceptor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideCoilOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val okHttpBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d("OkHttp", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpBuilder.addInterceptor(loggingInterceptor)
        }

        return okHttpBuilder.build()
    }

    @Provides
    @Singleton
    fun provideApiClient(authInterceptor: AuthInterceptor): ApiClient {
        val baseUrl = resolveBaseUrl()

        // Creamos el cliente OkHttp con el interceptor global
        val okHttpBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d("OkHttp", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpBuilder.addInterceptor(loggingInterceptor)
        }

        // Se lo pasamos al ApiClient generado
        val apiClient = ApiClient(
            baseUrl = baseUrl,
            okHttpClientBuilder = okHttpBuilder
        )

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
    @Singleton
    fun provideDisponibilidadApi(apiClient: ApiClient): DisponibilidadControllerApi =
        apiClient.createService(DisponibilidadControllerApi::class.java)

    @Provides
    @Singleton
    fun provideReservaApi(apiClient: ApiClient): ReservaControllerApi =
        apiClient.createService(ReservaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideFichaClinicaApi(apiClient: ApiClient): FichaClinicaControllerApi =
        apiClient.createService(FichaClinicaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideMaestrosApi(apiClient: ApiClient): MaestrosControllerApi =
        apiClient.createService(MaestrosControllerApi::class.java)

    @Provides
    @Singleton
    fun provideBloqueoApi(apiClient: ApiClient): BloqueoControllerApi =
        apiClient.createService(BloqueoControllerApi::class.java)

    @Provides
    @Singleton
    fun provideDeviceTokenApi(apiClient: ApiClient): DeviceTokenControllerApi =
        apiClient.createService(DeviceTokenControllerApi::class.java)

    @Provides
    @Singleton
    fun provideGaleriaApi(apiClient: ApiClient): GaleriaControllerApi =
        apiClient.createService(GaleriaControllerApi::class.java)

    @Provides
    @Singleton
    fun provideGestionAgendaApi(apiClient: ApiClient): GestinDeAgendaApi =
        apiClient.createService(GestinDeAgendaApi::class.java)

    @Provides
    @Singleton
    fun provideHistorialClinicoApi(apiClient: ApiClient): HistorialClnicoApi =
        apiClient.createService(HistorialClnicoApi::class.java)

    @Provides
    @Singleton
    fun provideInventarioApi(apiClient: ApiClient): InventarioControllerApi =
        apiClient.createService(InventarioControllerApi::class.java)

    @Provides
    @Singleton
    fun provideReporteApi(apiClient: ApiClient): ReporteControllerApi =
        apiClient.createService(ReporteControllerApi::class.java)

    @Provides
    @Singleton
    fun providePingApi(apiClient: ApiClient): PingControllerApi =
        apiClient.createService(PingControllerApi::class.java)

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
            read("BASE_URL_DEBUG"),
        )
        val chosen = candidates.firstOrNull { !it.isNullOrBlank() } ?: "BASE_URL_DEBUG"
        return if (chosen.endsWith('/')) chosen else "$chosen/"
    }
}
