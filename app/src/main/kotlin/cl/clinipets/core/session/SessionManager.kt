// core/session/SessionManager.kt
package cl.clinipets.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.clinipets.openapi.infrastructure.AuthInterceptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

@Singleton
class SessionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authInterceptor: AuthInterceptor
) {
    private val KEY = stringPreferencesKey("access_token")

    data class SessionSnapshot(
        val token: String?
    )

    val sessionFlow: Flow<SessionSnapshot> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY]
        SessionSnapshot(token)
    }

    val tokenFlow: Flow<String?> = sessionFlow.map { it.token }

    suspend fun setAndPersist(token: String) {
        authInterceptor.setToken(token)
        Logger.getLogger("SessionManager").info("Token set")
        context.dataStore.edit { it[KEY] = token }
    }

    suspend fun restoreIfAny() {
        val t = tokenFlow.first()
        if (!t.isNullOrBlank()) authInterceptor.setToken(t)
    }

    suspend fun clear() {
        authInterceptor.setToken(null)
        context.dataStore.edit {
            it.remove(KEY)
        }
    }
}
