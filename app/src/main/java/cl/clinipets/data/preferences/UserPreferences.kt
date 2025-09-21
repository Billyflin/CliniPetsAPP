// data/preferences/UserPreferences.kt  (asegúrate de este ctor para inyectar DataStore en tests)
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
class UserPreferences private constructor(
    private val dataStore: DataStore<Preferences>
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.dataStore)

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

    val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.IS_DARK_MODE] == true }

    val isDynamicColor: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.IS_DYNAMIC_COLOR] != false }

    val contrast: Flow<Contrast> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { Contrast.valueOf(it[Keys.CONTRAST] ?: Contrast.Standard.name) }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.HAS_COMPLETED_ONBOARDING] == true }

    val userId: Flow<String?> = dataStore.data.map { it[Keys.USER_ID] }
    val userEmail: Flow<String?> = dataStore.data.map { it[Keys.USER_EMAIL] }
    val userDisplayName: Flow<String?> = dataStore.data.map { it[Keys.USER_DISPLAY_NAME] }
    val userPhotoUrl: Flow<String?> = dataStore.data.map { it[Keys.USER_PHOTO_URL] }

    suspend fun setDarkMode(value: Boolean) { dataStore.edit { it[Keys.IS_DARK_MODE] = value } }
    suspend fun setDynamicColor(value: Boolean) { dataStore.edit { it[Keys.IS_DYNAMIC_COLOR] = value } }
    suspend fun setContrast(value: Contrast) { dataStore.edit { it[Keys.CONTRAST] = value.name } }
    suspend fun setOnboardingCompleted(value: Boolean) { dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = value } }

    suspend fun updateUserData(userId: String?, email: String?, displayName: String?, photoUrl: String?) {
        dataStore.edit { p ->
            userId?.let { p[Keys.USER_ID] = it }
            email?.let { p[Keys.USER_EMAIL] = it }
            displayName?.let { p[Keys.USER_DISPLAY_NAME] = it }
            photoUrl?.let { p[Keys.USER_PHOTO_URL] = it }
        }
    }

    suspend fun clearUserData() {
        dataStore.edit {
            it.remove(Keys.USER_ID)
            it.remove(Keys.USER_EMAIL)
            it.remove(Keys.USER_DISPLAY_NAME)
            it.remove(Keys.USER_PHOTO_URL)
        }
    }
}
