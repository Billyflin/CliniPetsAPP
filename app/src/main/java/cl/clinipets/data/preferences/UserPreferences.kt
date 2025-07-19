// data/preferences/UserPreferences.kt (CORREGIDO)
package cl.clinipets.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.clinipets.ui.theme.Contrast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_DYNAMIC_COLOR = booleanPreferencesKey("is_dynamic_color")
        val CONTRAST = stringPreferencesKey("contrast")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
    }

    // Theme preferences
    val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[Keys.IS_DARK_MODE] == true
        }

    val isDynamicColor: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[Keys.IS_DYNAMIC_COLOR] != false
        }

    val contrast: Flow<Contrast> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val contrastString = preferences[Keys.CONTRAST] ?: Contrast.Standard.name
            Contrast.valueOf(contrastString)
        }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[Keys.HAS_COMPLETED_ONBOARDING] == true
        }

    // User data
    val userId: Flow<String?> = dataStore.data
        .map { preferences -> preferences[Keys.USER_ID] }

    val userEmail: Flow<String?> = dataStore.data
        .map { preferences -> preferences[Keys.USER_EMAIL] }

    val userDisplayName: Flow<String?> = dataStore.data
        .map { preferences -> preferences[Keys.USER_DISPLAY_NAME] }

    val userPhotoUrl: Flow<String?> = dataStore.data
        .map { preferences -> preferences[Keys.USER_PHOTO_URL] }

    // Update functions
    suspend fun setDarkMode(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_DARK_MODE] = value
        }
    }

    suspend fun setDynamicColor(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_DYNAMIC_COLOR] = value
        }
    }

    suspend fun setContrast(value: Contrast) {
        dataStore.edit { preferences ->
            preferences[Keys.CONTRAST] = value.name
        }
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.HAS_COMPLETED_ONBOARDING] = value
        }
    }

    suspend fun updateUserData(
        userId: String?,
        email: String?,
        displayName: String?,
        photoUrl: String?
    ) {
        dataStore.edit { preferences ->
            userId?.let { preferences[Keys.USER_ID] = it }
            email?.let { preferences[Keys.USER_EMAIL] = it }
            displayName?.let { preferences[Keys.USER_DISPLAY_NAME] = it }
            photoUrl?.let { preferences[Keys.USER_PHOTO_URL] = it }
        }
    }

    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.USER_ID)
            preferences.remove(Keys.USER_EMAIL)
            preferences.remove(Keys.USER_DISPLAY_NAME)
            preferences.remove(Keys.USER_PHOTO_URL)
        }
    }
}