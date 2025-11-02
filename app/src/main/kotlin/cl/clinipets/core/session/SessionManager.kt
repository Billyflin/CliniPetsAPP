// core/session/SessionManager.kt
package cl.clinipets.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.clinipets.openapi.infrastructure.ApiClient
import cl.clinipets.openapi.models.MeResponse
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
    private val ROLES_KEY = stringPreferencesKey("user_roles")
    private val NAME_KEY = stringPreferencesKey("user_name")

    data class SessionSnapshot(
        val token: String?,
        val roles: List<String>,
        val displayName: String?
    )

    val sessionFlow: Flow<SessionSnapshot> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY]
        val roles = prefs[ROLES_KEY]?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?: emptyList()
        val name = prefs[NAME_KEY]
        SessionSnapshot(token, roles, name)
    }

    val tokenFlow: Flow<String?> = sessionFlow.map { it.token }
    val rolesFlow: Flow<List<String>> = sessionFlow.map { it.roles }
    val displayNameFlow: Flow<String?> = sessionFlow.map { it.displayName }

    suspend fun setAndPersist(token: String) {
        apiClient.setBearerToken(token)
        context.dataStore.edit { it[KEY] = token }
    }

    suspend fun restoreIfAny() {
        val t = tokenFlow.first()
        if (!t.isNullOrBlank()) apiClient.setBearerToken(t)
    }

    suspend fun persistProfile(me: MeResponse) {
        context.dataStore.edit { prefs ->
            val roles = me.roles.orEmpty().filter { it.isNotBlank() }
            if (roles.isEmpty()) prefs.remove(ROLES_KEY) else prefs[ROLES_KEY] = roles.joinToString(",")
            val name = me.nombre?.takeIf { it.isNotBlank() }
            if (name == null) prefs.remove(NAME_KEY) else prefs[NAME_KEY] = name
        }
    }

    suspend fun clear() {
        apiClient.setBearerToken("")
        context.dataStore.edit {
            it.remove(KEY)
            it.remove(ROLES_KEY)
            it.remove(NAME_KEY)
        }
    }
}
