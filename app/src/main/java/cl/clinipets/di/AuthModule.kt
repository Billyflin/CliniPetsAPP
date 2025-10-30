package cl.clinipets.di

import android.content.Context
import cl.clinipets.BuildConfig
import cl.clinipets.feature.auth.data.GoogleAuthProvider
import cl.clinipets.feature.auth.data.GoogleAuthProviderImpl
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideGoogleSignInOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
            .requestEmail()
            .build()

    @Provides
    @Singleton
    fun provideGoogleSignInClient(
        @ApplicationContext context: Context,
        options: GoogleSignInOptions,
    ): GoogleSignInClient = GoogleSignIn.getClient(context, options)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GoogleAuthProviderModule {

    @Binds
    @Singleton
    abstract fun bindGoogleAuthProvider(
        impl: GoogleAuthProviderImpl,
    ): GoogleAuthProvider
}
