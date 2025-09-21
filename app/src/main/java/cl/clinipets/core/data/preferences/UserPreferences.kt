package cl.clinipets.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.clinipets.core.ui.theme.Contrast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val CONTRAST = stringPreferencesKey("contrast")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PHOTO = stringPreferencesKey("user_photo")
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { it[Keys.DARK_MODE] ?: false }

    suspend fun setDarkMode(value: Boolean) {
        dataStore.edit { it[Keys.DARK_MODE] = value }
    }

    val isDynamicColor: Flow<Boolean> = dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    suspend fun setDynamicColor(value: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    }

    val hasCompletedOnboarding: Flow<Boolean> =
        dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingCompleted(value: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = value }
    }

    val contrast: Flow<Contrast> =
        dataStore.data.map { Contrast.valueOf(it[Keys.CONTRAST] ?: Contrast.Standard.name) }

    suspend fun setContrast(value: Contrast) {
        dataStore.edit { it[Keys.CONTRAST] = value.name }
    }

    val userId: Flow<String?> = dataStore.data.map { it[Keys.USER_ID] }
    val userEmail: Flow<String?> = dataStore.data.map { it[Keys.USER_EMAIL] }
    val userDisplayName: Flow<String?> = dataStore.data.map { it[Keys.USER_NAME] }
    val userPhotoUrl: Flow<String?> = dataStore.data.map { it[Keys.USER_PHOTO] }

    suspend fun updateUserData(id: String?, email: String?, name: String?, photo: String?) {
        dataStore.edit { prefs ->
            if (id != null) prefs[Keys.USER_ID] = id
            if (email != null) prefs[Keys.USER_EMAIL] = email
            if (name != null) prefs[Keys.USER_NAME] = name
            if (photo != null) prefs[Keys.USER_PHOTO] = photo
        }
    }

    suspend fun clearUserData() {
        dataStore.edit {
            it.remove(Keys.USER_ID)
            it.remove(Keys.USER_EMAIL)
            it.remove(Keys.USER_NAME)
            it.remove(Keys.USER_PHOTO)
        }
    }
}
