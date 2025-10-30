package cl.clinipets.di

import cl.clinipets.BuildConfig
import cl.clinipets.feature.auth.data.AuthRepositorioImpl
import cl.clinipets.feature.auth.data.AuthInterceptor
import cl.clinipets.feature.auth.data.AuthPreferencesDataSource
import cl.clinipets.feature.auth.data.SesionLocalDataSource
import cl.clinipets.feature.auth.domain.AuthRepositorio
import cl.clinipets.feature.mascotas.data.MascotasRepositorioImpl
import cl.clinipets.feature.mascotas.domain.MascotasRepositorio
import cl.clinipets.openapi.apis.AutenticacinApi
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.infrastructure.ApiClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object RedModule {

    @Provides
    @Singleton
    fun proveeOkHttpBuilder(
        authInterceptor: AuthInterceptor,
    ): OkHttpClient.Builder {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
    }

    @Provides
    @Singleton
    fun proveeApiClient(
        builder: OkHttpClient.Builder,
    ): ApiClient = ApiClient(
        baseUrl = BuildConfig.BASE_URL,
        okHttpClientBuilder = builder,
    )

    @Provides
    @Singleton
    fun proveeAutenticacionApi(apiClient: ApiClient): AutenticacinApi =
        apiClient.createService(AutenticacinApi::class.java)

    @Provides
    @Singleton
    fun proveeMascotasApi(apiClient: ApiClient): MascotasApi =
        apiClient.createService(MascotasApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {

    @Binds
    @Singleton
    abstract fun vinculaMascotasRepositorio(
        impl: MascotasRepositorioImpl,
    ): MascotasRepositorio

    @Binds
    @Singleton
    abstract fun vinculaAuthRepositorio(
        impl: AuthRepositorioImpl,
    ): AuthRepositorio

    @Binds
    @Singleton
    abstract fun vinculaSesionLocalDataSource(
        impl: AuthPreferencesDataSource,
    ): SesionLocalDataSource
}
