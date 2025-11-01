    package cl.clinipets.feature.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.clinipets.feature.auth.domain.Sesion
import cl.clinipets.openapi.models.LoginResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

    private const val DATASTORE_NAME = "auth_preferences"
private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

private val KEY_TOKEN = stringPreferencesKey("token")
private val KEY_EXPIRES_AT = stringPreferencesKey("expires_at")

@Singleton
class AuthPreferencesDataSource @Inject constructor(
    @ApplicationContext context: Context,
) : SesionLocalDataSource {

    private val dataStore: DataStore<Preferences> = context.authDataStore

    override val sesion: Flow<Sesion?> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            val token = prefs[KEY_TOKEN] ?: return@map null
            val expiresAt = prefs[KEY_EXPIRES_AT]?.let {
                runCatching { OffsetDateTime.parse(it) }.getOrNull()
            }
            Sesion(token = token, expiraEn = expiresAt)
        }

    override suspend fun guardarSesion(login: LoginResponse) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = login.token
            prefs[KEY_EXPIRES_AT] = login.expiresAt.toString()
        }
    }

    override suspend fun limpiarSesion() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_EXPIRES_AT)
        }
    }
}
