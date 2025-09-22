package cl.clinipets.core.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import cl.clinipets.R
import cl.clinipets.core.data.preferences.UserPreferences
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /* ---------- Firebase ---------- */

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore =
        Firebase.firestore.apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(persistentCacheSettings {})
                .build()
        }

    /* ---------- Identity / Auth ---------- */

    // One Tap (Google Identity Services clásico)
    @Provides
    @Singleton
    fun provideOneTapClient(@ApplicationContext ctx: Context): SignInClient =
        Identity.getSignInClient(ctx)

    // Credential Manager (recomendado para Google Sign-In + Passkeys)
    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext ctx: Context): CredentialManager =
        CredentialManager.create(ctx)

    @Provides
    @Singleton
    @WebClientId
    fun provideWebClientId(@ApplicationContext ctx: Context): String =
        ctx.getString(R.string.default_web_client_id)

    // Opción Google ID para Credential Manager
    @Provides
    @Singleton
    fun provideGoogleIdOption(@WebClientId webClientId: String): GetGoogleIdOption =
        GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

    // Request base de Credential Manager
    @Provides
    @Singleton
    fun provideGetCredentialRequest(googleIdOption: GetGoogleIdOption): GetCredentialRequest =
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

    /* ---------- DataStore / Prefs ---------- */

    @Provides
    @Singleton
    @UserPrefs
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.dataStoreFile("user_prefs.preferences_pb") }
    )

    @Provides
    @Singleton
    fun provideUserPreferences(@UserPrefs ds: DataStore<Preferences>): UserPreferences =
        UserPreferences(ds)

    /* ---------- Dispatchers ---------- */

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

/* Qualifiers */
@Qualifier
annotation class UserPrefs

@Qualifier
annotation class WebClientId

@Qualifier
annotation class IoDispatcher
