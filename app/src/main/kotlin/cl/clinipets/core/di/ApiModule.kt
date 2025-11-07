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
        val baseUrl =   if (BuildConfig.DEBUG)  BuildConfig.BASE_URL_DEBUG else BuildConfig.BASE_URL_RELEASE
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

}
