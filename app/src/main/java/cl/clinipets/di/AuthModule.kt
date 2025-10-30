package cl.clinipets.di

import android.content.Context
import cl.clinipets.auth.AuthInterceptor
import cl.clinipets.auth.EncryptedCookieJar
import cl.clinipets.auth.TokenRepository
import cl.clinipets.auth.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore {
        return TokenStore(context)
    }

    @Provides
    @Singleton
    fun provideAppCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideTokenRepository(tokenStore: TokenStore, appCoroutineScope: CoroutineScope): TokenRepository {
        return TokenRepository(tokenStore, appCoroutineScope)
    }

    @Provides
    @Singleton
    fun provideEncryptedCookieJar(@ApplicationContext context: Context): EncryptedCookieJar {
        return EncryptedCookieJar(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenRepository: TokenRepository): AuthInterceptor {
        return AuthInterceptor(tokenRepository)
    }
}
