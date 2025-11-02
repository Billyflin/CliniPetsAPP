package cl.clinipets.core.di

import cl.clinipets.BuildConfig
import cl.clinipets.openapi.apis.AutenticacinApi
import cl.clinipets.openapi.apis.DescubrimientoApi
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.apis.ReservasApi
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
        return ApiClient(baseUrl = BuildConfig.BASE_URL, authNames = arrayOf("bearerAuth"))
    }

    @Provides
    @Singleton
    fun provideAuthApi(apiClient: ApiClient): AutenticacinApi =
        apiClient.createService(AutenticacinApi::class.java)

    @Provides
    @Singleton
    fun provideDescubrimientoApi(apiClient: ApiClient): DescubrimientoApi =
        apiClient.createService(DescubrimientoApi::class.java)

    @Provides
    @Singleton
    fun provideMascotasApi(apiClient: ApiClient): MascotasApi =
        apiClient.createService(MascotasApi::class.java)

    @Provides
    @Singleton
    fun provideReservasApi(apiClient: ApiClient): ReservasApi =
        apiClient.createService(ReservasApi::class.java)

    @Provides
    @Singleton
    fun provideVeterinariosApi(apiClient: ApiClient): VeterinariosApi =
        apiClient.createService(VeterinariosApi::class.java)

    // opcional: proveer el CLIENT_ID si lo querés inyectar
    @Provides
    @Named("GoogleClientId")
    fun provideGoogleClientId(): String = BuildConfig.GOOGLE_SERVER_CLIENT_ID

}
