// core/session/SessionManager.kt
package cl.clinipets.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.clinipets.openapi.infrastructure.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient
) {
    private val KEY = stringPreferencesKey("access_token")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY] }

    suspend fun setAndPersist(token: String) {
        apiClient.setBearerToken(token)
        context.dataStore.edit { it[KEY] = token }
    }

    suspend fun restoreIfAny() {
        val t = tokenFlow.first()
        if (!t.isNullOrBlank()) apiClient.setBearerToken(t)
    }

    suspend fun clear() {
        apiClient.setBearerToken("")
        context.dataStore.edit { it.remove(KEY) }
    }
}
